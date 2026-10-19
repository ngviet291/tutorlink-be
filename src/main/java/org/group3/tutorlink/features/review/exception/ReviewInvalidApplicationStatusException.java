package org.group3.tutorlink.features.review.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ReviewInvalidApplicationStatusException extends AppException {
    public ReviewInvalidApplicationStatusException() {
        super(ReviewErrorCode.INVALID_APPLICATION_STATUS);
    }
}
