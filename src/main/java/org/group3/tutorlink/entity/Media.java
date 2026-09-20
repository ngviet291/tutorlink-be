package org.group3.tutorlink.entity;

import org.group3.tutorlink.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media extends BaseEntity{

    @Id
    private UUID id;
    private String publicId;
    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    private MediaType type;

    // Media n -- 1 Message
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
