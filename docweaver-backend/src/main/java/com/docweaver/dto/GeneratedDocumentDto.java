package com.docweaver.dto;

import com.docweaver.entity.GeneratedType;
import com.docweaver.entity.ProcessingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GeneratedDocumentDto(
        UUID id,
        GeneratedType type,
        UUID sourceImageId,
        UUID sourceGroupId,
        String outputPath,
        String outputName,
        ProcessingStatus status,
        boolean deleteOriginals,
        boolean dryRun,
        String message,
        OffsetDateTime createdAt
) {
}
