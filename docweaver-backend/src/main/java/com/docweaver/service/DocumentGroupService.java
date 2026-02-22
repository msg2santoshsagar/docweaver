package com.docweaver.service;

import com.docweaver.dto.DocumentGroupDto;
import com.docweaver.entity.DocumentGroup;
import com.docweaver.entity.DocumentImage;
import com.docweaver.entity.ImageAsset;
import com.docweaver.entity.ImageMode;
import com.docweaver.mapper.DocumentGroupMapper;
import com.docweaver.mapper.ImageAssetMapper;
import com.docweaver.repository.DocumentGroupRepository;
import com.docweaver.repository.DocumentImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentGroupService {

    private final DocumentGroupRepository documentGroupRepository;
    private final DocumentImageRepository documentImageRepository;
    private final ImageService imageService;
    private final ImageAssetMapper imageAssetMapper;
    private final DocumentGroupMapper documentGroupMapper;

    @Transactional
    public DocumentGroupDto create(String name, List<UUID> imageIds) {
        DocumentGroup group = new DocumentGroup();
        group.setName(name.trim());
        DocumentGroup saved = documentGroupRepository.save(group);
        applyGroupImages(saved, imageIds);
        return toDto(saved);
    }

    @Transactional
    public DocumentGroupDto update(UUID groupId, String name, List<UUID> imageIds) {
        DocumentGroup group = getEntity(groupId);
        group.setName(name.trim());
        DocumentGroup saved = documentGroupRepository.save(group);
        applyGroupImages(saved, imageIds);
        return toDto(saved);
    }

    @Transactional
    public DocumentGroupDto reorder(UUID groupId, List<UUID> orderedImageIds) {
        DocumentGroup group = getEntity(groupId);
        List<DocumentImage> entries = orderedEntries(group);
        Map<UUID, DocumentImage> byImageId = entries.stream()
                .collect(Collectors.toMap(entry -> entry.getImageAsset().getId(), Function.identity()));

        if (orderedImageIds.size() != entries.size() || !byImageId.keySet().containsAll(orderedImageIds)) {
            throw new IllegalArgumentException("Ordered images must include exactly all group images");
        }

        for (int i = 0; i < orderedImageIds.size(); i++) {
            DocumentImage entry = byImageId.get(orderedImageIds.get(i));
            entry.setSortOrder(i);
            documentImageRepository.save(entry);
        }
        return toDto(group);
    }

    @Transactional
    public DocumentGroupDto updatePageRotation(UUID groupId, UUID imageId, int rotationDegrees) {
        DocumentGroup group = getEntity(groupId);
        List<DocumentImage> entries = orderedEntries(group);
        DocumentImage target = entries.stream()
                .filter(entry -> entry.getImageAsset().getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Image does not belong to this group"));
        target.setRotationDegrees(normalizeRotation(rotationDegrees));
        documentImageRepository.save(target);
        return toDto(group);
    }

    @Transactional
    public void delete(UUID groupId) {
        DocumentGroup group = getEntity(groupId);
        List<DocumentImage> entries = orderedEntries(group);
        for (DocumentImage entry : entries) {
            entry.getImageAsset().setMode(ImageMode.STANDALONE);
        }
        documentImageRepository.deleteByDocumentGroup(group);
        documentGroupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public List<DocumentGroupDto> list() {
        return documentGroupRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DocumentGroup getEntity(UUID groupId) {
        return documentGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    @Transactional(readOnly = true)
    public List<ImageAsset> orderedImages(DocumentGroup group) {
        return orderedEntries(group)
                .stream()
                .map(DocumentImage::getImageAsset)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentImage> orderedEntries(DocumentGroup group) {
        return documentImageRepository.findByDocumentGroupOrderBySortOrderAsc(group);
    }

    private void applyGroupImages(DocumentGroup group, List<UUID> imageIds) {
        List<DocumentImage> existingRows = orderedEntries(group);
        Map<UUID, Integer> existingRotation = existingRows.stream()
                .collect(Collectors.toMap(row -> row.getImageAsset().getId(), row -> normalizeRotation(row.getRotationDegrees())));
        for (DocumentImage existing : existingRows) {
            existing.getImageAsset().setMode(ImageMode.STANDALONE);
        }
        documentImageRepository.deleteByDocumentGroup(group);
        List<DocumentImage> rows = new ArrayList<>();

        for (int i = 0; i < imageIds.size(); i++) {
            ImageAsset image = imageService.getEntity(imageIds.get(i));
            List<DocumentImage> links = documentImageRepository.findByImageAsset_Id(image.getId());
            for (DocumentImage link : links) {
                if (!link.getDocumentGroup().getId().equals(group.getId())) {
                    documentImageRepository.delete(link);
                }
            }
            image.setMode(ImageMode.GROUPED);

            DocumentImage row = new DocumentImage();
            row.setDocumentGroup(group);
            row.setImageAsset(image);
            row.setSortOrder(i);
            row.setRotationDegrees(existingRotation.getOrDefault(image.getId(), normalizeRotation(image.getRotationDegrees())));
            rows.add(row);
        }
        documentImageRepository.saveAll(rows);
    }

    public DocumentGroupDto toDto(DocumentGroup group) {
        List<DocumentImage> entries = orderedEntries(group);
        List<ImageAsset> images = entries.stream().map(DocumentImage::getImageAsset).toList();
        Map<UUID, Integer> rotationByImageId = entries.stream()
                .collect(Collectors.toMap(entry -> entry.getImageAsset().getId(), entry -> normalizeRotation(entry.getRotationDegrees())));
        return documentGroupMapper.toDto(
                group,
                images.stream().map(imageAssetMapper::toDto).toList(),
                rotationByImageId
        );
    }

    private int normalizeRotation(Integer rotationDegrees) {
        if (rotationDegrees == null) {
            return 0;
        }
        int normalized = rotationDegrees % 360;
        if (normalized < 0) {
            normalized += 360;
        }
        return normalized;
    }
}
