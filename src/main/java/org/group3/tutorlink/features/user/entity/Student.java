package org.group3.tutorlink.features.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.chat.entity.ChatChannel;
import org.group3.tutorlink.features.review.entity.Review;

import java.util.Set;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Student extends User {

    private String grade;

    private String school;

    @Column(columnDefinition = "TEXT")
    private String learningGoal;

    // Student 1 -- 0..* Review
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews ;

    // Student 1 -- 0..* Application
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Application> applications ;

    // Student 1 -- 0..* ChatChannel
    @OneToMany(mappedBy = "student")
    private Set<ChatChannel> chatChannels;
}
