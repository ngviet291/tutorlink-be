package org.group3.tutorlink.features.review.exception;

import org.group3.tutorlink.common.exception.AppException;

public class ReviewComplaintNotFoundException extends AppException {
    public ReviewComplaintNotFoundException() {
        super(ReviewErrorCode.COMPLAINT_NOT_FOUND);
    }
}
