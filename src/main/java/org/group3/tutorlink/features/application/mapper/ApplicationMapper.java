package org.group3.tutorlink.features.application.mapper;

import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring")
public abstract class ApplicationMapper {

    // Cửa sổ chờ thanh toán (W1) và chờ applicant confirm sau khi đã trả (W2).
    // Phải khớp ApplicationExpiryScheduler — đơn quá hạn sẽ bị scheduler CANCELLED.
    @Value("${application.expiry.payment-window-minutes:1440}")
    private long paymentWindowMinutes;

    @Value("${application.expiry.confirm-window-minutes:1440}")
    private long confirmWindowMinutes;

    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "processedByAdminId", source = "processedByAdmin.id")
    @Mapping(target = "transactionId", source = "transaction.id")
    @Mapping(target = "transactionStatus", expression = "java(application.getTransaction() != null ? application.getTransaction().getStatus().name() : null)")
    public abstract ApplicationResponse toApplicationResponse(Application application);

    /**
     * Tính expiresAt = hạn cuối của bước đang chờ, để FE hiển thị countdown.
     * Chỉ đơn ACCEPTED mới có hạn:
     * - Đã trả phí (transaction SUCCESS) → transactionDate + confirm-window (W2, applicant confirm)
     * - Chưa trả → selectedAt + payment-window (W1, tutor thanh toán)
     */
    @AfterMapping
    protected void fillExpiresAt(Application application, @MappingTarget ApplicationResponse response) {
        if (application.getApplicationStatus() != ApplicationStatus.ACCEPTED) {
            return;
        }
        Transaction transaction = application.getTransaction();
        Instant paidAt = transaction != null && transaction.getStatus() == TransactionStatus.SUCCESS
                ? transaction.getTransactionDate()
                : null;
        if (paidAt != null) {
            response.setExpiresAt(paidAt.plus(confirmWindowMinutes, ChronoUnit.MINUTES));
        } else if (application.getSelectedAt() != null) {
            response.setExpiresAt(application.getSelectedAt().plus(paymentWindowMinutes, ChronoUnit.MINUTES));
        }
    }
}