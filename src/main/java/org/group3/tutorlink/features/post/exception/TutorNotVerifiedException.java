package org.group3.tutorlink.features.post.exception;

import org.group3.tutorlink.common.exception.AppException;

public class TutorNotVerifiedException extends AppException {

    public TutorNotVerifiedException() {
        super(PostErrorCode.TUTOR_NOT_VERIFIED);
    }
}