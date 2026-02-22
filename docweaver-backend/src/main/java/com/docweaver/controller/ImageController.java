package com.docweaver.controller;

import com.docweaver.dto.ImageAssetDto;
import com.docweaver.dto.RenameImageRequest;
import com.docweaver.dto.UpdateImageRotationRequest;
import com.docweaver.dto.UpdateImageModeRequest;
import com.docweaver.dto.UploadResponse;
import com.docweaver.entity.ImageAsset;
import com.docweaver.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestPart("files") List<MultipartFile> files) {
        return new UploadResponse(imageService.upload(files));
    }

    @GetMapping
    public List<ImageAssetDto> list() {
        return imageService.list();
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID imageId) {
        imageService.removeFromWorkspace(imageId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{imageId}/rename")
    public ImageAssetDto rename(@PathVariable UUID imageId, @Valid @org.springframework.web.bind.annotation.RequestBody RenameImageRequest request) {
        return imageService.rename(imageId, request.displayName());
    }

    @PatchMapping("/{imageId}/mode")
    public ImageAssetDto updateMode(@PathVariable UUID imageId, @Valid @org.springframework.web.bind.annotation.RequestBody UpdateImageModeRequest request) {
        return imageService.updateMode(imageId, request.mode());
    }

    @PatchMapping("/{imageId}/rotation")
    public ImageAssetDto updateRotation(@PathVariable UUID imageId, @Valid @org.springframework.web.bind.annotation.RequestBody UpdateImageRotationRequest request) {
        return imageService.updateRotation(imageId, request.rotationDegrees());
    }

    @GetMapping("/{imageId}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable UUID imageId) {
        ImageAsset image = imageService.getEntity(imageId);
        byte[] bytes = imageService.loadImageBytes(imageId);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, image.getMimeType())
                .body(bytes);
    }
}
