package org.group3.tutorlink.features.subject.exception;

import org.group3.tutorlink.common.exception.AppException;

public class SubjectInUseException extends AppException {
    public SubjectInUseException() {
        super(SubjectErrorCode.SUBJECT_IN_USE);
    }
}
