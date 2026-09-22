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
    // Role n -- n Permission
    @ManyToMany
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions ;
    // Role 1 -- n User
    @OneToMany(mappedBy = "role")
    private Set<User> users  ;
}
