package org.group3.tutorlink.features.chat.expection;

import lombok.Getter;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatErrorCode implements BaseErrorCode {

    CHAT_ONLY_TUTOR_STUDENT("CHAT-001", "Chat is only allowed between a tutor and a student", HttpStatus.FORBIDDEN),
    NOT_PARTICIPATING("CHAT-002", "You are not a participant of this chat", HttpStatus.FORBIDDEN),
    CHAT_CHANNEL_NOT_FOUND("CHAT-003", "Chat channel not found", HttpStatus.NOT_FOUND),
    CANNOT_MESSAGE_SELF("CHAT-004", "Cannot send message to yourself", HttpStatus.BAD_REQUEST),



    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ChatErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
