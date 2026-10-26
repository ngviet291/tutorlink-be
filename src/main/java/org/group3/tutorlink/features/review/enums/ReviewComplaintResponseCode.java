package org.group3.tutorlink.features.review.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
@RequiredArgsConstructor
public enum ReviewComplaintResponseCode implements ResponseCode {
    CREATE_SUCCESS("REVIEW_COMPLAINT_201", "Review complaint created successfully"),
    GET_SUCCESS("REVIEW_COMPLAINT_200", "Review complaints retrieved successfully"),
    RESOLVE_SUCCESS("REVIEW_COMPLAINT_200", "Review complaint resolved successfully");

    private final String code;
    private final String message;
}
