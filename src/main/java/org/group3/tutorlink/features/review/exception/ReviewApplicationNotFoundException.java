package org.group3.tutorlink.features.review.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ReviewApplicationNotFoundException extends AppException {
    public ReviewApplicationNotFoundException() {
        super(ReviewErrorCode.APPLICATION_NOT_FOUND);
    }
}
