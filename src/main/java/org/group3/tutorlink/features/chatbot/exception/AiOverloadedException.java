package org.group3.tutorlink.features.chatbot.exception;

import org.group3.tutorlink.common.exception.AppException;

/** Gemini trả về HTTP 503: máy chủ AI đang quá tải (đã retry hết số lần cấu hình mà vẫn lỗi). */
public class AiOverloadedException extends AppException {
    public AiOverloadedException() {
        super(ChatbotErrorCode.AI_OVERLOADED);
    }
}
