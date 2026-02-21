package com.docweaver.mapper;

import com.docweaver.dto.ImageAssetDto;
import com.docweaver.entity.ImageAsset;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImageAssetMapper {
    ImageAssetDto toDto(ImageAsset image);
}
