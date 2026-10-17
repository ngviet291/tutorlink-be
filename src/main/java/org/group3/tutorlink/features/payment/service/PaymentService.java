package org.group3.tutorlink.features.payment.service;

import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.payment.dto.request.TopUpRequest;
import org.group3.tutorlink.features.payment.dto.response.RevenueStatisticsResponse;
import org.group3.tutorlink.features.payment.dto.response.TransactionResponse;
import org.group3.tutorlink.features.payment.dto.response.WalletBalanceResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface PaymentService {

    WalletBalanceResponse getWalletBalance();

    TransactionResponse topUp(TopUpRequest request);

    TransactionResponse payCommission(UUID applicationId);

    CursorResponse<TransactionResponse> getMyTransactions(UUID cursor, int limit);

    CursorResponse<TransactionResponse> getAllTransactions(String status, UUID cursor, int limit);

    TransactionResponse refundTransaction(UUID transactionId);

    RevenueStatisticsResponse getRevenueStatistics(LocalDate from, LocalDate to);
}
