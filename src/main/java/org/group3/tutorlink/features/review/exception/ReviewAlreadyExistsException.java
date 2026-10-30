package org.group3.tutorlink.features.review.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ReviewAlreadyExistsException extends AppException {
    public ReviewAlreadyExistsException() {
        super(ReviewErrorCode.ALREADY_EXISTS);
    }
}
