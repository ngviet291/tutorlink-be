package org.group3.tutorlink.features.auth.event;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.auth.service.TutorCertificateReviewService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TutorCertificateReviewListener {
    private final TutorCertificateReviewService tutorCertificateReviewService;

    // AFTER_COMMIT: chỉ chạy khi tutor đã lưu thật; @Async: không làm chậm response đăng ký
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TutorCertificatesSubmittedEvent e) {
        tutorCertificateReviewService.review(e.getTutorId());
    }
}