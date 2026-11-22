package org.group3.tutorlink.common.controller;

import org.group3.tutorlink.common.service.AddressService;
import org.group3.tutorlink.common.dto.response.ProvinceResponse;
import org.group3.tutorlink.common.dto.response.WardResponse;
import org.group3.tutorlink.common.enums.AddressResponseCode;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // 1. Danh sách tỉnh / thành phố (public — dùng ở form đăng ký khi chưa đăng nhập)
    @GetMapping("/provinces")
    public ApiResponse<List<ProvinceResponse>> getProvinces() {
        return ApiResponse.<List<ProvinceResponse>>builder()
                .code(AddressResponseCode.GET_PROVINCES.getCode())
                .message(AddressResponseCode.GET_PROVINCES.getMessage())
                .data(addressService.getProvinces())
                .build();
    }

    // 2. Danh sách phường / xã của một tỉnh
    @GetMapping("/provinces/{code}/wards")
    public ApiResponse<List<WardResponse>> getWards(@PathVariable int code) {
        return ApiResponse.<List<WardResponse>>builder()
                .code(AddressResponseCode.GET_WARDS.getCode())
                .message(AddressResponseCode.GET_WARDS.getMessage())
                .data(addressService.getWards(code))
                .build();
    }
}