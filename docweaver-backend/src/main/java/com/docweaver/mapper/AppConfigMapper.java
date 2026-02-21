package com.docweaver.mapper;

import com.docweaver.dto.AppConfigDto;
import com.docweaver.entity.AppConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppConfigMapper {

    @Mapping(target = "defaultDeleteOriginals", expression = "java(Boolean.TRUE.equals(config.getDefaultDeleteOriginals()))")
    @Mapping(target = "dryRun", expression = "java(Boolean.TRUE.equals(config.getDryRun()))")
    @Mapping(target = "aiEnabled", expression = "java(Boolean.TRUE.equals(config.getAiEnabled()))")
    AppConfigDto toDto(AppConfig config);
}
