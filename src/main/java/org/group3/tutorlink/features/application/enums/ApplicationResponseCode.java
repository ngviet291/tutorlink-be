package org.group3.tutorlink.features.application.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApplicationResponseCode {
    CREATE_SUCCESS("APP_201", "Application created successfully"),
    GET_SUCCESS("APP_200", "Get application successfully"),
    SELECT_SUCCESS("APP_202", "Application selected successfully"),
    CONFIRM_SUCCESS("APP_203", "Application confirmed successfully"),
    CANCEL_SUCCESS("APP_204", "Application cancelled successfully");

    private final String code;
    private final String message;
}