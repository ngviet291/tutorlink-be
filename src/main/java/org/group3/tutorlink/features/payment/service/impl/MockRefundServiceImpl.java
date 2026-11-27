package org.group3.tutorlink.features.payment.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.entity.Wallet;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.enums.TransactionType;
import org.group3.tutorlink.features.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.payment.repository.TransactionRepository;
import org.group3.tutorlink.features.payment.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockRefundServiceImpl implements RefundService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public void refundCommission(Application application) {
        Transaction transaction = application.getTransaction();
        if (transaction == null
                || transaction.getType() != TransactionType.COMMISSION
                || transaction.getStatus() != TransactionStatus.SUCCESS
                || transaction.getTutor() == null) {
            return;
        }

        Wallet wallet = walletRepository.findForUpdateByTutorId(transaction.getTutor().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Wallet not found for tutor " + transaction.getTutor().getId()));
        wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        walletRepository.save(wallet);

        transaction.setStatus(TransactionStatus.REFUNDED);
        transactionRepository.save(transaction);
        log.info("Refunded commission {} for application {} (tutor={})",
                transaction.getAmount(), application.getId(), transaction.getTutor().getId());
    }
}