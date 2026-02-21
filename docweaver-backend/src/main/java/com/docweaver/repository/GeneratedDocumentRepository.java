package com.docweaver.repository;

import com.docweaver.entity.GeneratedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, UUID> {
}
