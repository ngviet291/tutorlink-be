package org.group3.tutorlink.features.statistics.enums;

import lombok.Getter;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
public enum StatisticsResponseCode implements ResponseCode {

    GET_REVENUE_STATISTICS("STATISTICS_200", "Revenue statistics retrieved successfully!"),
    GET_ACCEPTED_APPLICATIONS_COUNT("STATISTICS_201", "Accepted applications count retrieved successfully!"),
    GET_TOP_TUTORS("STATISTICS_202", "Top tutors retrieved successfully!");

    private final String code;
    private final String message;

    StatisticsResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
