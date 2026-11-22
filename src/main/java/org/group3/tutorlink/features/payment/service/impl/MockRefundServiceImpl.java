package org.group3.tutorlink.features.payment.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.enums.TransactionType;
import org.group3.tutorlink.features.payment.service.RefundService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mock refund: chỉ log, KHÔNG cộng lại ví, KHÔNG đổi trạng thái transaction.
 * Thay bằng implementation thật khi refund flow được chốt.
 */
@Slf4j
@Service
public class MockRefundServiceImpl implements RefundService {

    @Override
    @Transactional
    public void refundCommission(Application application) {
        Transaction transaction = application.getTransaction();
        if (transaction == null
                || transaction.getType() != TransactionType.COMMISSION
                || transaction.getStatus() != TransactionStatus.SUCCESS) {
            return;
        }

        // TODO: refund thật = wallet.balance += transaction.amount
        //       + transaction.status = REFUNDED + lưu DB
        log.info("[MOCK REFUND] Would refund commission {} for application {} (tutor={})",
                transaction.getAmount(),
                application.getId(),
                transaction.getTutor() != null ? transaction.getTutor().getId() : null);
    }
}
