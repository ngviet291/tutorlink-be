package org.group3.tutorlink.features.user.dto.response;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StudentResponse extends BaseUserResponse {
    private String grade;
    private String learningGoal;
}