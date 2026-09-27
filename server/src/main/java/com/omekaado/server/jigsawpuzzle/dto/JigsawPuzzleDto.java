package com.omekaado.server.jigsawpuzzle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class JigsawPuzzleDto{

    public record CreateRequest (
        @NotNull @Positive Integer width,
        @NotNull @Positive Integer height,
        @NotNull @Positive Integer pieceNumber,
        @NotNull @Positive Integer rowNumber,
        @NotNull @Positive Integer columnNumber,
        @NotNull @Positive Integer seed

    ) { }

    public record CreateResponse (
        Integer id,
        String imagePath,
        Integer width,
        Integer height,
        Integer pieceNumber,
        Integer rowNumber,
        Integer columnNumber,
        Integer seed
    ) {}
}
