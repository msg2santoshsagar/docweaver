package com.docweaver.service;

import com.docweaver.config.StorageProperties;
import com.docweaver.dto.ImageAssetDto;
import com.docweaver.entity.ImageAsset;
import com.docweaver.entity.ImageMode;
import com.docweaver.mapper.ImageAssetMapper;
import com.docweaver.repository.DocumentImageRepository;
import com.docweaver.repository.ImageAssetRepository;
import com.docweaver.util.FilenameUtil;
import com.docweaver.util.StorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageAssetRepository imageAssetRepository;
    private final DocumentImageRepository documentImageRepository;
    private final StorageProperties storageProperties;
    private final StorageUtil storageUtil;
    private final ImageAssetMapper imageAssetMapper;

    @Transactional
    public List<ImageAssetDto> upload(List<MultipartFile> files) {
        Path uploadsRoot;
        try {
            uploadsRoot = storageUtil.ensureFolder(storageProperties.uploadsDir());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize upload folder", e);
        }

        return files.stream().map(file -> {
            String originalName = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
            String ext = FilenameUtil.extensionFromFileName(originalName);
            String baseName = FilenameUtil.sanitizeBaseName(originalName.contains(".")
                    ? originalName.substring(0, originalName.lastIndexOf('.'))
                    : originalName);
            Path target = storageUtil.uniquePath(uploadsRoot, baseName, ext);

            try {
                file.transferTo(target);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to store file: " + originalName, ex);
            }

            ImageAsset image = new ImageAsset();
            image.setOriginalFileName(originalName);
            image.setDisplayName(baseName);
            image.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            image.setOriginalPath(target.toString());
            image.setFileSize(file.getSize());
            image.setMode(ImageMode.STANDALONE);
            image.setUploadedAt(OffsetDateTime.now());
            return toDto(imageAssetRepository.save(image));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<ImageAssetDto> list() {
        return imageAssetRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public ImageAssetDto rename(UUID imageId, String displayName) {
        ImageAsset image = getEntity(imageId);
        image.setDisplayName(displayName.trim());
        return toDto(imageAssetRepository.save(image));
    }

    @Transactional
    public ImageAssetDto updateMode(UUID imageId, ImageMode mode) {
        ImageAsset image = getEntity(imageId);
        image.setMode(mode);
        return toDto(imageAssetRepository.save(image));
    }

    @Transactional
    public void removeFromWorkspace(UUID imageId) {
        ImageAsset image = getEntity(imageId);
        if (!documentImageRepository.findByImageAsset_Id(imageId).isEmpty()) {
            throw new IllegalArgumentException("Image belongs to a group. Remove it from group first.");
        }
        imageAssetRepository.delete(image);
    }

    @Transactional(readOnly = true)
    public ImageAsset getEntity(UUID imageId) {
        return imageAssetRepository.findById(imageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
    }

    @Transactional(readOnly = true)
    public byte[] loadImageBytes(UUID imageId) {
        ImageAsset image = getEntity(imageId);
        try {
            return Files.readAllBytes(Path.of(image.getOriginalPath()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image file missing");
        }
    }

    public ImageAssetDto toDto(ImageAsset image) {
        return imageAssetMapper.toDto(image);
    }
}
