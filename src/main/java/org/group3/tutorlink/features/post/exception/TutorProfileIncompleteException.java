package org.group3.tutorlink.features.post.exception;

import org.group3.tutorlink.common.exception.AppException;

public class TutorProfileIncompleteException extends AppException {

    public TutorProfileIncompleteException() {
        super(PostErrorCode.TUTOR_PROFILE_INCOMPLETE);
    }
}
