package org.group3.tutorlink.features.payment.controller;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.payment.dto.response.BankResponse;
import org.group3.tutorlink.features.payment.enums.PaymentResponseCode;
import org.group3.tutorlink.features.payment.service.BankService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    // Danh sách ngân hàng (nguồn VietQR) cho form rút tiền; yêu cầu đăng nhập như các API khác.
    @GetMapping
    public ApiResponse<List<BankResponse>> getBanks() {
        return ApiResponse.<List<BankResponse>>builder()
                .code(PaymentResponseCode.GET_BANKS.getCode())
                .message(PaymentResponseCode.GET_BANKS.getMessage())
                .data(bankService.getBanks())
                .build();
    }
}