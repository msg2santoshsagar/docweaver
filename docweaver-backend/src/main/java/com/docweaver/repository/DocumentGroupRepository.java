package com.docweaver.repository;

import com.docweaver.entity.DocumentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentGroupRepository extends JpaRepository<DocumentGroup, UUID> {
}
