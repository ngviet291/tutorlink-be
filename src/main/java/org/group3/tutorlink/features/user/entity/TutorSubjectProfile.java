package org.group3.tutorlink.features.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.subject.entity.Subject;

import java.math.BigDecimal;
import java.util.UUID;


@Entity
@Table(name = "tutor_subject_profiles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tutor_id", "subject_id"}))
@Getter
@Setter
@NoArgsConstructor
public class TutorSubjectProfile {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    private int experienceYears;

    private BigDecimal hourlyRate;

    @Enumerated(EnumType.STRING)
    private TeachingMode teachingMode;
}
