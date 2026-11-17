package org.group3.tutorlink.features.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Nạp một lần các đơn ACCEPTED đang tồn tại vào Redis expiry queue.
 * Chạy với application.expiry.backfill=true rồi tắt.
 * Idempotent: ZADD ghi đè score nên chạy lại không hại.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.expiry.backfill", havingValue = "true")
public class ExpiryBackfillRunner implements ApplicationRunner {

    private final ApplicationRepository applicationRepository;
    private final ApplicationExpiryQueue queue;

    @Value("${application.expiry.payment-window-minutes:1440}")
    private long paymentWindowMinutes;

    @Value("${application.expiry.confirm-window-minutes:1440}")
    private long confirmWindowMinutes;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        int scheduled = 0;
        for (Application a : applicationRepository.findAllByApplicationStatus(ApplicationStatus.ACCEPTED)) {
            try {
                Transaction transaction = a.getTransaction();
                boolean paid = transaction != null && transaction.getStatus() == TransactionStatus.SUCCESS;
                if (paid) {
                    // Ưu tiên mốc paidAt thật; fallback now + window để đơn cũ có thêm thời gian
                    Instant base = transaction.getTransactionDate() != null
                            ? transaction.getTransactionDate()
                            : Instant.now();
                    queue.schedule(ApplicationExpiryQueue.Stage.CONFIRM, a.getId(),
                            base.plus(confirmWindowMinutes, ChronoUnit.MINUTES));
                    scheduled++;
                } else if (a.getSelectedAt() != null) {
                    queue.schedule(ApplicationExpiryQueue.Stage.PAYMENT, a.getId(),
                            a.getSelectedAt().plus(paymentWindowMinutes, ChronoUnit.MINUTES));
                    scheduled++;
                }
            } catch (Exception e) {
                log.error("Backfill expiry failed for application {}", a.getId(), e);
            }
        }
        log.info("Expiry backfill done: {} applications scheduled", scheduled);
    }
}
