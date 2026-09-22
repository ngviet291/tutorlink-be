package org.group3.tutorlink.features.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.entity.Tutor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "chat_channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatChannel extends BaseEntity {

    @Id
    private UUID id;

    private String name;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastMessageAt;

    // ChatChannel n -- 1 Tutor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    // ChatChannel n -- 1 Student
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // ChatChannel 1 -- 0..* Message
    @OneToMany(mappedBy = "chatChannel", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Message> messages ;
}
