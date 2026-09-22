package org.group3.tutorlink.features.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.user.entity.Admin;

import java.time.LocalDate;
import java.util.UUID;


@Entity
@Table(name = "matching_fee_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingFeeConfig extends BaseEntity {

    @Id
    private UUID id;

    private double feePercentage;

    private double fixedFee;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin createdByAdmin;
}
