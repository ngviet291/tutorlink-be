package org.group3.tutorlink.features.chatbot.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ConversationResponse {
    private UUID id;
    private String title;
    private Instant lastMessageAt;
}
