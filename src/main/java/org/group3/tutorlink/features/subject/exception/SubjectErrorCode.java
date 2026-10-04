package org.group3.tutorlink.features.subject.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum SubjectErrorCode implements BaseErrorCode {

    SUBJECT_NOT_FOUND("SUBJECT_404", "Subject not found", HttpStatus.NOT_FOUND),
    SUBJECT_IN_USE("SUBJECT_409", "Subject is being used by tutors and cannot be deleted", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    SubjectErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
