package org.group3.tutorlink.features.payment.service;

import org.group3.tutorlink.features.payment.dto.response.BankResponse;

import java.util.List;

public interface BankService {

    List<BankResponse> getBanks();
}