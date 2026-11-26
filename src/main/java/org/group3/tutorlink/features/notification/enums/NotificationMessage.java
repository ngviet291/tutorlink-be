package org.group3.tutorlink.features.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum NotificationMessage {

    // 2.1 Chọn ứng viên
    APPLICATION_SELECTED_TUTOR(NotificationType.APPLICATION),
    APPLICATION_SELECTED_STUDENT(NotificationType.APPLICATION),
    APPLICATION_PAY_FEE(NotificationType.APPLICATION),

    // 2.2 Đơn hết hạn
    APPLICATION_EXPIRED_PAYER_REFUNDED(NotificationType.APPLICATION),
    APPLICATION_EXPIRED_PAYER_UNPAID(NotificationType.APPLICATION),
    APPLICATION_EXPIRED_STUDENT(NotificationType.APPLICATION),
    APPLICATION_REOPENED(NotificationType.APPLICATION),

    // 2.3 Ví, thanh toán
    TOP_UP_SUCCESS(NotificationType.TRANSACTION),
    TOP_UP_FAILED(NotificationType.TRANSACTION),
    TOP_UP_MISMATCH(NotificationType.TRANSACTION),
    TOP_UP_MISMATCH_ADMIN(NotificationType.TRANSACTION),
    BALANCE_LOW(NotificationType.TRANSACTION),
    FEE_PAID_TUTOR(NotificationType.TRANSACTION),
    FEE_PAID_STUDENT(NotificationType.APPLICATION),
    FEE_REFUNDED(NotificationType.TRANSACTION),

    // 2.4 Đánh giá, khiếu nại
    REVIEW_RECEIVED(NotificationType.REVIEW),
    COMPLAINT_CREATED_ADMIN(NotificationType.REVIEW),
    COMPLAINT_APPROVED(NotificationType.REVIEW),
    COMPLAINT_REJECTED(NotificationType.REVIEW),

    // 2.5 Đóng, gỡ bài
    POST_CLOSED_APPLICANT(NotificationType.POST),
    POST_REMOVED_BY_AUTHOR(NotificationType.POST),
    POST_REMOVED_BY_ADMIN(NotificationType.POST),
    POST_REMOVED_BY_ADMIN_AUTHOR(NotificationType.SYSTEM),

    // 2.6 Duyệt hồ sơ gia sư
    TUTOR_APPROVED(NotificationType.SYSTEM),
    TUTOR_REJECTED(NotificationType.SYSTEM),
    TUTOR_REJECTED_NO_REASON(NotificationType.SYSTEM),
    TUTOR_REVIEW_FAILED_ADMIN(NotificationType.SYSTEM),
    TUTOR_RESUBMITTED_ADMIN(NotificationType.SYSTEM),

    // 2.7 Tài khoản
    PASSWORD_CHANGED(NotificationType.SYSTEM),
    PASSWORD_RESET(NotificationType.SYSTEM),
    ACCOUNT_UNLOCKED(NotificationType.SYSTEM),

    // 2.8 Đơn ứng tuyển (ApplicationServiceImpl)
    APPLICATION_CREATED(NotificationType.APPLICATION),
    APPLICATION_REJECTED_OTHER_CHOSEN(NotificationType.APPLICATION),
    APPLICATION_REJECTED(NotificationType.APPLICATION),
    APPLICATION_UNSELECTED(NotificationType.APPLICATION),
    APPLICATION_CONFIRMED(NotificationType.APPLICATION),
    APPLICATION_FINISHED(NotificationType.APPLICATION),
    APPLICATION_CANCELLED(NotificationType.APPLICATION),
    APPLICATION_CONFIRM_REQUIRED(NotificationType.APPLICATION),

    // 2.9 Duyệt chứng chỉ bằng AI (TutorCertificateReviewService)
    TUTOR_APPROVED_BY_AI(NotificationType.SYSTEM),
    TUTOR_REJECTED_BY_AI(NotificationType.SYSTEM),
    CERTIFICATE_VERIFIED(NotificationType.SYSTEM),
    CERTIFICATE_INVALID(NotificationType.SYSTEM),
    CERTIFICATE_REVIEW_FAILED(NotificationType.SYSTEM),

    // 2.10 Chứng chỉ AI không duyệt được, chuyển admin duyệt tay
    CERTIFICATE_SENT_TO_ADMIN(NotificationType.SYSTEM),
    CERTIFICATE_REVIEW_NEEDED_ADMIN(NotificationType.SYSTEM),
    // 2.11 Chat
    NEW_MESSAGE(NotificationType.SYSTEM),
    REVIEW_UPDATED(NotificationType.SYSTEM)

    ;
    private final NotificationType type;

    /** Khóa trong messages.properties: TOP_UP_SUCCESS -> top-up-success */
    public String getKey() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}