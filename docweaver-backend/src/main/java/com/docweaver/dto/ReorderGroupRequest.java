package com.docweaver.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderGroupRequest(@NotEmpty List<UUID> orderedImageIds) {
}
