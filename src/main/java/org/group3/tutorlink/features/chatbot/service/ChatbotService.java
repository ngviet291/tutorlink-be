package org.group3.tutorlink.features.chatbot.service;

import org.group3.tutorlink.features.chatbot.dto.request.ChatMessageRequest;
import org.group3.tutorlink.features.chatbot.dto.response.ChatMessageResponse;
import org.group3.tutorlink.features.chatbot.dto.response.ChatResponse;
import org.group3.tutorlink.features.chatbot.dto.response.ConversationResponse;

import java.util.List;
import java.util.UUID;

public interface ChatbotService {

    ChatResponse sendMessage(ChatMessageRequest request, UUID userId);

    List<ConversationResponse> getMyConversations();

    List<ChatMessageResponse> getMessages(UUID conversationId);

    void deleteConversation(UUID conversationId);
}
