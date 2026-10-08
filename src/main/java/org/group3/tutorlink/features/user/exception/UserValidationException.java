package org.group3.tutorlink.features.user.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.AppException;

@Getter
public class UserValidationException extends AppException {
    public UserValidationException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
