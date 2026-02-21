package com.docweaver.dto;

public record NameSuggestionResponse(
        String suggestedName,
        double confidence,
        String source
) {
}
