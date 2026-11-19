package org.group3.tutorlink.features.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/** Định dạng phản hồi mà VietQR mong đợi cho API transaction-sync. */
@Getter
@AllArgsConstructor
public class VietQrCallbackResponse {

    private final boolean error;
    private final String errorReason;
    private final String toastMessage;
    private final Map<String, String> object;

    public static VietQrCallbackResponse success(String message, String refTransactionId) {
        return new VietQrCallbackResponse(false, null, message, Map.of("reftransactionid", refTransactionId));
    }

    public static VietQrCallbackResponse failure(String reason, String message) {
        return new VietQrCallbackResponse(true, reason, message, null);
    }
}
