package org.group3.tutorlink.features.chatbot.exception;

import org.group3.tutorlink.common.exception.AppException;

public class AiOverloadedException extends AppException {
    public AiOverloadedException() {
        super(ChatbotErrorCode.AI_OVERLOADED);
    }
}
