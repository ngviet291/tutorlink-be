package org.group3.tutorlink.features.application.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.user.entity.Admin;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application extends BaseEntity {

    @Id
    private UUID id;

    /**
     * Trạng thái Application:
     *
     * PENDING    : đã gửi, chờ chủ Post chọn
     * ACCEPTED   : chủ Post đã chọn
     * COMPLETED  : người được chọn đã xác nhận
     * REJECTED   : bị từ chối
     * CANCELLED  : bị hủy
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus applicationStatus;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    /**
     * Application n -- 1 Post
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /**
     * Student ứng tuyển.
     *
     * Với FIND_STUDENT:
     * Student là applicant.
     *
     * Với FIND_TUTOR:
     * Student sẽ được set khi Student chọn Tutor.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    /**
     * Tutor ứng tuyển.
     *
     * Với FIND_TUTOR:
     * Tutor là applicant.
     *
     * Với FIND_STUDENT:
     * Tutor sẽ được set khi Tutor chọn Student.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;

    /**
     * Admin xử lý Application nếu cần lưu
     * người đã xem xét Application.
     *
     * Việc Admin xem xét KHÔNG làm thay đổi
     * applicationStatus.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_admin_id")
    private Admin processedByAdmin;

    /**
     * Application 1 -- 0..1 Transaction
     */
    @OneToOne(
            mappedBy = "application",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Transaction transaction;
}