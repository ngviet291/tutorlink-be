package org.group3.tutorlink.features.review.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ReviewInvalidApplicationException extends AppException {
    public ReviewInvalidApplicationException() {
        super(ReviewErrorCode.INVALID_APPLICATION);
    }
}
