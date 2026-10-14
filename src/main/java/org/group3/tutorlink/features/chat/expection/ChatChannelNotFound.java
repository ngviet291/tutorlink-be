package org.group3.tutorlink.features.chat.expection;

import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.exception.BaseErrorCode;

public class ChatChannelNotFound extends AppException {
    public ChatChannelNotFound() {
        super(ChatErrorCode.CHAT_CHANNEL_NOT_FOUND);
    }
}
