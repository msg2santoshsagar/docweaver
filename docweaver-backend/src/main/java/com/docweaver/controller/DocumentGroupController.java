package com.docweaver.controller;

import com.docweaver.dto.DocumentGroupDto;
import com.docweaver.dto.DocumentGroupRequest;
import com.docweaver.dto.ReorderGroupRequest;
import com.docweaver.dto.UpdatePageRotationRequest;
import com.docweaver.service.DocumentGroupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
public class DocumentGroupController {

    private final DocumentGroupService documentGroupService;

    public DocumentGroupController(DocumentGroupService documentGroupService) {
        this.documentGroupService = documentGroupService;
    }

    @PostMapping
    public DocumentGroupDto create(@Valid @RequestBody DocumentGroupRequest request) {
        return documentGroupService.create(request.name(), request.imageIds());
    }

    @PutMapping("/{groupId}")
    public DocumentGroupDto update(@PathVariable UUID groupId, @Valid @RequestBody DocumentGroupRequest request) {
        return documentGroupService.update(groupId, request.name(), request.imageIds());
    }

    @PutMapping("/{groupId}/reorder")
    public DocumentGroupDto reorder(@PathVariable UUID groupId, @Valid @RequestBody ReorderGroupRequest request) {
        return documentGroupService.reorder(groupId, request.orderedImageIds());
    }

    @PatchMapping("/{groupId}/images/{imageId}/rotation")
    public DocumentGroupDto updatePageRotation(@PathVariable UUID groupId,
                                               @PathVariable UUID imageId,
                                               @Valid @RequestBody UpdatePageRotationRequest request) {
        return documentGroupService.updatePageRotation(groupId, imageId, request.rotationDegrees());
    }

    @DeleteMapping("/{groupId}")
    public void delete(@PathVariable UUID groupId) {
        documentGroupService.delete(groupId);
    }

    @GetMapping
    public List<DocumentGroupDto> list() {
        return documentGroupService.list();
    }
}
