package org.group3.tutorlink.features.review.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ReviewInvalidPaginationException extends AppException {
    public ReviewInvalidPaginationException() {
        super(ReviewErrorCode.INVALID_PAGINATION);
    }
}
