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
@Table(name = "generated_documents")
public class GeneratedDocument {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GeneratedType type;

    private UUID sourceImageId;

    private UUID sourceGroupId;

    @Column(nullable = false)
    private String outputPath;

    @Column(nullable = false)
    private String outputName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;

    @Column(nullable = false)
    private Boolean deleteOriginals;

    @Column(nullable = false)
    private Boolean dryRun;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GeneratedType getType() {
        return type;
    }

    public void setType(GeneratedType type) {
        this.type = type;
    }

    public UUID getSourceImageId() {
        return sourceImageId;
    }

    public void setSourceImageId(UUID sourceImageId) {
        this.sourceImageId = sourceImageId;
    }

    public UUID getSourceGroupId() {
        return sourceGroupId;
    }

    public void setSourceGroupId(UUID sourceGroupId) {
        this.sourceGroupId = sourceGroupId;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getOutputName() {
        return outputName;
    }

    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
    }

    public Boolean getDeleteOriginals() {
        return deleteOriginals;
    }

    public void setDeleteOriginals(Boolean deleteOriginals) {
        this.deleteOriginals = deleteOriginals;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
