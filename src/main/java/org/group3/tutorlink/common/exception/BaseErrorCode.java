package org.group3.tutorlink.common.exception;

import org.springframework.http.HttpStatusCode;

public interface BaseErrorCode {
    String getMessage();

    HttpStatusCode getHttpStatus();

    String toString();
}