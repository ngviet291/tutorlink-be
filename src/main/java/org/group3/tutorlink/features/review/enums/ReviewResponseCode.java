package org.group3.tutorlink.features.review.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
@RequiredArgsConstructor
public enum ReviewResponseCode implements ResponseCode {
    REVIEW_CREATED("REVIEW_201", "Review created successfully"),
    REVIEWS_RETRIEVED("REVIEW_200", "Reviews retrieved successfully"),
    REVIEW_FOUND("REVIEW_200", "Review retrieved successfully");

    private final String code;
    private final String message;
}
