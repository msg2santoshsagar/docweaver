package com.docweaver.dto;

import com.docweaver.entity.ImageMode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ImageAssetDto(
        UUID id,
        String originalFileName,
        String displayName,
        String mimeType,
        String originalPath,
        Long fileSize,
        ImageMode mode,
        OffsetDateTime uploadedAt
) {
}
