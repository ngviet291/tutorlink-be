package org.group3.tutorlink.features.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.auth.dto.request.*;
import org.group3.tutorlink.features.auth.dto.response.AuthenticateResponse;
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
    public ApiResponse<AuthenticateResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.success(authService.login(req));
    }
    // POST /v1/auth/register/student
    @PostMapping("/register/student")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthenticateResponse> registerStudent(@Valid @RequestBody RegisterStudentRequest req) {
        return ApiResponse.success(authService.registerStudent(req));
    }

    // POST /v1/auth/register/tutor
    @PostMapping("/register/tutor")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthenticateResponse> registerTutor(@Valid @RequestBody RegisterTutorRequest req) {
        return ApiResponse.success(authService.registerTutor(req));
    }

    // POST /v1/auth/refresh-token
    @PostMapping("/refresh-token")
    public ApiResponse<AuthenticateResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest req) {
        return ApiResponse.success(authService.refreshToken(req));
    }

    // POST /v1/auth/logout
    @PostMapping("/logout")
    public ApiResponse<String> logout(@Valid @RequestBody LogoutRequest req) {
        authService.logout(req);
        return ApiResponse.success("Logout successfully");
    }
}
