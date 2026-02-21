package com.docweaver.service;

import com.docweaver.entity.ImageAsset;
import com.docweaver.entity.NamePattern;
import com.docweaver.repository.NamePatternRepository;
import com.docweaver.util.FilenameUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NamePatternService {

    private final NamePatternRepository namePatternRepository;

    @Transactional
    public void learnFromRename(ImageAsset image, String finalDisplayName) {
        String docType = sanitizeToken(image.getAiDocType());
        if (docType.isBlank()) {
            return;
        }

        String template = inferTemplate(finalDisplayName, image);
        if (template.isBlank()) {
            return;
        }

        NamePattern pattern = namePatternRepository.findFirstByDocTypeOrderByUpdatedAtDesc(docType)
                .orElseGet(NamePattern::new);
        pattern.setDocType(docType);
        pattern.setTemplate(template);
        pattern.setUsageCount((pattern.getUsageCount() == null ? 0 : pattern.getUsageCount()) + 1);
        namePatternRepository.save(pattern);
    }

    @Transactional(readOnly = true)
    public String applyPattern(String docType, String subject, String documentDate, String fallbackName) {
        String normalizedDocType = sanitizeToken(docType);
        if (normalizedDocType.isBlank()) {
            return fallbackName;
        }

        return namePatternRepository.findFirstByDocTypeOrderByUpdatedAtDesc(normalizedDocType)
                .map(pattern -> fillTemplate(pattern.getTemplate(), subject, documentDate, normalizedDocType, fallbackName))
                .orElse(fallbackName);
    }

    private String inferTemplate(String finalDisplayName, ImageAsset image) {
        String template = FilenameUtil.sanitizeBaseName(finalDisplayName);
        if (template.isBlank()) {
            return "";
        }

        String subject = sanitizeToken(image.getAiSubject());
        String date = sanitizeToken(image.getAiDocumentDate());
        String docType = sanitizeToken(image.getAiDocType());

        template = replaceToken(template, subject, "{subject}");
        template = replaceToken(template, date, "{date}");
        template = replaceToken(template, docType, "{type}");

        // Keep only meaningful templates that contain at least one placeholder.
        if (!template.contains("{subject}") && !template.contains("{date}") && !template.contains("{type}")) {
            return "";
        }
        return template;
    }

    private String fillTemplate(String template, String subject, String documentDate, String docType, String fallbackName) {
        String filled = template;
        filled = filled.replace("{subject}", sanitizeToken(subject));
        filled = filled.replace("{date}", sanitizeToken(documentDate));
        filled = filled.replace("{type}", sanitizeToken(docType));
        filled = filled.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        String normalized = FilenameUtil.sanitizeBaseName(filled);
        return normalized.isBlank() ? fallbackName : normalized;
    }

    private String replaceToken(String source, String tokenValue, String placeholder) {
        if (tokenValue.isBlank()) {
            return source;
        }
        return source.replace(tokenValue, placeholder);
    }

    private String sanitizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return FilenameUtil.sanitizeBaseName(value);
    }
}
