package com.docweaver.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NameSuggestionRequest(
        @NotBlank String fileName,
        @NotBlank String mimeType,
        @NotBlank String fileBytes,
        @NotNull @Valid SuggestionContext context
) {
    public record SuggestionContext(
            @NotBlank String category,
            boolean grouped
    ) {
    }
}
