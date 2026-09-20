package org.group3.tutorlink.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * System-wide matching fee configuration (e.g. 10%/20% tiers).
 * Not tied to a specific Tutor/User in the diagram — treated as global config.
 */
@Entity
@Table(name = "matching_fee_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingFeeConfig extends BaseEntity{

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
