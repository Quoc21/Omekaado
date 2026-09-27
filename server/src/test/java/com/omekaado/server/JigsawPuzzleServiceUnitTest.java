package com.omekaado.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.LocalDateTime;

import com.omekaado.server.jigsawpuzzle.dto.JigsawPuzzleDto.CreateRequest;
import com.omekaado.server.jigsawpuzzle.dto.JigsawPuzzleDto.CreateResponse;
import com.omekaado.server.jigsawpuzzle.mapper.JigsawPuzzleMapper;
import com.omekaado.server.jigsawpuzzle.mapper.JigsawPuzzleMapperImpl;
import com.omekaado.server.jigsawpuzzle.model.JigsawPuzzle;
import com.omekaado.server.jigsawpuzzle.repository.JigsawPuzzleRepository;
import com.omekaado.server.jigsawpuzzle.service.FileStorageService;
import com.omekaado.server.jigsawpuzzle.service.JigsawPuzzleService;

@ExtendWith(MockitoExtension.class)
public class JigsawPuzzleServiceUnitTest {
    @Mock private JigsawPuzzleRepository pzRepository;
    @Mock private FileStorageService fileStorageService;

    private JigsawPuzzleService pzService;
    private final JigsawPuzzleMapper mapper = new JigsawPuzzleMapperImpl();

    @BeforeEach
    void setUp() {
        pzService = new JigsawPuzzleService(fileStorageService, pzRepository, mapper);
    }

    @Test
    public void createJigsawPuzzleHappyFlow() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "image",                      // form field name
                "puzzle.png",                 // original filename
                "image/png",                  // content type
                new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' } // PNG magic bytes
        );
        given(fileStorageService.store(file)).willReturn("stored.png");

        CreateRequest rq = new CreateRequest(800, 700, 99, 10, 10, 3108);

        JigsawPuzzle returnPuzzle = mapper.toEntity(rq);
        returnPuzzle.setId(1L);
        returnPuzzle.setUserId(1);
        returnPuzzle.setImagePath("stored.png");
        returnPuzzle.setCreatedAt(LocalDateTime.now());
        given(pzRepository.save(any(JigsawPuzzle.class))).willReturn(returnPuzzle);

        CreateResponse res = pzService.createJigsawPuzzle(file, rq);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.imagePath()).isEqualTo("stored.png");
        assertThat(res.width()).isEqualTo(800);
        assertThat(res.height()).isEqualTo(700);
        assertThat(res.pieceNumber()).isEqualTo(99);
        assertThat(res.rowNumber()).isEqualTo(10);
        assertThat(res.columnNumber()).isEqualTo(10);
        assertThat(res.seed()).isEqualTo(3108);

        verify(pzRepository).save(any(JigsawPuzzle.class));
        verify(fileStorageService, never()).delete(anyString());
    }
}
