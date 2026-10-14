package org.group3.tutorlink.common.exception;

public class UploadMediaException extends AppException {
    public UploadMediaException() {
        super(ErrorCode.UPLOAD_MEDIA_FAILED);
    }
}
