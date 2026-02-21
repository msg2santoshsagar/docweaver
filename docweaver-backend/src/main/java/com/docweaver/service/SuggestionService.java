package com.docweaver.service;

import com.docweaver.dto.NameSuggestionRequest;
import com.docweaver.dto.NameSuggestionResponse;
import com.docweaver.entity.AppConfig;
import com.docweaver.util.FilenameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final AppConfigService appConfigService;
    private final LocalAiService localAiService;
    private final NamePatternService namePatternService;

    public NameSuggestionResponse suggest(NameSuggestionRequest request) {
        AppConfig config = appConfigService.getOrCreate();
        LocalAiService.AiDocumentAnalysis analysis = localAiService.analyzeImage(config, request.mimeType(), request.fileBytes());

        if (Boolean.TRUE.equals(config.getAiEnabled()) && hasUsableAiSignal(analysis)) {
            String aiName = buildAiName(analysis, request.fileName());
            String learned = namePatternService.applyPattern(
                    analysis.docType(),
                    analysis.subject(),
                    analysis.documentDate(),
                    aiName
            );
            return new NameSuggestionResponse(
                    learned,
                    analysis.confidence() > 0.0 ? analysis.confidence() : 0.65,
                    "AI_LOCAL",
                    analysis.docType(),
                    analysis.subject(),
                    analysis.documentDate(),
                    analysis.groupKey()
            );
        }

        String base = request.fileName();
        if (base.contains(".")) {
            base = base.substring(0, base.lastIndexOf('.'));
        }
        String suggested = FilenameUtil.sanitizeBaseName(base);
        return new NameSuggestionResponse(suggested, 0.72, "BASIC", "other", "", "", "");
    }

    private boolean hasUsableAiSignal(LocalAiService.AiDocumentAnalysis analysis) {
        return analysis.confidence() > 0.20
                || !analysis.subject().isBlank()
                || !analysis.documentDate().isBlank()
                || !"other".equals(analysis.docType());
    }

    private String buildAiName(LocalAiService.AiDocumentAnalysis analysis, String fileName) {
        String base = fileName;
        if (base.contains(".")) {
            base = base.substring(0, base.lastIndexOf('.'));
        }
        String fallback = FilenameUtil.sanitizeBaseName(base);
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
}
