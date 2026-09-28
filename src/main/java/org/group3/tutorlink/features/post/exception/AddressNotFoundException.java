package org.group3.tutorlink.features.post.exception;

import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.exception.BaseErrorCode;

public class AddressNotFoundException extends AppException {
    public AddressNotFoundException() {
        super(PostErrorCode.ADDRESS_NOT_FOUND);
    }
}
