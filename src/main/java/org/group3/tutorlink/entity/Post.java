package org.group3.tutorlink.entity;

import org.group3.tutorlink.enums.PostStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post extends BaseEntity{

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private PostStatus status;

    // Post n -- 1 Tutor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    // Post 1 -- 0..* Review
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)

    private Set<Review> reviews ;

    // Post 1 -- 0..* Application
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)

    private Set<Application> applications ;
}
