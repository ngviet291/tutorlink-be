package org.group3.tutorlink.common.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.tracking.event.SystemActionOccurred;
import org.group3.tutorlink.features.tracking.mapper.SystemLogMapper;
import org.group3.tutorlink.features.tracking.repo.SystemLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemLogListener {

    private final SystemLogRepository repository;
    private final SystemLogMapper mapper;
    private final AppUtil appUtil;

    // Commit thành công, hoặc publish ngoài transaction
    @Async("trackingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCommit(SystemActionOccurred event) {
        save(event);
    }

    // Transaction rollback: chỉ giữ lại log FAILED
    @Async("trackingExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_ROLLBACK,
            condition = "#event.status.name() == 'FAILED'")
    public void onRollback(SystemActionOccurred event) {
        save(event);
    }

    private void save(SystemActionOccurred event) {
        try {
            repository.save(mapper.toSystemLog(event, appUtil.generateUUID()));
        } catch (Exception e) {
            log.error("Failed to save system log {}", event.getAction(), e);
        }
    }
}