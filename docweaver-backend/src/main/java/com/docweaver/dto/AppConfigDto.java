package com.docweaver.dto;

import com.docweaver.entity.OutputType;

public record AppConfigDto(
        String outputFolder,
        OutputType defaultStandaloneOutputType,
        boolean defaultDeleteOriginals,
        boolean dryRun,
        boolean aiEnabled,
        String aiModel,
        String aiBaseUrl
) {
}
