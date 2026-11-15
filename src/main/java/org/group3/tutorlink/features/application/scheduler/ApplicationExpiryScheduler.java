package org.group3.tutorlink.features.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.service.RefundService;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationExpiryScheduler {

    private final ApplicationRepository applicationRepository;
    private final RefundService refundService;

    @Value("${application.expiry.payment-window-minutes:2}")
    private long paymentWindowMinutes;

    @Value("${application.expiry.confirm-window-minutes:1440}")
    private long confirmWindowMinutes;

    @Scheduled(fixedDelayString = "${application.expiry.interval-ms:3600000}")
    @Transactional
    public void expireApplications() {
        Instant paymentCutoff = Instant.now().minus(paymentWindowMinutes, ChronoUnit.MINUTES);
        Instant confirmCutoff = Instant.now().minus(confirmWindowMinutes, ChronoUnit.MINUTES);

        List<Application> expiredSelections = applicationRepository.findExpiredSelections(
                ApplicationStatus.ACCEPTED,
                PostStatus.PENDING,
                paymentCutoff,
                TransactionStatus.SUCCESS
        );
        for (Application application : expiredSelections) {
            expireApplication(application);
        }

        List<Application> expiredConfirmations = applicationRepository.findExpiredConfirmations(
                ApplicationStatus.ACCEPTED,
                TransactionStatus.SUCCESS,
                confirmCutoff
        );
        for (Application application : expiredConfirmations) {
            refundService.refundCommission(application);
            expireApplication(application);
        }
    }

    private void expireApplication(Application application) {
        Post post = application.getPost();
        application.setApplicationStatus(ApplicationStatus.CANCELLED);
        application.setSelectedAt(null);

        if (post != null) {
            post.setStatus(PostStatus.PUBLISHED);
            restoreRejectedApplications(post);
        }

        applicationRepository.save(application);
        log.info("Application {} expired and post reverted to PUBLISHED", application.getId());
    }

    private void restoreRejectedApplications(Post post) {
        Page<Application> rejectedApplications = applicationRepository
                .findByPostIdAndApplicationStatus(
                        post.getId(),
                        ApplicationStatus.REJECTED,
                        Pageable.unpaged()
                );
        for (Application rejected : rejectedApplications.getContent()) {
            rejected.setApplicationStatus(ApplicationStatus.PENDING);
        }
    }
}
