package org.group3.tutorlink.features.chatbot.enums;

import lombok.Getter;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
public enum ChatbotResponseCode implements ResponseCode {
    MESSAGE_SENT("CHAT_200", "Chatbot replied successfully"),
    GET_CONVERSATIONS("CHAT_201", "Conversations retrieved successfully"),
    GET_MESSAGES("CHAT_202", "Messages retrieved successfully"),
    CONVERSATION_DELETED("CHAT_203", "Conversation deleted successfully");

    private final String code;
    private final String message;

    ChatbotResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
