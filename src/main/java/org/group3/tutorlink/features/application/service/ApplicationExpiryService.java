package org.group3.tutorlink.features.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.application.scheduler.ApplicationExpiryQueue;
import org.group3.tutorlink.features.application.util.ApplicationParties;
import org.group3.tutorlink.features.notification.enums.NotificationMessage;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.notification.service.NotificationHelper;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.service.RefundService;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationExpiryService {
    private final NotificationHelper notificationHelper;

    private final ApplicationRepository applicationRepository;
    private final RefundService refundService;
    private final ApplicationRestoreService applicationRestoreService;
    @Value("${application.expiry.payment-window-minutes:1440}")
    private long paymentWindowMinutes;

    @Value("${application.expiry.confirm-window-minutes:1440}")
    private long confirmWindowMinutes;
    @Value("${application.expiry.finish-window-minutes:1440}")
    private long finishWindowMinutes;

    @Transactional
    public void expire(UUID applicationId, ApplicationExpiryQueue.Stage stage) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            return;
        }
        if (stage == ApplicationExpiryQueue.Stage.FINISH) {
            autoFinish(application);
            return;
        }
        if (application.getApplicationStatus() != ApplicationStatus.ACCEPTED) {
            return;
        }

        Transaction transaction = application.getTransaction();
        boolean paid = transaction != null && transaction.getStatus() == TransactionStatus.SUCCESS;
        Instant now = Instant.now();

        if (stage == ApplicationExpiryQueue.Stage.PAYMENT) {
            if (paid) {
                return; // đã thanh toán => không hủy ở stage này
            }
            // Kiểm tra deadline thật: selectedAt có thể đã bị đổi (chọn lại) với hạn mới còn ở tương lai
            if (application.getSelectedAt() == null
                    || application.getSelectedAt().plus(paymentWindowMinutes, ChronoUnit.MINUTES).isAfter(now)) {
                return;
            }
            expireApplication(application, false);
        } else {
            if (!paid || transaction.getTransactionDate() == null) {
                return;
            }
            if (transaction.getTransactionDate().plus(confirmWindowMinutes, ChronoUnit.MINUTES).isAfter(now)) {
                return;
            }
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
            applicationRestoreService.restoreRejectedApplications(post);
        }

        applicationRepository.save(application);
        log.info("Application {} expired (refunded={})", application.getId(), refunded);

        String postTitle = post != null ? post.getTitle() : "";
        NotificationTarget target = post != null ? NotificationTarget.POST : NotificationTarget.NONE;
        UUID targetId = post != null ? post.getId() : null;

        // Người trả phí (gia sư) và bên còn lại (học viên) cần câu khác nhau
        NotificationMessage payerMessage = refunded
                ? NotificationMessage.APPLICATION_EXPIRED_PAYER_REFUNDED
                : NotificationMessage.APPLICATION_EXPIRED_PAYER_UNPAID;
        notificationHelper.notify(ApplicationParties.payerTutorOf(application), payerMessage,
                target, targetId, postTitle);
        notificationHelper.notify(application.getStudent(), NotificationMessage.APPLICATION_EXPIRED_STUDENT,
                target, targetId, postTitle);
    }
    // Quá 24h bên kia không phản hồi -> tự động hoàn thành
    private void autoFinish(Application application) {
        if (application.getApplicationStatus() != ApplicationStatus.FINISH_REQUESTED
                || application.getFinishRequestedAt() == null
                || application.getFinishRequestedAt()
                .plus(finishWindowMinutes, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            return;
        }
        application.setApplicationStatus(ApplicationStatus.FINISHED);
        applicationRepository.save(application);
        log.info("Application {} auto-finished", application.getId());

        String title = application.getPost() != null ? application.getPost().getTitle() : "";
        notificationHelper.notify(application.getStudent(), NotificationMessage.APPLICATION_AUTO_FINISHED,
                NotificationTarget.MY_APPLICATIONS, application.getId(), title);
        notificationHelper.notify(application.getTutor(), NotificationMessage.APPLICATION_AUTO_FINISHED,
                NotificationTarget.MY_APPLICATIONS, application.getId(), title);
    }
}