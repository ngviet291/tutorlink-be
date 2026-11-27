package org.group3.tutorlink.features.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.application.service.ApplicationExpiryService;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Lưới an toàn: quét DB định kỳ (rất thưa) để vớt các đơn bị mất khỏi
 * Redis delay queue (flush, mất AOF, process chết sau khi claim).
 * Dùng chung {@link ApplicationExpiryService#expire} — tự kiểm tra lại
 * trạng thái DB nên chạy trùng với Redis scheduler vẫn an toàn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.expiry.reconcile.enabled", havingValue = "true")
public class ApplicationExpiryReconciler {

    private final ApplicationRepository applicationRepository;
    private final ApplicationExpiryService expiryService;

    @Value("${application.expiry.payment-window-minutes:1440}")
    private long paymentWindowMinutes;

    @Value("${application.expiry.confirm-window-minutes:1440}")
    private long confirmWindowMinutes;
    @Value("${application.expiry.finish-window-minutes:1440}")
    private long finishWindowMinutes;
    @Scheduled(fixedDelayString = "${application.expiry.reconcile.interval-ms}")
    public void reconcile() {
        Instant paymentCutoff = Instant.now().minus(paymentWindowMinutes, ChronoUnit.MINUTES);
        Instant confirmCutoff = Instant.now().minus(confirmWindowMinutes, ChronoUnit.MINUTES);

        applicationRepository.findExpiredSelections(
                        ApplicationStatus.ACCEPTED, paymentCutoff, TransactionStatus.SUCCESS)
                .forEach(a -> safe(a.getId(), ApplicationExpiryQueue.Stage.PAYMENT));
        applicationRepository.findExpiredConfirmations(
                        ApplicationStatus.ACCEPTED, TransactionStatus.SUCCESS, confirmCutoff)
                .forEach(a -> safe(a.getId(), ApplicationExpiryQueue.Stage.CONFIRM));

        Instant finishCutoff = Instant.now().minus(finishWindowMinutes, ChronoUnit.MINUTES);
        applicationRepository.findExpiredFinishRequests(ApplicationStatus.FINISH_REQUESTED, finishCutoff)
                .forEach(a -> safe(a.getId(), ApplicationExpiryQueue.Stage.FINISH));
    }

    private void safe(UUID id, ApplicationExpiryQueue.Stage stage) {
        try {
            expiryService.expire(id, stage);
        } catch (Exception e) {
            log.error("Reconcile expire application {} ({}) failed", id, stage, e);
        }
    }
}
