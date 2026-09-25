package org.group3.tutorlink.features.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.user.entity.User;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {
    @Id
    private UUID id;
    @Column(length = 20)
    private String name;
    private String description;

    // Role 1 -- n User
    @OneToMany(mappedBy = "role")
    private Set<User> users  ;
}
