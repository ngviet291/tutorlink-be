package org.group3.tutorlink.features.auth.exception;


import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.exception.ErrorCode;

public class UserNotFoundException extends AppException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
