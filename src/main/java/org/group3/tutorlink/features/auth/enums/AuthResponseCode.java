package org.group3.tutorlink.features.auth.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
@RequiredArgsConstructor
public enum AuthResponseCode implements ResponseCode {

    REGISTER_SUCCESS("AUTH_201", "Register successfully"),
    LOGIN_SUCCESS("AUTH_200", "Login successfully"),
    TOKEN_REFRESH_SUCCESS("AUTH_200", "Refresh token successfully"),
    LOGOUT_SUCCESS("AUTH_200", "Logout successfully");

    private final String code;
    private final String message;
}