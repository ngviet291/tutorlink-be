package org.group3.tutorlink.features.chatbot.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.group3.tutorlink.features.chatbot.enums.ChatIntent;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ChatResponse {

    //Guest thì null
    private UUID conversationId;
    private ChatIntent intent;
    private String message;
    private List<PostSummaryResponse> posts;
}
