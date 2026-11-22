package org.group3.tutorlink.features.post.exception;

import org.group3.tutorlink.common.exception.AppException;

public class PostNotFoundException extends AppException{
    public PostNotFoundException() {
        super(PostErrorCode.POST_NOT_FOUND);
    }
}
