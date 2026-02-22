package com.docweaver.service;

import com.docweaver.dto.ProcessRequest;
import com.docweaver.dto.ProcessResponse;
import com.docweaver.dto.ProcessResultItemDto;
import com.docweaver.dto.ProcessStandaloneItem;
import com.docweaver.entity.AppConfig;
import com.docweaver.entity.DocumentGroup;
import com.docweaver.entity.DocumentImage;
import com.docweaver.entity.GeneratedDocument;
import com.docweaver.entity.GeneratedType;
import com.docweaver.entity.ImageAsset;
import com.docweaver.entity.OutputType;
import com.docweaver.entity.ProcessingStatus;
import com.docweaver.repository.DocumentGroupRepository;
import com.docweaver.repository.DocumentImageRepository;
import com.docweaver.repository.GeneratedDocumentRepository;
import com.docweaver.repository.ImageAssetRepository;
import com.docweaver.util.FilenameUtil;
import com.docweaver.util.ImageTransformUtil;
import com.docweaver.util.PdfUtil;
import com.docweaver.util.StorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessingService {

    private final AppConfigService appConfigService;
    private final ImageService imageService;
    private final DocumentGroupService documentGroupService;
    private final GeneratedDocumentRepository generatedDocumentRepository;
    private final ImageAssetRepository imageAssetRepository;
    private final DocumentImageRepository documentImageRepository;
    private final DocumentGroupRepository documentGroupRepository;
    private final StorageUtil storageUtil;
    private final PdfUtil pdfUtil;
    private final ImageTransformUtil imageTransformUtil;

    @Transactional
    public ProcessResponse process(ProcessRequest request) {
        AppConfig config = appConfigService.getOrCreate();
        boolean dryRun = Boolean.TRUE.equals(config.getDryRun());
        boolean deleteOriginals = request.deleteOriginals() == null
                ? Boolean.TRUE.equals(config.getDefaultDeleteOriginals())
                : request.deleteOriginals();

        Path outputRoot;
        try {
            outputRoot = storageUtil.ensureFolder(config.getOutputFolder());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid output folder");
        }

        List<ProcessResultItemDto> results = new ArrayList<>();
        List<Path> originalsToDelete = new ArrayList<>();
        boolean overallSuccess = true;

        for (ProcessStandaloneItem item : request.safeStandaloneItems()) {
            ResultWithOriginal result = processStandalone(item, outputRoot, dryRun, deleteOriginals);
            results.add(result.result());
            if (result.result().status() == ProcessingStatus.SUCCESS && result.originalPath() != null) {
                originalsToDelete.add(result.originalPath());
            } else {
                overallSuccess = false;
            }
        }

        for (UUID groupId : request.safeGroupIds()) {
            ResultWithOriginal result = processGroup(groupId, outputRoot, dryRun, deleteOriginals);
            results.add(result.result());
            if (result.result().status() != ProcessingStatus.SUCCESS) {
                overallSuccess = false;
            } else if (result.groupOriginals() != null) {
                originalsToDelete.addAll(result.groupOriginals());
            }
        }

        boolean originalsDeleted = false;
        if (deleteOriginals && overallSuccess && !dryRun) {
            List<Path> uniquePaths = new ArrayList<>(new LinkedHashSet<>(originalsToDelete));
            originalsDeleted = deleteOriginalFiles(uniquePaths);
            if (!originalsDeleted) {
                overallSuccess = false;
            } else {
                removeDeletedOriginalsFromWorkspace(uniquePaths);
            }
        }

        if (deleteOriginals && !overallSuccess) {
            originalsDeleted = false;
        }

        return new ProcessResponse(
                overallSuccess,
                deleteOriginals,
                originalsDeleted,
                dryRun,
                results
        );
    }

    private ResultWithOriginal processStandalone(ProcessStandaloneItem item, Path outputRoot, boolean dryRun,
                                                 boolean deleteOriginals) {
        ImageAsset image = imageService.getEntity(item.imageId());

        String baseName = FilenameUtil.sanitizeBaseName(image.getDisplayName());
        OutputType outputType = item.outputType() == null
                ? appConfigService.getOrCreate().getDefaultStandaloneOutputType()
                : item.outputType();
        int standaloneRotation = normalizeRotation(image.getRotationDegrees());

        String extension = outputType == OutputType.PDF
                ? "pdf"
                : FilenameUtil.extensionFromFileName(image.getOriginalFileName());
        if (extension.isBlank()) {
            extension = outputType == OutputType.PDF ? "pdf" : "img";
        }

        Path target = storageUtil.uniquePath(outputRoot, baseName, extension);

        try {
            if (!dryRun) {
                if (outputType == OutputType.PDF) {
                    pdfUtil.createMultiImagePdf(
                            List.of(new PdfUtil.PageInput(Path.of(image.getOriginalPath()), standaloneRotation)),
                            target
                    );
                } else {
                    if (standaloneRotation == 0) {
                        Files.copy(Path.of(image.getOriginalPath()), target);
                    } else {
                        byte[] rotated = imageTransformUtil.readRotatedImage(
                                Path.of(image.getOriginalPath()),
                                standaloneRotation,
                                extension
                        );
                        Files.write(target, rotated);
                    }
                }
            }

            GeneratedDocument doc = new GeneratedDocument();
            doc.setType(outputType == OutputType.PDF ? GeneratedType.STANDALONE_PDF : GeneratedType.STANDALONE_IMAGE);
            doc.setSourceImageId(image.getId());
            doc.setOutputPath(target.toString());
            doc.setOutputName(target.getFileName().toString());
            doc.setDeleteOriginals(deleteOriginals);
            doc.setDryRun(dryRun);
            doc.setStatus(ProcessingStatus.SUCCESS);
            doc.setMessage(dryRun ? "Dry-run validated, no file written" : "Generated successfully");
            GeneratedDocument saved = generatedDocumentRepository.save(doc);

            return new ResultWithOriginal(
                    new ProcessResultItemDto(saved.getId(), saved.getType(), saved.getSourceImageId(), null,
                            saved.getOutputPath(), saved.getOutputName(), saved.getStatus(), saved.getMessage()),
                    Path.of(image.getOriginalPath()),
                    null
            );
        } catch (Exception ex) {
            String failureMessage = "Failed: " + ex.getMessage();
            if (isSourceMissing(image)) {
                removeStaleStandaloneImage(image);
                failureMessage = "Failed: original file is missing from uploads. Stale entry removed from workspace.";
            }

            GeneratedDocument doc = new GeneratedDocument();
            doc.setType(outputType == OutputType.PDF ? GeneratedType.STANDALONE_PDF : GeneratedType.STANDALONE_IMAGE);
            doc.setSourceImageId(image.getId());
            doc.setOutputPath(target.toString());
            doc.setOutputName(target.getFileName().toString());
            doc.setDeleteOriginals(deleteOriginals);
            doc.setDryRun(dryRun);
            doc.setStatus(ProcessingStatus.FAILED);
            doc.setMessage(failureMessage);
            GeneratedDocument saved = generatedDocumentRepository.save(doc);
            return new ResultWithOriginal(
                    new ProcessResultItemDto(saved.getId(), saved.getType(), saved.getSourceImageId(), null,
                            saved.getOutputPath(), saved.getOutputName(), saved.getStatus(), saved.getMessage()),
                    null,
                    null
            );
        }
    }

    private ResultWithOriginal processGroup(UUID groupId, Path outputRoot, boolean dryRun, boolean deleteOriginals) {
        DocumentGroup group = documentGroupService.getEntity(groupId);
        List<DocumentImage> entries = documentGroupService.orderedEntries(group);
        List<ImageAsset> images = entries.stream().map(DocumentImage::getImageAsset).toList();
        if (images.isEmpty()) {
            throw new IllegalArgumentException("Group has no images");
        }

        String baseName = FilenameUtil.sanitizeBaseName(group.getName());
        Path target = storageUtil.uniquePath(outputRoot, baseName, "pdf");

        try {
            if (!dryRun) {
                List<PdfUtil.PageInput> pages = entries.stream()
                        .map(entry -> new PdfUtil.PageInput(
                                Path.of(entry.getImageAsset().getOriginalPath()),
                                entry.getRotationDegrees() == null ? 0 : entry.getRotationDegrees()
                        ))
                        .toList();
                pdfUtil.createMultiImagePdf(pages, target);
            }

            GeneratedDocument doc = new GeneratedDocument();
            doc.setType(GeneratedType.GROUP_PDF);
            doc.setSourceGroupId(group.getId());
            doc.setOutputPath(target.toString());
            doc.setOutputName(target.getFileName().toString());
            doc.setDeleteOriginals(deleteOriginals);
            doc.setDryRun(dryRun);
            doc.setStatus(ProcessingStatus.SUCCESS);
            doc.setMessage(dryRun ? "Dry-run validated, no file written" : "Generated successfully");
            GeneratedDocument saved = generatedDocumentRepository.save(doc);

            List<Path> originalPaths = images.stream().map(img -> Path.of(img.getOriginalPath())).toList();
            return new ResultWithOriginal(
                    new ProcessResultItemDto(saved.getId(), saved.getType(), null, saved.getSourceGroupId(),
                            saved.getOutputPath(), saved.getOutputName(), saved.getStatus(), saved.getMessage()),
                    null,
                    originalPaths
            );
        } catch (Exception ex) {
            GeneratedDocument doc = new GeneratedDocument();
            doc.setType(GeneratedType.GROUP_PDF);
            doc.setSourceGroupId(group.getId());
            doc.setOutputPath(target.toString());
            doc.setOutputName(target.getFileName().toString());
            doc.setDeleteOriginals(deleteOriginals);
            doc.setDryRun(dryRun);
            doc.setStatus(ProcessingStatus.FAILED);
            doc.setMessage("Failed: " + ex.getMessage());
            GeneratedDocument saved = generatedDocumentRepository.save(doc);
            return new ResultWithOriginal(
                    new ProcessResultItemDto(saved.getId(), saved.getType(), null, saved.getSourceGroupId(),
                            saved.getOutputPath(), saved.getOutputName(), saved.getStatus(), saved.getMessage()),
                    null,
                    null
            );
        }
    }

    private boolean deleteOriginalFiles(List<Path> files) {
        try {
            for (Path file : files) {
                if (Files.exists(file)) {
                    Files.delete(file);
                }
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private void removeDeletedOriginalsFromWorkspace(List<Path> deletedOriginals) {
        if (deletedOriginals.isEmpty()) {
            return;
        }

        List<String> deletedPaths = deletedOriginals.stream()
                .map(Path::toString)
                .toList();
        List<ImageAsset> deletedAssets = imageAssetRepository.findByOriginalPathIn(deletedPaths);
        if (deletedAssets.isEmpty()) {
            return;
        }

        for (ImageAsset asset : deletedAssets) {
            List<DocumentImage> links = documentImageRepository.findByImageAsset_Id(asset.getId());
            if (!links.isEmpty()) {
                documentImageRepository.deleteAll(links);
            }
        }
        imageAssetRepository.deleteAll(deletedAssets);

        for (DocumentGroup group : documentGroupRepository.findAll()) {
            if (!documentImageRepository.existsByDocumentGroup(group)) {
                documentGroupRepository.delete(group);
            }
        }
    }

    private boolean isSourceMissing(ImageAsset image) {
        try {
            return !Files.exists(Path.of(image.getOriginalPath()));
        } catch (Exception ignored) {
            return true;
        }
    }

    private void removeStaleStandaloneImage(ImageAsset image) {
        if (!documentImageRepository.findByImageAsset_Id(image.getId()).isEmpty()) {
            return;
        }
        imageAssetRepository.delete(image);
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

    private record ResultWithOriginal(ProcessResultItemDto result, Path originalPath, List<Path> groupOriginals) {
    }
}
