package org.group3.tutorlink.features.payment.entity;

import org.group3.tutorlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;

import java.time.LocalDate;
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

    private double amount;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private LocalDate transactionDate;

    // Transaction 0..1 -- 1 Application
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;
}
