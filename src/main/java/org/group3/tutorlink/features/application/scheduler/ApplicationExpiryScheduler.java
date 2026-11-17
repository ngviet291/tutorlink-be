package org.group3.tutorlink.features.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.application.service.ApplicationExpiryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Poll Redis delay queue: claim các đơn đã đến hạn rồi giao cho
 * {@link ApplicationExpiryService} xử lý (mỗi đơn một transaction riêng).
 * Scheduler KHÔNG có @Transactional.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationExpiryScheduler {

    private final ApplicationExpiryQueue queue;
    private final ApplicationExpiryService expiryService;

    @Value("${application.expiry.batch-size:100}")
    private int batchSize;

    @Value("${application.expiry.retry-delay-seconds:60}")
    private long retryDelaySeconds;

    @Scheduled(fixedDelayString = "${application.expiry.interval-ms}")
    public void expireApplications() {
        for (ApplicationExpiryQueue.Stage stage : ApplicationExpiryQueue.Stage.values()) {
            for (String raw : queue.claimDue(stage, batchSize)) {
                UUID id;
                try {
                    id = UUID.fromString(raw);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid application id in expiry queue ({}): {}", stage, raw);
                    continue;
                }
                try {
                    expiryService.expire(id, stage);
                } catch (Exception e) {
                    log.error("Expire application {} ({}) failed, retry later", id, stage, e);
                    queue.schedule(stage, id, Instant.now().plusSeconds(retryDelaySeconds));
                }
            }
        }
    }
}
