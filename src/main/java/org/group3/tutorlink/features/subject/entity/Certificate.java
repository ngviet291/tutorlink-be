package org.group3.tutorlink.features.subject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.user.entity.Tutor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "certificates", indexes = {
        @Index(name = "idx_certificates_tutor_subject", columnList = "tutor_id, subject_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate extends BaseEntity {

    @Id
    private UUID id;

    private String name;

    private String issuedBy;

    private LocalDate issuedDate;

    private LocalDate expiryDate;

    private String fileUrl;

    private String cloudinaryPublicId;

    private boolean verified;

    // AI không tự duyệt được -> chờ admin duyệt tay (verified vẫn false cho tới khi admin duyệt)
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean pendingAdminReview;

    // Lý do AI không duyệt được, để admin tham khảo
    @Column(columnDefinition = "TEXT")
    private String reviewNote;
    // Chứng chỉ chứng minh cho môn nào (null với chứng chỉ cũ)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;
    // Nhiều chứng chỉ có thể thuộc về cùng một gia sư.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;
}
