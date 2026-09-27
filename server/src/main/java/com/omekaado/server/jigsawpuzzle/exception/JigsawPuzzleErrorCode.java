package com.omekaado.server.jigsawpuzzle.exception;

import org.springframework.http.HttpStatus;

import com.omekaado.server.shared.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter 
@RequiredArgsConstructor 
public enum JigsawPuzzleErrorCode implements ErrorCode {
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "File null or empty"),
    FILE_IS_NOT_IMAGE(HttpStatus.BAD_REQUEST, "File only accept image/jpeg or image/png"),
    PIECE_NUMBER_MISMATCH_ROW_COLUMN(HttpStatus.BAD_REQUEST, "row_number * column_number must be >= piece_number");
    
    private final HttpStatus httpStatus;
    private final String message;
}
