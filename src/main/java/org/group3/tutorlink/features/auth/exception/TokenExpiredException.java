package org.group3.tutorlink.features.auth.exception;


import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.exception.ErrorCode;

public class TokenExpiredException extends AppException {
    public TokenExpiredException() {
        super(ErrorCode.TOKEN_EXPIRED);
    }
}
