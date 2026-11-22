package org.group3.tutorlink.features.subject.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
@RequiredArgsConstructor
public enum SubjectResponseCode implements ResponseCode {

    GET_SUBJECTS("SUBJECT_200", "Subjects retrieved successfully!"),
    SUBJECT_FOUND("SUBJECT_200", "Subject found successfully!"),
    SUBJECT_CREATED("SUBJECT_201", "Subject created successfully!"),
    SUBJECT_UPDATED("SUBJECT_200", "Subject updated successfully!");

    private final String code;
    private final String message;
}
