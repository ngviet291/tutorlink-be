package org.group3.tutorlink.features.auth.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.auth.service.TutorCertificateReviewService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Định kỳ chuyển các chứng chỉ bị kẹt (AI không xử lý xong) sang admin. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateReviewSweeper {

    private final TutorCertificateReviewService reviewService;

    @Scheduled(fixedDelayString = "${app.verification.sweep-interval-ms:300000}")
    public void sweep() {
        try {
            reviewService.sweepStuck();
        } catch (Exception e) {
            log.error("Certificate sweep failed", e);
        }
    }
}
