package org.group3.tutorlink.features.application.entity;

import org.group3.tutorlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.payment.entity.Transaction;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.user.entity.Admin;
import org.group3.tutorlink.features.user.entity.Student;

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

    @Enumerated(EnumType.STRING)
    private ApplicationStatus applicationStatus;

    @Column(columnDefinition = "TEXT")
    private String message;
    //Sửa Instant
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    // Application n -- 1 Post
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // Application n -- 1 Student (applicant)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Application n -- 1 Admin (accepts / processes), optional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_admin_id")
    private Admin processedByAdmin;

    // Application 1 -- 0..1 Transaction
    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private Transaction transaction;
}
