package org.group3.tutorlink.features.tracking.service;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.filter.TrackingContextFilter;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.tracking.entity.SystemLog;
import org.group3.tutorlink.features.tracking.enums.LogStatus;
import org.group3.tutorlink.features.tracking.enums.SystemAction;
import org.group3.tutorlink.features.tracking.repo.SystemLogRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRepository repository;
    private final AppUtil appUtil;

    /** Ghi đồng bộ, tham gia transaction hiện tại */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSync(UUID userId, SystemAction action, String entityType,
                           Object entityId, Map<String, Object> details) {
        SystemLog entity = SystemLog.builder()
                .id(appUtil.generateUUID())
                .requestId(MDC.get(TrackingContextFilter.REQUEST_ID))
                .sessionId(MDC.get(TrackingContextFilter.SESSION_ID))
                .userId(userId)
                .action(action)
                .status(LogStatus.SUCCESS)
                .entityType(entityType)
                .entityId(entityId != null ? entityId.toString() : null)
                .details(details)
                .build();
        repository.save(entity);
    }
}