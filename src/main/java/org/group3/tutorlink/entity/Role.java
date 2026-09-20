package org.group3.tutorlink.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity{

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
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
