package org.group3.tutorlink.features.chatbot.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatbotErrorCode implements BaseErrorCode {
    CONVERSATION_NOT_FOUND("CHAT_404", "Conversation not found", HttpStatus.NOT_FOUND),
    AI_OVERLOADED("CHAT_503", "Máy chủ AI đang quá tải, vui lòng thử lại sau ít phút", HttpStatus.SERVICE_UNAVAILABLE),
    AI_UNAVAILABLE("CHAT_502", "Trợ lý AI tạm thời không khả dụng, vui lòng thử lại sau", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ChatbotErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
