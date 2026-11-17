package org.group3.tutorlink.features.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.notification.enums.NotificationType;
import org.group3.tutorlink.features.notification.service.NotificationCreator;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.service.RefundService;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.user.entity.User;
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

    private static final String NOTI_APPLICATION_EXPIRED_TITLE = "Đơn ứng tuyển đã hết hạn";

    private final ApplicationRepository applicationRepository;
    private final RefundService refundService;
    private final NotificationCreator notificationCreator;

    @Value("${application.expiry.payment-window-minutes:1440}")
    private long paymentWindowMinutes;

    @Value("${application.expiry.confirm-window-minutes:1440}")
    private long confirmWindowMinutes;

    @Scheduled(fixedDelayString = "${application.expiry.interval-ms}")
    @Transactional
    public void expireApplications() {
        Instant paymentCutoff = Instant.now().minus(paymentWindowMinutes, ChronoUnit.MINUTES);
        Instant confirmCutoff = Instant.now().minus(confirmWindowMinutes, ChronoUnit.MINUTES);

        List<Application> expiredSelections = applicationRepository.findExpiredSelections(
                ApplicationStatus.ACCEPTED,
                paymentCutoff,
                TransactionStatus.SUCCESS
        );
        for (Application application : expiredSelections) {
            expireApplication(application, false);
        }

        List<Application> expiredConfirmations = applicationRepository.findExpiredConfirmations(
                ApplicationStatus.ACCEPTED,
                TransactionStatus.SUCCESS,
                confirmCutoff
        );
        for (Application application : expiredConfirmations) {
            refundService.refundCommission(application);
            expireApplication(application, true);
        }
    }

    private void expireApplication(Application application, boolean refunded) {
        Post post = application.getPost();
        application.setApplicationStatus(ApplicationStatus.CANCELLED);
        application.setSelectedAt(null);

        // Chỉ mở lại bài khi bài đang PENDING (đang chờ thanh toán/xác nhận).
        // Post CLOSED/REMOVED không được hồi sinh về PUBLISHED.
        if (post != null && post.getStatus() == PostStatus.PENDING) {
            post.setStatus(PostStatus.PUBLISHED);
            restoreRejectedApplications(post);
        }

        applicationRepository.save(application);
        log.info("Application {} expired (refunded={})", application.getId(), refunded);

        String content = refunded
                ? String.format("Đơn ứng tuyển của bạn vào bài \"%s\" đã hết hạn xác nhận. Phí hoa hồng đã được hoàn lại.",
                        post != null ? post.getTitle() : "")
                : String.format("Đơn ứng tuyển vào bài \"%s\" đã hết hạn thanh toán.",
                        post != null ? post.getTitle() : "");
        notifyUser(applicantOf(application), content);
        if (post != null && post.getAuthor() != null) {
            notifyUser(post.getAuthor(), content);
        }
    }

    /** Người apply: FIND_TUTOR → tutor, FIND_STUDENT → student. */
    private User applicantOf(Application application) {
        Post post = application.getPost();
        if (post != null && post.getType() == PostType.FIND_TUTOR) {
            return application.getTutor();
        }
        return application.getStudent();
    }

    private void notifyUser(User recipient, String content) {
        if (recipient != null) {
            notificationCreator.createNotification(
                    recipient.getId(), NotificationType.APPLICATION,
                    NOTI_APPLICATION_EXPIRED_TITLE, content);
        }
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
