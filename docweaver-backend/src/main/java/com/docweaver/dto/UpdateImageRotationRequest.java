package com.docweaver.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateImageRotationRequest(
        @NotNull Integer rotationDegrees
) {
}
