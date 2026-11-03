package org.group3.tutorlink.features.chat.exception;

import org.group3.tutorlink.common.exception.AppException;

public class CannotMessageSelfException extends AppException {
    public CannotMessageSelfException() {
        super(ChatErrorCode.CANNOT_MESSAGE_SELF);
    }
}