package org.group3.tutorlink.features.user.enums;


import lombok.*;

@Getter
public enum UserResponseCode {
    USER_FOUND("USER_001", "Get user information successfully"),
    PROFILE_UPDATED("USER_002", "Update user profile successfully"),
    PUBLIC_PROFILE_FOUND("USER_003", "Get public profile information successfully"),
    TUTOR_APPROVAL_UPDATED("USER_004", "Update tutor approval status successfully"),
    GET_USERS("USER_005", "Get users successfully"),
    USER_STATUS_UPDATED("USER_006", "Update user status successfully"),
    CERTIFICATES_FOUND("USER_007", "Get certificates successfully"),
    CERTIFICATES_UPLOADED("USER_008", "Upload certificates successfully, they will be reviewed shortly"),
    CERTIFICATE_DELETED("USER_009", "Delete certificate successfully"),
    PENDING_CERTIFICATES_FOUND("USER_010", "Get certificates waiting for admin review successfully"),
    CERTIFICATE_REVIEWED("USER_011", "Review certificate successfully"),
    TUTOR_CERTIFICATES_FOUND("USER_012", "Get tutor certificates successfully"),
    ;

    private final String code;
    private final String message;
    UserResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}