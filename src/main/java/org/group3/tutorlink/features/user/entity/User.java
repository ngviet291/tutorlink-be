package org.group3.tutorlink.features.user.entity;

import org.group3.tutorlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.group3.tutorlink.features.auth.entity.Role;
import org.group3.tutorlink.features.chat.entity.Media;
import org.group3.tutorlink.features.notification.entity.Notification;
import org.group3.tutorlink.features.user.enums.Gender;
import org.group3.tutorlink.features.user.enums.UserStatus;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;


@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class User extends BaseEntity {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String fullname;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;
    private String phone;
    private String avatarUrl;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    private LocalDate dateOfBirth;
    private String address;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus;

    // User n -- 1 Role
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    // User 1 -- 0..* Notification (receives)
    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Notification> notifications;
    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL,orphanRemoval = true)
    private Set<Media> media;

}
