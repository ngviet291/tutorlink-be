package org.group3.tutorlink.features.payment.service;

import org.group3.tutorlink.features.application.entity.Application;

/**
 * Hoàn tiền hoa hồng cho tutor khi một application đã thanh toán
 * nhưng bị hủy/expire (ví dụ: applicant không confirm trong 24h).
 *
 * TODO: thay bằng implementation thật (cộng lại ví + set transaction REFUNDED)
 * khi tích hợp refund thật; hiện tại dùng {@link org.group3.tutorlink.features.payment.service.impl.MockRefundServiceImpl}.
 */
public interface RefundService {

    /**
     * Hoàn tiền commission của application (nếu có transaction SUCCESS).
     * Idempotent: gọi nhiều lần trên cùng application không được hoàn trùng.
     */
    void refundCommission(Application application);
}
