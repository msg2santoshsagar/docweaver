package com.docweaver.repository;

import com.docweaver.entity.NamePattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NamePatternRepository extends JpaRepository<NamePattern, Long> {
    Optional<NamePattern> findFirstByDocTypeOrderByUpdatedAtDesc(String docType);
}
