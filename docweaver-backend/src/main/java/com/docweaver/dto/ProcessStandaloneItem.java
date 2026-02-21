package com.docweaver.dto;

import com.docweaver.entity.OutputType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProcessStandaloneItem(
        @NotNull UUID imageId,
        @NotNull OutputType outputType
) {
}
