package com.omekaado.server.jigsawpuzzle.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.omekaado.server.jigsawpuzzle.dto.JigsawPuzzleDto.CreateRequest;
import com.omekaado.server.jigsawpuzzle.dto.JigsawPuzzleDto.CreateResponse;
import com.omekaado.server.jigsawpuzzle.service.JigsawPuzzleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor 
@RequestMapping("/jigsawpuzzle")
public class JigsawPuzzleController {
    private final JigsawPuzzleService jigsawPuzzleService;

    @PostMapping
    public CreateResponse createJigsawPuzzle(
        @RequestPart("file") MultipartFile file, // Jakarta Validation cannot validate type and empty so move validate to service
        @RequestPart("data") @Valid CreateRequest createRequest
    ) throws IOException {
        return jigsawPuzzleService.createJigsawPuzzle(file, createRequest);
    }
}
