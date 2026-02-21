package com.docweaver.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameImageRequest(
        @NotBlank String displayName,
        String aiSuggestedName,
        String aiDocType,
        String aiSubject,
        String aiDocumentDate,
        String aiGroupKey,
        Double aiConfidence,
        Boolean autoApplied
) {
}
