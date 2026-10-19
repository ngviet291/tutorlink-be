package org.group3.tutorlink.features.review.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ReviewInvalidTutorIdException extends AppException {
    public ReviewInvalidTutorIdException() {
        super(ReviewErrorCode.INVALID_TUTOR_ID);
    }
}
