package org.group3.tutorlink.features.payment.service;

import org.group3.tutorlink.features.payment.dto.response.WalletBalanceResponse;

public interface PaymentService {

    WalletBalanceResponse getWalletBalance();
}
