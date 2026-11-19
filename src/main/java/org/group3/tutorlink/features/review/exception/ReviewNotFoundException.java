package org.group3.tutorlink.features.review.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ReviewNotFoundException extends AppException {
    public ReviewNotFoundException() {
        super(ReviewErrorCode.NOT_FOUND);
    }
}
