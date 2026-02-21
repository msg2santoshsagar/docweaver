package com.docweaver.service;

import com.docweaver.config.StorageProperties;
import com.docweaver.dto.AppConfigDto;
import com.docweaver.entity.AppConfig;
import com.docweaver.mapper.AppConfigMapper;
import com.docweaver.entity.OutputType;
import com.docweaver.repository.AppConfigRepository;
import com.docweaver.util.StorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;
    private final StorageProperties storageProperties;
    private final StorageUtil storageUtil;
    private final AppConfigMapper appConfigMapper;

    @Transactional
    public AppConfig getOrCreate() {
        AppConfig config = appConfigRepository.findById(AppConfig.SINGLETON_ID)
                .orElseGet(() -> {
                    AppConfig newConfig = new AppConfig();
                    newConfig.setId(AppConfig.SINGLETON_ID);
                    newConfig.setOutputFolder(storageProperties.outputDir());
                    newConfig.setDefaultStandaloneOutputType(OutputType.IMAGE);
                    newConfig.setDefaultDeleteOriginals(false);
                    newConfig.setDryRun(false);
                    newConfig.setAiEnabled(true);
                    newConfig.setAiModel("qwen2.5vl:7b");
                    newConfig.setAiBaseUrl("http://host.docker.internal:11434");
                    return appConfigRepository.save(newConfig);
                });
        boolean changed = false;
        if (config.getAiEnabled() == null) {
            config.setAiEnabled(true);
            changed = true;
        }
        if (config.getAiModel() == null || config.getAiModel().isBlank()) {
            config.setAiModel("qwen2.5vl:7b");
            changed = true;
        }
        if (config.getAiBaseUrl() == null || config.getAiBaseUrl().isBlank()) {
            config.setAiBaseUrl("http://host.docker.internal:11434");
            changed = true;
        }
        if (changed) {
            return appConfigRepository.save(config);
        }
        return config;
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
        config.setAiEnabled(request.aiEnabled());
        if (request.aiModel() != null && !request.aiModel().isBlank()) {
            config.setAiModel(request.aiModel().trim());
        }
        if (request.aiBaseUrl() != null && !request.aiBaseUrl().isBlank()) {
            config.setAiBaseUrl(request.aiBaseUrl().trim());
        }
        AppConfig saved = appConfigRepository.save(config);
        try {
            storageUtil.ensureFolder(saved.getOutputFolder());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid output folder: " + ex.getMessage());
        }
        return toDto(saved);
    }

    public AppConfigDto toDto(AppConfig config) {
        return appConfigMapper.toDto(config);
    }
}
