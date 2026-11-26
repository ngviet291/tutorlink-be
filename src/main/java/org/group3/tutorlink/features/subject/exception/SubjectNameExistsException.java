package org.group3.tutorlink.features.subject.exception;

import org.group3.tutorlink.common.exception.AppException;

public class SubjectNameExistsException extends AppException {
    public SubjectNameExistsException() {
        super(SubjectErrorCode.SUBJECT_NAME_EXISTS);
    }
}