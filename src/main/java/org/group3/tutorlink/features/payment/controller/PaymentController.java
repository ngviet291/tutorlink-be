package org.group3.tutorlink.features.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.payment.dto.request.TopUpRequest;
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

    // 2. Nạp tiền vào ví qua MoMo/ZaloPay - Tutor (trả paymentLink để chuyển sang trang thanh toán)
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

    // 5. Danh sách giao dịch - Admin
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

    // 8. Trạng thái giao dịch nạp tiền - Tutor (FE gọi sau khi user quay về từ MoMo/ZaloPay)
    @GetMapping("/wallet/top-up/{id}")
    public ApiResponse<TransactionResponse> getTopUpStatus(@PathVariable UUID id) {
        return ApiResponse.<TransactionResponse>builder()
                .code(PaymentResponseCode.GET_TOP_UP_STATUS.getCode())
                .message(PaymentResponseCode.GET_TOP_UP_STATUS.getMessage())
                .data(paymentService.syncTopUpStatus(id))
                .build();
    }

    // 9. IPN của MoMo (public, xác thực bằng chữ ký). MoMo yêu cầu phản hồi HTTP 204.
    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> momoIpn(@RequestBody String rawBody) {
        paymentService.handleGatewayCallback(PaymentMethod.MOMO, rawBody);
        return ResponseEntity.noContent().build();
    }

    // 10. Callback của ZaloPay (public, xác thực bằng key2). Phải trả JSON return_code theo quy ước của ZaloPay:
    //  1 = thành công, -1 = sai mac (không retry), 0 = lỗi hệ thống (ZaloPay sẽ gọi lại)
    @PostMapping("/zalopay/callback")
    public Map<String, Object> zaloPayCallback(@RequestBody String rawBody) {
        try {
            paymentService.handleGatewayCallback(PaymentMethod.ZALOPAY, rawBody);
            return Map.of("return_code", 1, "return_message", "success");
        } catch (AppException ex) {
            if (ex.getErrorCode() == PaymentErrorCode.INVALID_GATEWAY_SIGNATURE) {
                return Map.of("return_code", -1, "return_message", "mac not equal");
            }
            log.error("ZaloPay callback rejected: {}", ex.getMessage());
            return Map.of("return_code", 0, "return_message", ex.getMessage());
        } catch (Exception ex) {
            log.error("ZaloPay callback failed", ex);
            return Map.of("return_code", 0, "return_message", "internal error");
        }
    }
}
