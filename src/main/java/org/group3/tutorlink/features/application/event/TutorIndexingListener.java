package org.group3.tutorlink.features.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.application.matching.semantic.TutorVectorIndexer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TutorIndexingListener {
    private final TutorVectorIndexer indexer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileChanged(TutorProfileChangedEvent event) {
        try {
            indexer.index(event.getTutorId());
        } catch (RuntimeException e) {
            log.error("Failed to index tutor {}", event.getTutorId(), e);
        }
    }
}