package com.docweaver.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DocumentGroupDto(
        UUID id,
        String name,
        List<ImageAssetDto> images,
        Map<UUID, Integer> rotationByImageId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
