package com.docweaver.repository;

import com.docweaver.entity.GeneratedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, UUID>, JpaSpecificationExecutor<GeneratedDocument> {
    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
