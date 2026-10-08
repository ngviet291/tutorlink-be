package org.group3.tutorlink.features.payment.controller;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.payment.dto.response.WalletBalanceResponse;
import org.group3.tutorlink.features.payment.enums.PaymentResponseCode;
import org.group3.tutorlink.features.payment.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 1. Xem số dư ví - chỉ Tutor
    @GetMapping("/wallet/balance")
    public ApiResponse<WalletBalanceResponse> getWalletBalance() {
        return ApiResponse.<WalletBalanceResponse>builder()
                .code(PaymentResponseCode.GET_WALLET_BALANCE.getCode())
                .message(PaymentResponseCode.GET_WALLET_BALANCE.getMessage())
                .data(paymentService.getWalletBalance())
                .build();
    }
}
