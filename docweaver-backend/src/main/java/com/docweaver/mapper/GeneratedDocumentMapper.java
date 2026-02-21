package com.docweaver.mapper;

import com.docweaver.dto.GeneratedDocumentDto;
import com.docweaver.entity.GeneratedDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GeneratedDocumentMapper {

    @Mapping(target = "deleteOriginals", expression = "java(Boolean.TRUE.equals(doc.getDeleteOriginals()))")
    @Mapping(target = "dryRun", expression = "java(Boolean.TRUE.equals(doc.getDryRun()))")
    GeneratedDocumentDto toDto(GeneratedDocument doc);
}
