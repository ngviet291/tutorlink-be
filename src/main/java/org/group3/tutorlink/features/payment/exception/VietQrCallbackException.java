package org.group3.tutorlink.features.payment.exception;

import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.features.payment.enums.VietQrCallbackCode;

public class VietQrCallbackException extends AppException {
    public VietQrCallbackException( VietQrCallbackCode vietQrCallbackCode) {
        super(vietQrCallbackCode);
    }
}
