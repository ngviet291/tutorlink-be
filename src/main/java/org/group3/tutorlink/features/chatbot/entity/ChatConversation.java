package org.group3.tutorlink.features.chatbot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.group3.tutorlink.common.entity.BaseEntity;
import org.group3.tutorlink.features.user.entity.User;

import java.time.Instant;
import java.util.UUID;

/** Một cuộc trò chuyện của thành viên đã đăng nhập (guest không được lưu). */
@Entity
@Table(name = "chat_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation extends BaseEntity {

    @Id
    private UUID id;

    private String title;

    private Instant lastMessageAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
