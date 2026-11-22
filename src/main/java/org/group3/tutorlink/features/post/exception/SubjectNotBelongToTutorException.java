package org.group3.tutorlink.features.post.exception;

import org.group3.tutorlink.common.exception.AppException;

public class SubjectNotBelongToTutorException extends AppException {
    public SubjectNotBelongToTutorException() {
        super(PostErrorCode.SUBJECT_NOT_BELONG_TO_TUTOR);
    }
}
