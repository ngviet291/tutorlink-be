package org.group3.tutorlink.features.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.user.entity.Tutor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    // Wallet 1 -- 1 Tutor
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false, unique = true)
    private Tutor tutor;
}
