package org.group3.tutorlink.features.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.user.entity.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

    @Id
    private UUID id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private boolean isRead;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    // Message n -- 1 ChatChannel
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_channel_id", nullable = false)
    private ChatChannel chatChannel;

    // Message n -- 1 User (sender) — not explicit in diagram, added for a usable chat model
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // Message 1 -- 0..* Media
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Media> mediaList ;
}
