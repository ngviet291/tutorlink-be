package org.group3.tutorlink.common.exception;

import org.group3.tutorlink.common.dto.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ErrorResponse response = ErrorResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        ErrorResponse response = ErrorResponse.builder()
                        .code("SYSTEM_001")
                        .message("Internal server error")
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity.internalServerError().body(response);
    }
}