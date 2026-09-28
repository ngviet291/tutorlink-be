package org.group3.tutorlink.features.post.exception;

import lombok.Getter;
import org.group3.tutorlink.common.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum PostErrorCode implements BaseErrorCode {
    POST_NOT_FOUND("POST_404", "Post not found", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND("ADDRESS_404", "Address not found", HttpStatus.NOT_FOUND),
    AUTHOR_MUST_BE_STUDENT_FOR_FIND_TUTOR(
            "POST_001",
            "Author must be a Student for FIND_TUTOR posts", HttpStatus.BAD_REQUEST
    ),

    AUTHOR_MUST_BE_TUTOR_FOR_FIND_STUDENT(
            "POST_002",
            "Author must be a Tutor for FIND_STUDENT posts", HttpStatus.BAD_REQUEST
    );
    ;
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    PostErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
