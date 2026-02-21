package com.docweaver.service;

import com.docweaver.dto.GeneratedDocumentDto;
import com.docweaver.entity.GeneratedDocument;
import com.docweaver.mapper.GeneratedDocumentMapper;
import com.docweaver.repository.GeneratedDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final GeneratedDocumentRepository generatedDocumentRepository;
    private final GeneratedDocumentMapper generatedDocumentMapper;

    @Transactional(readOnly = true)
    public List<GeneratedDocumentDto> list() {
        return generatedDocumentRepository.findAll().stream()
                .sorted(Comparator.comparing(GeneratedDocument::getCreatedAt).reversed())
                .map(generatedDocumentMapper::toDto)
                .toList();
    }
}
