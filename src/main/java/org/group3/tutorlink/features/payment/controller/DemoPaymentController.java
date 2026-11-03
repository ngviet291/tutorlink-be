package org.group3.tutorlink.features.payment.controller;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.payment.dto.response.TransactionResponse;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.gateway.mock.MockPaymentGateway;
import org.group3.tutorlink.features.payment.service.PaymentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Cờ demo riêng cho phát triển, không thay profile hay cấu hình CSDL. */
@RestController
@ConditionalOnProperty(name = "payment.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/v1/payments/demo")
public class DemoPaymentController {
    private final PaymentService paymentService;
    private final MockPaymentGateway gateway;

    @PostMapping("/top-up/{id}/confirm")
    @PreAuthorize("hasAuthority('TUTOR')")
    public ApiResponse<TransactionResponse> confirm(@PathVariable UUID id) {
        // Kiểm tra chủ sở hữu và loại giao dịch trước khi thay đổi cổng giả lập.
        TransactionResponse transaction = paymentService.syncTopUpStatus(id);
        if (transaction.getStatus() == TransactionStatus.PENDING) {
            gateway.confirmDemo(transaction.getProviderOrderId());
            // Dùng khóa giao dịch và cơ chế cộng ví một lần của luồng hiện có.
            transaction = paymentService.syncTopUpStatus(id);
        }
        return ApiResponse.<TransactionResponse>builder()
                .message("Giao dịch giả lập phát triển, không thanh toán tiền thật")
                .data(transaction)
                .build();
    }
}
