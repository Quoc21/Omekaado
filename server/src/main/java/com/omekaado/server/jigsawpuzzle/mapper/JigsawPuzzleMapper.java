package com.omekaado.server.jigsawpuzzle.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.omekaado.server.config.GlobalMapperConfig;
import com.omekaado.server.jigsawpuzzle.dto.JigsawPuzzleDto.CreateRequest;
import com.omekaado.server.jigsawpuzzle.dto.JigsawPuzzleDto.CreateResponse;
import com.omekaado.server.jigsawpuzzle.model.JigsawPuzzle;


@Mapper(config = GlobalMapperConfig.class)
public interface JigsawPuzzleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    JigsawPuzzle toEntity(CreateRequest createRequest);
    CreateResponse toResponseDTO(JigsawPuzzle puzzle);
}