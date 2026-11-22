package org.group3.tutorlink.features.chatbot.exception;

import org.group3.tutorlink.common.exception.AppException;

public class AiQuotaExceededException extends AppException {
    public AiQuotaExceededException() {
        super(ChatbotErrorCode.AI_QUOTA_EXCEEDED);
    }
}
