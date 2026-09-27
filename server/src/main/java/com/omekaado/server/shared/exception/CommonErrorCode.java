package com.omekaado.server.shared.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
@Getter 
public enum CommonErrorCode implements  ErrorCode{
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Invalid information"),
    PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "File too large");

    private final HttpStatus httpStatus;
    private final String message;
}
