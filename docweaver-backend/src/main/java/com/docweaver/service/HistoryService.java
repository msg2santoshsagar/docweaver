package com.docweaver.service;

import com.docweaver.dto.GeneratedDocumentDto;
import com.docweaver.entity.GeneratedDocument;
import com.docweaver.repository.GeneratedDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class HistoryService {

    private final GeneratedDocumentRepository generatedDocumentRepository;

    public HistoryService(GeneratedDocumentRepository generatedDocumentRepository) {
        this.generatedDocumentRepository = generatedDocumentRepository;
    }

    @Transactional(readOnly = true)
    public List<GeneratedDocumentDto> list() {
        return generatedDocumentRepository.findAll().stream()
                .sorted(Comparator.comparing(GeneratedDocument::getCreatedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    private GeneratedDocumentDto toDto(GeneratedDocument doc) {
        return new GeneratedDocumentDto(
                doc.getId(),
                doc.getType(),
                doc.getSourceImageId(),
                doc.getSourceGroupId(),
                doc.getOutputPath(),
                doc.getOutputName(),
                doc.getStatus(),
                Boolean.TRUE.equals(doc.getDeleteOriginals()),
                Boolean.TRUE.equals(doc.getDryRun()),
                doc.getMessage(),
                doc.getCreatedAt()
        );
    }
}
