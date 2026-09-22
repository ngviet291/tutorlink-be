package org.group3.tutorlink.features.subject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.user.entity.Tutor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String issuedBy;

    private LocalDate issuedDate;

    private LocalDate expiryDate;

    private String fileUrl;

    private boolean verified;

    // Certificate n -- 1 Tutor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;
}
