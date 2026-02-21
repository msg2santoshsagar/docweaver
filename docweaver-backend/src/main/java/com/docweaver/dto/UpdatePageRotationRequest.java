package com.docweaver.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePageRotationRequest(
        @NotNull Integer rotationDegrees
) {
}
