package org.group3.tutorlink.features.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthResponseCode {

    REGISTER_SUCCESS("AUTH_201", "Register successfully"),
    LOGIN_SUCCESS("AUTH_200", "Login successfully"),
    TOKEN_REFRESH_SUCCESS("AUTH_200", "Refresh token successfully"),
    LOGOUT_SUCCESS("AUTH_200", "Logout successfully");

    private final String code;
    private final String message;
}