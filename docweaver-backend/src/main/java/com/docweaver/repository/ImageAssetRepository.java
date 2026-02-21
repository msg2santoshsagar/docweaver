package com.docweaver.repository;

import com.docweaver.entity.ImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, UUID> {
}
