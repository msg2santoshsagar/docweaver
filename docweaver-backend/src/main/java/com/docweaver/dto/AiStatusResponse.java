package com.docweaver.dto;

import java.util.List;

public record AiStatusResponse(
        boolean enabled,
        boolean available,
        String configuredModel,
        String endpoint,
        String reason,
        List<String> availableModels
) {
}
