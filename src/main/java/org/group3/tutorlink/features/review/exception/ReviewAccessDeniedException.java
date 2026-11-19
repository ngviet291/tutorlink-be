package org.group3.tutorlink.features.review.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ReviewAccessDeniedException extends AppException {
    public ReviewAccessDeniedException() {
        super(ReviewErrorCode.NOT_ALLOWED);
    }
}
