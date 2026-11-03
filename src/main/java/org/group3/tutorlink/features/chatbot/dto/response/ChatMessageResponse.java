package org.group3.tutorlink.features.chatbot.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.group3.tutorlink.features.chatbot.enums.ChatRole;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ChatMessageResponse {
    private UUID id;
    private ChatRole role;
    private String content;
    private Instant createdAt;
}
