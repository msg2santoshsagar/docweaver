package com.docweaver.dto;

import com.docweaver.entity.ImageMode;
import jakarta.validation.constraints.NotNull;

public record UpdateImageModeRequest(
        @NotNull ImageMode mode
) {
}
