package org.group3.tutorlink.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * LƯU Ý: cấu trúc (kiểu của code, message) là GIẢ ĐỊNH.
 * Hãy copy đúng cấu trúc từ SubjectResponseCode rồi chỉnh lại cho khớp ApiResponse.
 */
@Getter
@AllArgsConstructor
public enum AddressResponseCode {
    GET_PROVINCES("GET_PROVINCES", "Lấy danh sách tỉnh/thành thành công"),
    GET_WARDS("GET_WARDS", "Lấy danh sách phường/xã thành công");

    private final String code;
    private final String message;
}