package org.group3.tutorlink.common.utils;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.common.dto.response.ErrorResponse;
import org.group3.tutorlink.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
@Component
@NoArgsConstructor
public class AppUtil {

    public static UUID generateUUID() {
        return UuidCreator.getTimeOrderedEpoch();
    }
    public static Date expirationDate(long seconds) {
        return Date.from(Instant.now().plus(seconds, ChronoUnit.SECONDS));
    }
    public static String generateOpaqueToken() {
        return generateUUID().toString();
    }

    public static ErrorResponse generateErrorResponse(HttpServletRequest request, ErrorCode errorCode) {
        return ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(errorCode.getHttpStatus().value())
                .error(HttpStatus.valueOf(errorCode.getHttpStatus().value()).getReasonPhrase())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build();
    }
}