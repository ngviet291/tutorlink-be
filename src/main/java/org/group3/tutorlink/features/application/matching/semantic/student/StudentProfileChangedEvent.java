package org.group3.tutorlink.features.application.matching.semantic.student;

import lombok.*;

import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentProfileChangedEvent {
    private UUID studentId;
}