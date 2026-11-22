package org.group3.tutorlink.features.review.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ReviewInvalidComplaintStatusException extends AppException {
    public ReviewInvalidComplaintStatusException() {
        super(ReviewErrorCode.INVALID_COMPLAINT_STATUS);
    }
}
