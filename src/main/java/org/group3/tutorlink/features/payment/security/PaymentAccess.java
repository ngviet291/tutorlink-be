package org.group3.tutorlink.features.payment.security;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.exception.PaymentErrorCode;
import org.group3.tutorlink.features.payment.repository.TransactionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Kiểm tra quyền sở hữu dữ liệu; quyền ADMIN do annotation xử lý. */
@Component("paymentAccess")
@RequiredArgsConstructor
public class PaymentAccess {

    private final TransactionRepository transactionRepository;
    private final AppUtil appUtil;

    @Transactional(readOnly = true)
    public boolean isOwner(UUID transactionId) {
        UUID userId = appUtil.userIdFromAuthentication();
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.TRANSACTION_NOT_FOUND));

        if (!userId.equals(transaction.getTutor().getId())) {
            throw new AppException(PaymentErrorCode.NOT_TRANSACTION_OWNER);
        }

        return true;
    }
}
