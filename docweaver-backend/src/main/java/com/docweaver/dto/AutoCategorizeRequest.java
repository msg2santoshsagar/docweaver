package com.docweaver.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record AutoCategorizeRequest(
        List<UUID> imageIds
) {
    public List<UUID> safeImageIds() {
        return imageIds == null ? List.of() : new ArrayList<>(imageIds);
    }
}
