package com.docweaver.service;

import com.docweaver.dto.AutoCategorizeRequest;
import com.docweaver.dto.AutoCategorizeResponse;
import com.docweaver.entity.AppConfig;
import com.docweaver.entity.ImageAsset;
import com.docweaver.repository.ImageAssetRepository;
import com.docweaver.util.FilenameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AutoCategorizeService {

    private final AppConfigService appConfigService;
    private final ImageService imageService;
    private final ImageAssetRepository imageAssetRepository;
    private final LocalAiService localAiService;
    private final NamePatternService namePatternService;

    @Transactional
    public AutoCategorizeResponse autoCategorize(AutoCategorizeRequest request) {
        AppConfig config = appConfigService.getOrCreate();

        List<ImageAsset> images = request.safeImageIds().stream()
                .map(imageService::getEntity)
                .toList();

        Map<UUID, AnalysisWithKey> analysisById = new LinkedHashMap<>();
        List<AutoCategorizeResponse.RenamedImage> renamedImages = new ArrayList<>();
        for (ImageAsset image : images) {
            byte[] bytes = imageService.loadImageBytes(image.getId());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            LocalAiService.AiDocumentAnalysis analysis = localAiService.analyzeImage(config, image.getMimeType(), base64);

            String suggestedName = resolveDisplayName(config, image, analysis);
            String source = (Boolean.TRUE.equals(config.getAiEnabled()) && hasUsableAiSignal(analysis)) ? "AI_LOCAL" : "BASIC";
            String previousName = image.getDisplayName();
            if (!suggestedName.isBlank() && !suggestedName.equals(previousName)) {
                image.setDisplayName(suggestedName);
                renamedImages.add(new AutoCategorizeResponse.RenamedImage(
                        image.getId(),
                        previousName,
                        suggestedName,
                        source
                ));
            }

            image.setAiSuggestedName(suggestedName);
            image.setAiDocType(analysis.docType());
            image.setAiSubject(analysis.subject());
            image.setAiDocumentDate(analysis.documentDate());
            image.setAiGroupKey(analysis.groupKey());
            image.setAiConfidence(analysis.confidence());
            imageAssetRepository.save(image);

            String groupKey = resolveGroupKey(analysis);
            analysisById.put(image.getId(), new AnalysisWithKey(analysis, groupKey));
        }

        Map<String, List<UUID>> grouped = new LinkedHashMap<>();
        for (Map.Entry<UUID, AnalysisWithKey> entry : analysisById.entrySet()) {
            String key = entry.getValue().groupKey();
            if (key.isBlank()) {
                continue;
            }
            grouped.computeIfAbsent(key, __ -> new ArrayList<>()).add(entry.getKey());
        }

        List<AutoCategorizeResponse.GroupProposal> groups = new ArrayList<>();
        for (Map.Entry<String, List<UUID>> entry : grouped.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            UUID firstId = entry.getValue().get(0);
            LocalAiService.AiDocumentAnalysis first = analysisById.get(firstId).analysis();
            String groupName = buildGroupName(first, entry.getKey());
            groups.add(new AutoCategorizeResponse.GroupProposal(
                    groupName,
                    entry.getValue(),
                    "Detected multipage or same-document key",
                    first.confidence()
            ));
        }

        List<UUID> groupedIds = groups.stream()
                .flatMap(g -> g.imageIds().stream())
                .distinct()
                .toList();
        List<UUID> standaloneIds = analysisById.keySet().stream()
                .filter(id -> !groupedIds.contains(id))
                .toList();

        return new AutoCategorizeResponse(standaloneIds, groups, renamedImages);
    }

    private String resolveDisplayName(AppConfig config, ImageAsset image, LocalAiService.AiDocumentAnalysis analysis) {
        if (Boolean.TRUE.equals(config.getAiEnabled()) && hasUsableAiSignal(analysis)) {
            String aiName = buildAiName(analysis, image.getOriginalFileName());
            return namePatternService.applyPattern(
                    analysis.docType(),
                    analysis.subject(),
                    analysis.documentDate(),
                    aiName
            );
        }
        return fallbackName(image.getOriginalFileName());
    }

    private boolean hasUsableAiSignal(LocalAiService.AiDocumentAnalysis analysis) {
        return analysis.confidence() > 0.20
                || !analysis.subject().isBlank()
                || !analysis.documentDate().isBlank()
                || !"other".equals(analysis.docType());
    }

    private String buildAiName(LocalAiService.AiDocumentAnalysis analysis, String fileName) {
        String fallback = fallbackName(fileName);
        String subject = FilenameUtil.sanitizeBaseName(analysis.subject());
        String date = FilenameUtil.sanitizeBaseName(analysis.documentDate());
        String type = FilenameUtil.sanitizeBaseName(analysis.docType());
        String subType = FilenameUtil.sanitizeBaseName(analysis.subType());
        String attrs = FilenameUtil.sanitizeBaseName(analysis.attributes());

        String raw = String.join("-",
                subject.isBlank() ? "document" : subject,
                date,
                type.isBlank() ? "other" : type,
                subType,
                attrs
        ).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");

        String sanitized = FilenameUtil.sanitizeBaseName(raw);
        return sanitized.isBlank() ? fallback : sanitized;
    }

    private String fallbackName(String fileName) {
        String base = fileName;
        int idx = base.lastIndexOf('.');
        if (idx > 0) {
            base = base.substring(0, idx);
        }
        return FilenameUtil.sanitizeBaseName(base);
    }

    private String resolveGroupKey(LocalAiService.AiDocumentAnalysis analysis) {
        if (!analysis.groupKey().isBlank()) {
            return analysis.groupKey();
        }
        if (analysis.totalPages() > 1 || analysis.pageNumber() > 0) {
            String candidate = String.join("-",
                    sanitize(analysis.subject()),
                    sanitize(analysis.documentDate()),
                    sanitize(analysis.docType())
            ).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
            return candidate.isBlank() ? sanitize(analysis.docType()) : candidate;
        }
        return "";
    }

    private String buildGroupName(LocalAiService.AiDocumentAnalysis analysis, String fallbackKey) {
        String raw = String.join("-",
                sanitize(analysis.subject()),
                sanitize(analysis.documentDate()),
                sanitize(analysis.docType())
        ).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        String sanitized = FilenameUtil.sanitizeBaseName(raw);
        if (!sanitized.isBlank()) {
            return sanitized;
        }
        String fallback = FilenameUtil.sanitizeBaseName(fallbackKey);
        return fallback.isBlank() ? "grouped-document" : fallback;
    }

    private String sanitize(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        return FilenameUtil.sanitizeBaseName(token).toLowerCase(Locale.ROOT);
    }

    private record AnalysisWithKey(LocalAiService.AiDocumentAnalysis analysis, String groupKey) {
    }
}
