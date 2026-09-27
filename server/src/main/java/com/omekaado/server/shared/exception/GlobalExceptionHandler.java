package com.omekaado.server.shared.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ProblemDetail handleAppException(AppException ex) {
        return build(ex.getErrorCode());
    }

    // user upload image
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return build(CommonErrorCode.PAYLOAD_TOO_LARGE);
    }

    // @Valid for RequestBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = build(CommonErrorCode.VALIDATION_FAILED);

        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("field", fe.getField());
                m.put("code", fe.getCode());
                m.put("rejectedValue", fe.getRejectedValue());
                m.put("message", fe.getDefaultMessage());
                return m;
            })
            .toList();

        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedException(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(CommonErrorCode.INTERNAL_ERROR);
    }

    private ProblemDetail build(ErrorCode ec) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ec.getHttpStatus(), ec.getMessage());
        pd.setProperty("code", ec.name());
        return pd;
    }
}
