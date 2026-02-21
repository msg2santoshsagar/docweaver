package com.docweaver.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ProcessRequest(
        List<ProcessStandaloneItem> standaloneItems,
        List<UUID> groupIds,
        Boolean deleteOriginals
) {
    public List<ProcessStandaloneItem> safeStandaloneItems() {
        return standaloneItems == null ? new ArrayList<>() : standaloneItems;
    }

    public List<UUID> safeGroupIds() {
        return groupIds == null ? new ArrayList<>() : groupIds;
    }
}
