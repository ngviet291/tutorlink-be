package org.group3.tutorlink.features.auth.event;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TutorCertificatesSubmittedEvent {
    private UUID tutorId;

}