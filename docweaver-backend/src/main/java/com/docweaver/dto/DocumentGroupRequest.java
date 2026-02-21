package com.docweaver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record DocumentGroupRequest(
        @NotBlank String name,
        @NotEmpty List<UUID> imageIds
) {
}
