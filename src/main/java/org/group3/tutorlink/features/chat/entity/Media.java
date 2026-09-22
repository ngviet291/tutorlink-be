package org.group3.tutorlink.features.chat.entity;

import org.group3.tutorlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.features.chat.enums.MediaType;
import org.group3.tutorlink.features.user.entity.User;
import java.util.UUID;

@Entity
@Table(name = "media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media extends BaseEntity {

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
