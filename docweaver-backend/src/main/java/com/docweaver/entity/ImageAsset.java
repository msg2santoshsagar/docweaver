package com.docweaver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "image_assets")
public class ImageAsset {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private String originalPath;

    @Column(nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageMode mode;

    @Column(nullable = false)
    private OffsetDateTime uploadedAt;

    private String aiSuggestedName;

    private String aiDocType;

    private String aiSubject;

    private String aiDocumentDate;

    private String aiGroupKey;

    private Double aiConfidence;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (uploadedAt == null) {
            uploadedAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public ImageMode getMode() {
        return mode;
    }

    public void setMode(ImageMode mode) {
        this.mode = mode;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(OffsetDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getAiSuggestedName() {
        return aiSuggestedName;
    }

    public void setAiSuggestedName(String aiSuggestedName) {
        this.aiSuggestedName = aiSuggestedName;
    }

    public String getAiDocType() {
        return aiDocType;
    }

    public void setAiDocType(String aiDocType) {
        this.aiDocType = aiDocType;
    }

    public String getAiSubject() {
        return aiSubject;
    }

    public void setAiSubject(String aiSubject) {
        this.aiSubject = aiSubject;
    }

    public String getAiDocumentDate() {
        return aiDocumentDate;
    }

    public void setAiDocumentDate(String aiDocumentDate) {
        this.aiDocumentDate = aiDocumentDate;
    }

    public String getAiGroupKey() {
        return aiGroupKey;
    }

    public void setAiGroupKey(String aiGroupKey) {
        this.aiGroupKey = aiGroupKey;
    }

    public Double getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(Double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }
}
