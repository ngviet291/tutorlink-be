package org.group3.tutorlink.features.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.auth.dto.request.*;
import org.group3.tutorlink.features.auth.dto.response.AuthenticateResponse;
import org.group3.tutorlink.features.auth.exception.AuthResponseCode;
import org.group3.tutorlink.features.auth.service.AuthenticateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthenticateController {

    private final AuthenticateService authService;

    // POST /v1/auth/login
    @PostMapping("/login")
    public ApiResponse<AuthenticateResponse> login(
            @Valid @RequestBody LoginRequest req) {

        AuthenticateResponse response = authService.login(req);

        return ApiResponse.<AuthenticateResponse>builder()
                .code(AuthResponseCode.LOGIN_SUCCESS.getCode())
                .message(AuthResponseCode.LOGIN_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    // POST /v1/auth/register/student
    @PostMapping("/register/student")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthenticateResponse> registerStudent(
            @Valid @RequestBody RegisterStudentRequest req) {

        AuthenticateResponse response = authService.registerStudent(req);

        return ApiResponse.<AuthenticateResponse>builder()
                .code(AuthResponseCode.REGISTER_SUCCESS.getCode())
                .message(AuthResponseCode.REGISTER_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    // POST /v1/auth/register/tutor
    @PostMapping("/register/tutor")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthenticateResponse> registerTutor(
            @Valid @RequestBody RegisterTutorRequest req) {

        AuthenticateResponse response = authService.registerTutor(req);

        return ApiResponse.<AuthenticateResponse>builder()
                .code(AuthResponseCode.REGISTER_SUCCESS.getCode())
                .message(AuthResponseCode.REGISTER_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    // POST /v1/auth/refresh-token
    @PostMapping("/refresh-token")
    public ApiResponse<AuthenticateResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest req) {

        AuthenticateResponse response = authService.refreshToken(req);

        return ApiResponse.<AuthenticateResponse>builder()
                .code(AuthResponseCode.TOKEN_REFRESH_SUCCESS.getCode())
                .message(AuthResponseCode.TOKEN_REFRESH_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    // POST /v1/auth/logout
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody LogoutRequest req) {

        authService.logout(req);

        return ApiResponse.<Void>builder()
                .code(AuthResponseCode.LOGOUT_SUCCESS.getCode())
                .message(AuthResponseCode.LOGOUT_SUCCESS.getMessage())
                .build();
    }
}