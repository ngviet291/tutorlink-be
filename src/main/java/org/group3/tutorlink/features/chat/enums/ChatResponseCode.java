package org.group3.tutorlink.features.chat.enums;

import lombok.Getter;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
public enum ChatResponseCode implements ResponseCode {

    SEND_MESSAGE_SUCCESS("CHAT_001", "Send message successfully"),
    GET_CHANNELS_SUCCESS("CHAT_002", "Get chat channels successfully"),
    GET_CHANNEL_SUCCESS("CHAT_003", "Get chat channel successfully"),
    GET_MESSAGES_SUCCESS("CHAT_004", "Get messages successfully"),
    MARK_READ_SUCCESS("CHAT_005", "Mark messages as read successfully"),
    GET_UNREAD_COUNT_SUCCESS("CHAT_006", "Get unread count successfully"),
    ;


    private final String code;
    private final String message;

    ChatResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
