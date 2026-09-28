package org.group3.tutorlink.features.subject.exception;

import org.group3.tutorlink.common.exception.AppException;

public class SubjectNotFoundException extends AppException {
    public SubjectNotFoundException() {
        super(SubjectErrorCode.SUBJECT_NOT_FOUND);
    }
}
