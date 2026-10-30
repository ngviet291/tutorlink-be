package org.group3.tutorlink.features.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.payment.dto.request.CreateFeeConfigRequest;
import org.group3.tutorlink.features.payment.dto.request.TopUpRequest;
import org.group3.tutorlink.features.payment.dto.response.FeeConfigResponse;
import org.group3.tutorlink.features.payment.dto.response.RevenueStatisticsResponse;
import org.group3.tutorlink.features.payment.dto.response.TransactionResponse;
import org.group3.tutorlink.features.payment.dto.response.WalletBalanceResponse;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.enums.PaymentResponseCode;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.group3.tutorlink.features.payment.service.PaymentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 1. Xem số dư ví - Tutor
    @GetMapping("/wallet/balance")
    public ApiResponse<WalletBalanceResponse> getWalletBalance() {
        return ApiResponse.<WalletBalanceResponse>builder()
                .code(PaymentResponseCode.GET_WALLET_BALANCE.getCode())
                .message(PaymentResponseCode.GET_WALLET_BALANCE.getMessage())
                .data(paymentService.getWalletBalance())
                .build();
    }

    // 2. Nạp tiền vào ví qua MoMo - Tutor (trả paymentLink để chuyển sang trang thanh toán)
    @PostMapping("/wallet/top-up")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> topUp(@RequestBody @Valid TopUpRequest request) {
        return ApiResponse.<TransactionResponse>builder()
                .code(PaymentResponseCode.TOP_UP_SUCCESS.getCode())
                .message(PaymentResponseCode.TOP_UP_SUCCESS.getMessage())
                .data(paymentService.topUp(request))
                .build();
    }

    // 3. Thanh toán phí hoa hồng - chỉ Tutor của application đó
    @PostMapping("/applications/{id}/pay")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> payCommission(@PathVariable UUID id) {
        return ApiResponse.<TransactionResponse>builder()
                .code(PaymentResponseCode.PAY_COMMISSION_SUCCESS.getCode())
                .message(PaymentResponseCode.PAY_COMMISSION_SUCCESS.getMessage())
                .data(paymentService.payCommission(id))
                .build();
    }

    // 4. Lịch sử giao dịch - Tutor
    @GetMapping("/history")
    public ApiResponse<CursorResponse<TransactionResponse>> getMyTransactions(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "10") int limit) {

        return ApiResponse.<CursorResponse<TransactionResponse>>builder()
                .code(PaymentResponseCode.GET_PAYMENT_HISTORY.getCode())
                .message(PaymentResponseCode.GET_PAYMENT_HISTORY.getMessage())
                .data(paymentService.getMyTransactions(cursor, limit))
                .build();
    }

    // 5. Danh sách giao dịch - Admin (có thể lọc theo status và tutorId)
    @GetMapping
    public ApiResponse<CursorResponse<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "10") int limit) {

        return ApiResponse.<CursorResponse<TransactionResponse>>builder()
                .code(PaymentResponseCode.GET_PAYMENTS.getCode())
                .message(PaymentResponseCode.GET_PAYMENTS.getMessage())
                .data(paymentService.getAllTransactions(status, cursor, limit))
                .build();
    }

    // 5.1. Chi tiết giao dịch - Admin hoặc chủ giao dịch
    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> getTransactionDetail(@PathVariable UUID id) {
        return ApiResponse.<TransactionResponse>builder()
                .code(PaymentResponseCode.GET_PAYMENT_DETAIL.getCode())
                .message(PaymentResponseCode.GET_PAYMENT_DETAIL.getMessage())
                .data(paymentService.getTransactionDetail(id))
                .build();
    }

    // 6. Hoàn tiền - Admin
    @PostMapping("/{id}/refund")
    public ApiResponse<TransactionResponse> refundTransaction(@PathVariable UUID id) {
        return ApiResponse.<TransactionResponse>builder()
                .code(PaymentResponseCode.REFUND_SUCCESS.getCode())
                .message(PaymentResponseCode.REFUND_SUCCESS.getMessage())
                .data(paymentService.refundTransaction(id))
                .build();
    }

    // 7. Thống kê doanh thu - Admin
    @GetMapping("/statistics/revenue")
    public ApiResponse<RevenueStatisticsResponse> getRevenueStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ApiResponse.<RevenueStatisticsResponse>builder()
                .code(PaymentResponseCode.GET_REVENUE_STATISTICS.getCode())
                .message(PaymentResponseCode.GET_REVENUE_STATISTICS.getMessage())
                .data(paymentService.getRevenueStatistics(from, to))
                .build();
    }

    // 8. Trạng thái giao dịch nạp tiền - Tutor (FE gọi sau khi user quay về từ MoMo)
    @GetMapping("/wallet/top-up/{id}")
    public ApiResponse<TransactionResponse> getTopUpStatus(@PathVariable UUID id) {
        return ApiResponse.<TransactionResponse>builder()
                .code(PaymentResponseCode.GET_TOP_UP_STATUS.getCode())
                .message(PaymentResponseCode.GET_TOP_UP_STATUS.getMessage())
                .data(paymentService.syncTopUpStatus(id))
                .build();
    }

    // 9. IPN callback từ MoMo (public endpoint, xác thực bằng chữ ký HMAC-SHA256)
    // MoMo yêu cầu phản hồi HTTP 204 No Content
    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> momoIpn(@RequestBody String rawBody) {
        paymentService.handleGatewayCallback(PaymentMethod.MOMO, rawBody);
        return ResponseEntity.noContent().build();
    }

    // 10. Lịch sử cấu hình phí - Admin
    @GetMapping("/fee-configs")
    public ApiResponse<CursorResponse<FeeConfigResponse>> getFeeConfigs(
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "10") int limit) {

        return ApiResponse.<CursorResponse<FeeConfigResponse>>builder()
                .code(PaymentResponseCode.GET_FEE_CONFIGS.getCode())
                .message(PaymentResponseCode.GET_FEE_CONFIGS.getMessage())
                .data(paymentService.getFeeConfigs(cursor, limit))
                .build();
    }

    // 12. Tạo cấu hình phí mới - Admin
    @PostMapping("/fee-configs")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FeeConfigResponse> createFeeConfig(@RequestBody @Valid CreateFeeConfigRequest request) {
        return ApiResponse.<FeeConfigResponse>builder()
                .code(PaymentResponseCode.CREATE_FEE_CONFIG.getCode())
                .message(PaymentResponseCode.CREATE_FEE_CONFIG.getMessage())
                .data(paymentService.createFeeConfig(request))
                .build();
    }
}
