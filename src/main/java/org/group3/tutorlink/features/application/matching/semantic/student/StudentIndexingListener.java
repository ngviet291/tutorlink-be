package org.group3.tutorlink.features.application.matching.semantic.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudentIndexingListener {
    private final StudentVectorIndexer indexer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileChanged(StudentProfileChangedEvent event) {
        try {
            indexer.index(event.getStudentId());
        } catch (RuntimeException e) {
            log.error("Failed to index student {}", event.getStudentId(), e);
        }
    }
}