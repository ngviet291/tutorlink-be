package org.group3.tutorlink.features.chatbot.exception;

import org.group3.tutorlink.common.exception.AppException;

public class AiUnavailableException extends AppException {
    public AiUnavailableException() {
        super(ChatbotErrorCode.AI_UNAVAILABLE);
    }
}
