package org.group3.tutorlink.features.payment.service;

import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.payment.dto.request.CreateFeeConfigRequest;
import org.group3.tutorlink.features.payment.dto.request.TopUpRequest;
import org.group3.tutorlink.features.payment.dto.response.FeeConfigResponse;
import org.group3.tutorlink.features.payment.dto.response.RevenueStatisticsResponse;
import org.group3.tutorlink.features.payment.dto.response.TransactionResponse;
import org.group3.tutorlink.features.payment.dto.response.WalletBalanceResponse;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;

import java.time.LocalDate;
import java.util.UUID;

public interface PaymentService {

    WalletBalanceResponse getWalletBalance();

    TransactionResponse topUp(TopUpRequest request);

    TransactionResponse payCommission(UUID applicationId);

    CursorResponse<TransactionResponse> getMyTransactions(UUID cursor, int limit);

    CursorResponse<TransactionResponse> getAllTransactions(TransactionStatus status, UUID cursor, int limit);

    TransactionResponse getTransactionDetail(UUID transactionId);

    TransactionResponse refundTransaction(UUID transactionId);

    RevenueStatisticsResponse getRevenueStatistics(LocalDate from, LocalDate to);

    CursorResponse<FeeConfigResponse> getFeeConfigs(UUID cursor, int limit);

    FeeConfigResponse createFeeConfig(CreateFeeConfigRequest request);

    // IPN/callback từ MoMo, ZaloPay (server-to-server, không có JWT, xác thực bằng chữ ký)
    void handleGatewayCallback(PaymentMethod method, String rawBody);

    // Hỏi lại cổng về trạng thái giao dịch nạp tiền đang PENDING (Tutor sở hữu giao dịch)
    TransactionResponse syncTopUpStatus(UUID transactionId);
}
