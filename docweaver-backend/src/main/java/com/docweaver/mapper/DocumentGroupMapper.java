package com.docweaver.mapper;

import com.docweaver.dto.DocumentGroupDto;
import com.docweaver.dto.ImageAssetDto;
import com.docweaver.entity.DocumentGroup;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface DocumentGroupMapper {
    DocumentGroupDto toDto(DocumentGroup group, List<ImageAssetDto> images, Map<UUID, Integer> rotationByImageId);
}
