package com.docweaver.dto;

import java.util.List;

public record ProcessResponse(
        boolean success,
        boolean deleteOriginalsAttempted,
        boolean originalsDeleted,
        boolean dryRun,
        List<ProcessResultItemDto> results
) {
}
