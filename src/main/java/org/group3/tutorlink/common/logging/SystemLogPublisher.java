package org.group3.tutorlink.common.logging;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.filter.TrackingContextFilter;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.tracking.enums.LogStatus;
import org.group3.tutorlink.features.tracking.enums.SystemAction;
import org.group3.tutorlink.features.tracking.event.SystemActionOccurred;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SystemLogPublisher {

    private final ApplicationEventPublisher publisher;
    private final AppUtil appUtil;

    /** userId lấy từ SecurityContext */
    public void success(SystemAction action, String entityType, Object entityId,
                        Map<String, Object> details) {
        publish(appUtil.userIdFromAuthenticationOrNull(), action, LogStatus.SUCCESS, entityType, entityId, details);
    }

    /** Dùng khi SecurityContext chưa có user nhưng userId đã biết, ví dụ LOGIN thành công */
    public void success(UUID userId, SystemAction action, String entityType, Object entityId,
                        Map<String, Object> details) {
        publish(userId, action, LogStatus.SUCCESS, entityType, entityId, details);
    }

    public void failed(SystemAction action, String errorCode, Map<String, Object> details) {
        Map<String, Object> merged = new HashMap<>();
        if (details != null) {
            merged.putAll(details);
        }
        merged.put("errorCode", errorCode);
        publish(appUtil.userIdFromAuthenticationOrNull(), action, LogStatus.FAILED, null, null, merged);
    }

    private void publish(UUID userId, SystemAction action, LogStatus status,
                         String entityType, Object entityId, Map<String, Object> details) {
        SystemActionOccurred event = SystemActionOccurred.builder()
                .requestId(MDC.get(TrackingContextFilter.REQUEST_ID))
                .sessionId(MDC.get(TrackingContextFilter.SESSION_ID))
                .userId(userId)
                .action(action)
                .status(status)
                .entityType(entityType)
                .entityId(entityId != null ? entityId.toString() : null)
                .details(details)
                .occurredAt(Instant.now())
                .build();
        publisher.publishEvent(event);
    }


}