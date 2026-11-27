/*
 * @ (#) ApplicationResponse,java       1.0    29/09/2026
 *
 * Copyright (c) 2026 IUH. All righta reserved.
 */

package org.group3.tutorlink.features.application.dto.response;

/*
 * @description:
 * @author: Ho Thi Kim Xuyen
 * @version:     1.0
 * @date: 29/09/2026 14:56
 */

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {

    private UUID id;

    private UUID postId;

    private UserSummaryResponse student;

    private UserSummaryResponse tutor;

    private String applicationStatus;

    private String message;

    private Instant appliedAt;

    // Thời điểm chủ bài chọn đơn này (ACCEPTED); null nếu chưa select hoặc đã revert
    private Instant selectedAt;
    // Hạn cuối của bước đang chờ, để FE đếm ngược:
    // - ACCEPTED + đã trả phí (transaction SUCCESS) -> transactionDate + confirm-window (W2, chờ applicant confirm)
    // - ACCEPTED + chưa trả -> selectedAt + payment-window (W1, chờ tutor thanh toán)
    // - Trạng thái khác -> null
    private Instant expiresAt;
    private BigDecimal feeAmount;

    private UUID processedByAdminId;

    private UUID transactionId;

    private String transactionStatus;
    // Thoi diem thanh toan thanh cong (transaction SUCCESS); null neu chua tra
    private Instant paidAt;

    private Boolean paid;

    private BigDecimal proposedFee;
    private Instant finishRequestedAt;
    private UUID finishRequestedBy;
}
