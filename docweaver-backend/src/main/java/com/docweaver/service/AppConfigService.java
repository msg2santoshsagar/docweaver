package com.docweaver.service;

import com.docweaver.config.StorageProperties;
import com.docweaver.dto.AppConfigDto;
import com.docweaver.entity.AppConfig;
import com.docweaver.entity.OutputType;
import com.docweaver.repository.AppConfigRepository;
import com.docweaver.util.StorageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
@Service
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;
    private final StorageProperties storageProperties;
    private final StorageUtil storageUtil;

    public AppConfigService(AppConfigRepository appConfigRepository, StorageProperties storageProperties, StorageUtil storageUtil) {
        this.appConfigRepository = appConfigRepository;
        this.storageProperties = storageProperties;
        this.storageUtil = storageUtil;
    }

    @Transactional
    public AppConfig getOrCreate() {
        return appConfigRepository.findById(AppConfig.SINGLETON_ID)
                .orElseGet(() -> {
                    AppConfig config = new AppConfig();
                    config.setId(AppConfig.SINGLETON_ID);
                    config.setOutputFolder(storageProperties.outputDir());
                    config.setDefaultStandaloneOutputType(OutputType.IMAGE);
                    config.setDefaultDeleteOriginals(false);
                    config.setDryRun(false);
                    return appConfigRepository.save(config);
                });
    }

    @Transactional
    public void initializeStorage() throws IOException {
        storageUtil.ensureFolder(storageProperties.uploadsDir());
        storageUtil.ensureFolder(getOrCreate().getOutputFolder());
    }

    @Transactional
    public AppConfigDto read() {
        return toDto(getOrCreate());
    }

    @Transactional
    public AppConfigDto update(AppConfigDto request) {
        AppConfig config = getOrCreate();
        if (request.outputFolder() != null && !request.outputFolder().isBlank()) {
            config.setOutputFolder(request.outputFolder().trim());
        }
        if (request.defaultStandaloneOutputType() != null) {
            config.setDefaultStandaloneOutputType(request.defaultStandaloneOutputType());
        }
        config.setDefaultDeleteOriginals(request.defaultDeleteOriginals());
        config.setDryRun(request.dryRun());
        AppConfig saved = appConfigRepository.save(config);
        try {
            storageUtil.ensureFolder(saved.getOutputFolder());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid output folder: " + ex.getMessage());
        }
        return toDto(saved);
    }

    public AppConfigDto toDto(AppConfig config) {
        return new AppConfigDto(
                config.getOutputFolder(),
                config.getDefaultStandaloneOutputType(),
                Boolean.TRUE.equals(config.getDefaultDeleteOriginals()),
                Boolean.TRUE.equals(config.getDryRun())
        );
    }
}
