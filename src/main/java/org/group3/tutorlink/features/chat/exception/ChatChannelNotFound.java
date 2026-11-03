package org.group3.tutorlink.features.chat.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ChatChannelNotFound extends AppException {
    public ChatChannelNotFound() {
        super(ChatErrorCode.CHAT_CHANNEL_NOT_FOUND);
    }
}
