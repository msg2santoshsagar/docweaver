package com.docweaver.service;

import com.docweaver.dto.GeneratedDocumentDto;
import com.docweaver.dto.HistoryPageDto;
import com.docweaver.entity.GeneratedDocument;
import com.docweaver.entity.GeneratedType;
import com.docweaver.entity.ProcessingStatus;
import com.docweaver.mapper.GeneratedDocumentMapper;
import com.docweaver.repository.GeneratedDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 200;

    private final GeneratedDocumentRepository generatedDocumentRepository;
    private final GeneratedDocumentMapper generatedDocumentMapper;

    @Transactional(readOnly = true)
    public HistoryPageDto list(int page, int size, ProcessingStatus status, GeneratedType type, String query) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size <= 0 ? DEFAULT_PAGE_SIZE : size));

        Specification<GeneratedDocument> spec = (root, ignored, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, ignored, cb) -> cb.equal(root.get("status"), status));
        }
        if (type != null) {
            spec = spec.and((root, ignored, cb) -> cb.equal(root.get("type"), type));
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, ignored, cb) -> cb.or(
                    cb.like(cb.lower(root.get("outputName")), like),
                    cb.like(cb.lower(root.get("outputPath")), like),
                    cb.like(cb.lower(root.get("message")), like)
            ));
        }

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GeneratedDocument> result = generatedDocumentRepository.findAll(spec, pageRequest);

        return new HistoryPageDto(
                result.getContent().stream().map(generatedDocumentMapper::toDto).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    @Transactional
    @Scheduled(cron = "0 15 2 * * *")
    public void cleanupOldHistory() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusYears(1);
        long deleted = generatedDocumentRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("History cleanup removed {} record(s) older than {}", deleted, cutoff);
        }
    }
}
