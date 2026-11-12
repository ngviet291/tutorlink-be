package org.group3.tutorlink.features.application.event;

import lombok.*;

import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TutorProfileChangedEvent {
    private UUID tutorId;
}