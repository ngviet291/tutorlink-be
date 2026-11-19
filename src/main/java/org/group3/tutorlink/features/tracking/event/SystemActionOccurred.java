package org.group3.tutorlink.features.tracking.event;

import lombok.*;
import org.group3.tutorlink.features.tracking.enums.LogStatus;
import org.group3.tutorlink.features.tracking.enums.SystemAction;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemActionOccurred {

    private String requestId;
    private String sessionId;
    private UUID userId;
    private SystemAction action;
    private LogStatus status;
    private String entityType;
    private String entityId;
    private Map<String, Object> details;
    private Instant occurredAt;
}