package org.group3.tutorlink.features.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.enums.TransactionType;
import org.group3.tutorlink.features.user.entity.Tutor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Id
    private UUID id;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    // TOP_UP: nạp tiền vào ví, COMMISSION: trả phí hoa hồng cho 1 application
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private Instant transactionDate;

    // Mã đơn hàng phía cổng thanh toán (MoMo orderId / ZaloPay app_trans_id), chỉ có khi nạp tiền qua cổng
    @Column(unique = true)
    private String providerOrderId;

    // Mã giao dịch do cổng thanh toán trả về khi thanh toán xong (MoMo transId / ZaloPay zp_trans_id)
    private String providerTransId;

    // Transaction n -- 1 Tutor (chủ ví)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    // Transaction 0..1 -- 1 Application (chỉ có khi type = COMMISSION)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", unique = true)
    private Application application;
}
