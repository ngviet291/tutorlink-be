package org.group3.tutorlink.features.payment.service.impl;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.payment.dto.response.WalletBalanceResponse;
import org.group3.tutorlink.features.payment.entity.Wallet;
import org.group3.tutorlink.features.payment.exception.PaymentTutorNotFoundException;
import org.group3.tutorlink.features.payment.mapper.WalletMapper;
import org.group3.tutorlink.features.payment.repository.WalletRepository;
import org.group3.tutorlink.features.payment.service.PaymentService;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final WalletRepository walletRepository;
    private final TutorRepository tutorRepository;
    private final WalletMapper walletMapper;
    private final AppUtil appUtil;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('TUTOR')")
    public WalletBalanceResponse getWalletBalance() {
        UUID tutorId = appUtil.userIdFromAuthentication();

        Wallet wallet = walletRepository.findByTutorId(tutorId)
                .orElseGet(() -> createWallet(tutorId));

        return walletMapper.toWalletBalanceResponse(wallet);
    }

    private Wallet createWallet(UUID tutorId) {
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(PaymentTutorNotFoundException::new);

        Wallet wallet = Wallet.builder()
                .id(appUtil.generateUUID())
                .balance(BigDecimal.ZERO)
                .tutor(tutor)
                .build();

        return walletRepository.save(wallet);
    }
}
