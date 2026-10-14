package org.group3.tutorlink.features.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.user.entity.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_channels",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sender_id", "receiver_id"}))
public class ChatChannel extends BaseEntity {

    @Id
    private UUID id;

    private String name;

    private Instant lastMessageAt;

    // ChatChannel n -- 1 Sender (User)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // ChatChannel n -- 1 Receiver (User)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // ChatChannel 1 -- 0..* Message
    @OneToMany(mappedBy = "chatChannel", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Message> messages;
}
