package com.omekaado.server.jigsawpuzzle.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.omekaado.server.jigsawpuzzle.dto.JigsawPuzzleDto.CreateRequest;
import com.omekaado.server.jigsawpuzzle.dto.JigsawPuzzleDto.CreateResponse;
import com.omekaado.server.jigsawpuzzle.exception.JigsawPuzzleErrorCode;
import com.omekaado.server.jigsawpuzzle.mapper.JigsawPuzzleMapper;
import com.omekaado.server.jigsawpuzzle.model.JigsawPuzzle;
import com.omekaado.server.jigsawpuzzle.repository.JigsawPuzzleRepository;
import com.omekaado.server.shared.exception.AppException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor 
public class JigsawPuzzleService {
    private final FileStorageService fileStorageService; 
    private final JigsawPuzzleRepository jigsawPuzzleRepository;
    private final JigsawPuzzleMapper jigsawPuzzleMapper;

    public CreateResponse createJigsawPuzzle(MultipartFile file, CreateRequest createRequest) throws IOException{
        if(createRequest.rowNumber() * createRequest.columnNumber() < createRequest.pieceNumber()){
            throw new AppException(JigsawPuzzleErrorCode.PIECE_NUMBER_MISMATCH_ROW_COLUMN);
        }

        validateFile(file);
        String fileName = fileStorageService.store(file);
        Integer userId = 1; // mock now

        JigsawPuzzle pz = jigsawPuzzleMapper.toEntity(createRequest);
        pz.setUserId(userId);
        pz.setImagePath(fileName);
        try{
            JigsawPuzzle saved = jigsawPuzzleRepository.save(pz);

            return jigsawPuzzleMapper.toResponseDTO(saved);
        } catch (RuntimeException ex){ // if validate entity fail after flush() -> don't save to db + delete file
            try{
                fileStorageService.delete(fileName);
            } catch (IOException iex){ // catch ioe of delete to log, delete ioe will not be throw, constraint exception of validate entity will be throw instead
                log.warn("Failed to delete stored file '{}' after save failure", fileName, iex);
            }
            throw ex;
        }
    }

    private void validateFile(MultipartFile file) throws IOException{
        if (file == null || file.isEmpty()){
            throw new AppException(JigsawPuzzleErrorCode.FILE_EMPTY);
        }

        Tika tika = new Tika();
        String realType;
        try (InputStream is = file.getInputStream()) {
            realType = tika.detect(is);
        }
        if (!Set.of("image/jpeg", "image/png").contains(realType)) {
            throw new AppException(JigsawPuzzleErrorCode.FILE_IS_NOT_IMAGE);
        }
    }
}
