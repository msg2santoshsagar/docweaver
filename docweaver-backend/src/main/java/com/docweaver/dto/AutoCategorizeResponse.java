package com.docweaver.dto;

import java.util.List;
import java.util.UUID;

public record AutoCategorizeResponse(
        List<UUID> standaloneImageIds,
        List<GroupProposal> groups,
        List<RenamedImage> renamedImages
) {
    public record GroupProposal(
            String name,
            List<UUID> imageIds,
            String reason,
            double confidence
    ) {
    }

    public record RenamedImage(
            UUID imageId,
            String previousName,
            String newName,
            String source
    ) {
    }
}
