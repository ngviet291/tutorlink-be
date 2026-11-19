package org.group3.tutorlink.features.tracking.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.enums.ResponseCode;

@RequiredArgsConstructor
@Getter
public enum TrackingResponseCode implements ResponseCode {
    TRACKING_SUCCESS("TRACKING_SUCCESS", "Tracking events successfully")
    ;
    private final String code;
    private final String message;
}
