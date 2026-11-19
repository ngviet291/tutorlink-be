package org.group3.tutorlink.features.chatbot.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ConversationNotFoundException extends AppException {
    public ConversationNotFoundException() {
        super(ChatbotErrorCode.CONVERSATION_NOT_FOUND);
    }
}
