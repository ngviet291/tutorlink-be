package org.group3.tutorlink.features.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.auth.enums.RoleName;
import org.group3.tutorlink.features.user.dto.request.ApproveTutorRequest;
import org.group3.tutorlink.features.user.dto.request.UpdateProfileRequest;
import org.group3.tutorlink.features.user.dto.request.UpdateUserStatusRequest;
import org.group3.tutorlink.features.user.dto.response.BaseUserResponse;
import org.group3.tutorlink.features.user.dto.response.TutorResponse;
import org.group3.tutorlink.features.user.enums.UserResponseCode;
import org.group3.tutorlink.features.user.enums.UserStatus;
import org.group3.tutorlink.features.user.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    // GET /v1/users/me
    @GetMapping("/me")
    public ApiResponse<BaseUserResponse> getMyProfile() {
        return ApiResponse.<BaseUserResponse>builder()
                .code(UserResponseCode.USER_FOUND.getCode())
                .message(UserResponseCode.USER_FOUND.getMessage())
                .data(userService.getMyProfile())
                .build();
    }

    // PUT /v1/users/me
    @PutMapping("/me")
    public ApiResponse<BaseUserResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.<BaseUserResponse>builder()
                .code(UserResponseCode.PROFILE_UPDATED.getCode())
                .message(UserResponseCode.PROFILE_UPDATED.getMessage())
                .data(userService.updateMyProfile(request))
                .build();
    }

    // GET /v1/users/{id}/public-profile — public
    @GetMapping("/{id}/public-profile")
    public ApiResponse<BaseUserResponse> getPublicProfile(@PathVariable UUID id) {
        return ApiResponse.<BaseUserResponse>builder()
                .code(UserResponseCode.PUBLIC_PROFILE_FOUND.getCode())
                .message(UserResponseCode.PUBLIC_PROFILE_FOUND.getMessage())
                .data(userService.getPublicProfile(id))
                .build();
    }

    // GET /v1/users?role=&status=&cursor=&limit= — ROLE_ADMIN
    @GetMapping
    public ApiResponse<CursorResponse<BaseUserResponse>> getAllUsers(
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.<CursorResponse<BaseUserResponse>>builder()
                .code(UserResponseCode.GET_USERS.getCode())
                .message(UserResponseCode.GET_USERS.getMessage())
                .data(userService.getAllUsers(role, status, cursor, limit))
                .build();
    }

    // GET /v1/users/{id} — ROLE_ADMIN
    @GetMapping("/{id}")
    public ApiResponse<BaseUserResponse> getUserById(@PathVariable UUID id) {
        return ApiResponse.<BaseUserResponse>builder()
                .code(UserResponseCode.USER_FOUND.getCode())
                .message(UserResponseCode.USER_FOUND.getMessage())
                .data(userService.getUserById(id))
                .build();
    }

    // PATCH /v1/users/{id}/approve — ROLE_ADMIN
    @PatchMapping("/{id}/approve")
    public ApiResponse<TutorResponse> approveTutor(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveTutorRequest request) {
        return ApiResponse.<TutorResponse>builder()
                .code(UserResponseCode.TUTOR_APPROVAL_UPDATED.getCode())
                .message(UserResponseCode.TUTOR_APPROVAL_UPDATED.getMessage())
                .data(userService.approveTutor(id, request))
                .build();
    }

    // PATCH /v1/users/{id}/status — ROLE_ADMIN
    @PatchMapping("/{id}/status")
    public ApiResponse<BaseUserResponse> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.<BaseUserResponse>builder()
                .code(UserResponseCode.USER_STATUS_UPDATED.getCode())
                .message(UserResponseCode.USER_STATUS_UPDATED.getMessage())
                .data(userService.updateUserStatus(id, request))
                .build();
    }
}