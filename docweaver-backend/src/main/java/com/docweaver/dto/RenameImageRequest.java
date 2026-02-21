package com.docweaver.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameImageRequest(
        @NotBlank String displayName
) {
}
