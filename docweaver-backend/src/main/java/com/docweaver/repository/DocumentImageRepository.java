package com.docweaver.repository;

import com.docweaver.entity.DocumentGroup;
import com.docweaver.entity.DocumentImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentImageRepository extends JpaRepository<DocumentImage, Long> {
    List<DocumentImage> findByDocumentGroupOrderBySortOrderAsc(DocumentGroup group);

    List<DocumentImage> findByImageAsset_Id(UUID imageId);

    boolean existsByDocumentGroup(DocumentGroup group);

    void deleteByDocumentGroup(DocumentGroup group);
}
