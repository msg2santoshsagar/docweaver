package com.docweaver.dto;

public record NameSuggestionResponse(
        String suggestedName,
        double confidence,
        String source,
        String docType,
        String subject,
        String documentDate,
        String groupKey
) {
}
