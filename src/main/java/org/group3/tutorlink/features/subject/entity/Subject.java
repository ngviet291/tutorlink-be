package org.group3.tutorlink.features.subject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.user.entity.Tutor;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String category;

    // Subject 1 -- 1..* Tutor
    @OneToMany(mappedBy = "subject")
    private Set<Tutor> tutors ;
}
