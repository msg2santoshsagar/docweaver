package com.docweaver.dto;

import java.util.List;

public record HistoryPageDto(
        List<GeneratedDocumentDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
