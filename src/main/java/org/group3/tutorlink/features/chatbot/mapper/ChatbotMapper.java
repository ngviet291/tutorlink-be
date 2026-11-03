package org.group3.tutorlink.features.chatbot.mapper;

import org.group3.tutorlink.features.chatbot.dto.response.ChatMessageResponse;
import org.group3.tutorlink.features.chatbot.dto.response.ConversationResponse;
import org.group3.tutorlink.features.chatbot.entity.ChatConversation;
import org.group3.tutorlink.features.chatbot.entity.ChatMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatbotMapper {

    ConversationResponse toConversationResponse(ChatConversation conversation);

    ChatMessageResponse toChatMessageResponse(ChatMessage message);
}
