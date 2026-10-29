package org.group3.tutorlink.features.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.group3.tutorlink.features.chat.entity.ChatChannel;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.user.enums.VerificationStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tutors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Tutor extends User {

    @Column(columnDefinition = "TEXT")
    private String bio;

    private int experienceYears;

    private BigDecimal hourlyRate;

    private double averageRating;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;
    @Column(columnDefinition = "TEXT")
    private String verificationNote;

    private String education;

    @ElementCollection(targetClass = EducationLevel.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "tutor_teaching_levels", joinColumns = @JoinColumn(name = "tutor_id"))
    @Column(name = "teaching_level")
    private Set<EducationLevel> teachingLevels;

    @Enumerated(EnumType.STRING)
    private TeachingMode teachingMode;
    @ElementCollection
    @CollectionTable(name = "tutor_available_slots", joinColumns = @JoinColumn(name = "tutor_id"))
    private List<AvailableSlot> availableSlots;

    @ElementCollection
    @CollectionTable(name = "tutor_teaching_areas", joinColumns = @JoinColumn(name = "tutor_id"))
    private List<String> teachingAreas;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tutor_subjects",
            joinColumns = @JoinColumn(name = "tutor_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private Set<Subject> subjects;



    // Tutor 1 -- 1..* Certificate
    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Certificate> certificates;


}
