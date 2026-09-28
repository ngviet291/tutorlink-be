package org.group3.tutorlink.features.post.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PostValidationException extends AppException {
    public PostValidationException(PostErrorCode errorCode) {
        super(errorCode);
    }
}