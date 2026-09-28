package org.group3.tutorlink.common.utils;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.common.dto.response.ErrorResponse;
import org.group3.tutorlink.common.exception.ErrorCode;
import org.group3.tutorlink.features.auth.exception.UnauthenticatedException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
@NoArgsConstructor
public class AppUtil {

    public UUID generateUUID() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    public Date expirationDate(long seconds) {
        return Date.from(Instant.now().plus(seconds, ChronoUnit.SECONDS));
    }

    public String generateOpaqueToken() {
        return generateUUID().toString();
    }

    public ErrorResponse generateErrorResponse(HttpServletRequest request, ErrorCode errorCode) {
        return ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(errorCode.getHttpStatus().value())
                .error(HttpStatus.valueOf(errorCode.getHttpStatus().value()).getReasonPhrase())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build();
    }


    public static AppUtil builder() {
        return new AppUtil();
    }


    /**
     * Get the user ID from the authentication context.
     *
     * @return the user ID as a UUID
     * @throws UnauthenticatedException if the user is not authenticated or the principal is invalid
     */
    public UUID userIdFromAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedException();
        }

        String principal = authentication.getName();
        if (principal == null || principal.isBlank()) {
            throw new UnauthenticatedException();
        }

        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException ex) {
            throw new UnauthenticatedException();
        }
    }


}