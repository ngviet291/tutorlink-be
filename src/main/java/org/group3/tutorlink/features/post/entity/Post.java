package org.group3.tutorlink.features.post.entity;


import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.user.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    // Student tìm Tutor hoặc Tutor tìm Student
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status;

    // Người tạo bài: Student hoặc Tutor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // Môn học
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    // Cấp học
    @Enumerated(EnumType.STRING)
    private EducationLevel educationLevel;

    // Online / Offline / Both
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeachingMode teachingMode;

    // Ví dụ: Quận 7, TP.HCM
    @Embedded
    private Address address;

    // Mức giá
    private BigDecimal minBudget;

    private BigDecimal maxBudget;

    // Số buổi / tuần
    private Integer sessionsPerWeek;

    // Thời lượng mỗi buổi, đơn vị phút
    private Integer durationMinutes;

    // Hạn nhận ứng tuyển
    private Instant deadline;

    @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<Application> applications;
}