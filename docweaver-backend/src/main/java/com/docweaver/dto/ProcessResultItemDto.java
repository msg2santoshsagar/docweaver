package com.docweaver.dto;

import com.docweaver.entity.GeneratedType;
import com.docweaver.entity.ProcessingStatus;

import java.util.UUID;

public record ProcessResultItemDto(
        UUID generatedDocumentId,
        GeneratedType type,
        UUID sourceImageId,
        UUID sourceGroupId,
        String outputPath,
        String outputName,
        ProcessingStatus status,
        String message
) {
}
