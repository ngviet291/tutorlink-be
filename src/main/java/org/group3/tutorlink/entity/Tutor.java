package org.group3.tutorlink.entity;

import org.group3.tutorlink.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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

    private double hourlyRate;

    private double averageRating;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    private String education;

    // Tutor n -- 1 Subject (see README note: diagram reads Subject 1 -- 1..* Tutor)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    // Tutor 1 -- 1..* Certificate
    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Certificate> certificates;

    // Tutor 1 -- 0..* Post
    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Post> posts ;

    // Tutor 1 -- 0..* ChatChannel
    @OneToMany(mappedBy = "tutor")
    private Set<ChatChannel> chatChannels ;
}
