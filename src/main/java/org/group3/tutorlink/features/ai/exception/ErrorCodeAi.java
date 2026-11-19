package org.group3.tutorlink.features.ai.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCodeAi implements BaseErrorCode {

    AI_PROVIDER_ERROR(
            "AI_001",
            "AI service is temporarily unavailable",
            HttpStatus.BAD_GATEWAY
    );

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;


}