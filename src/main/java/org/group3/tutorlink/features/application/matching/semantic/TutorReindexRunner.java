package org.group3.tutorlink.features.application.matching.semantic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "matching.semantic", name = "reindex-on-startup", havingValue = "true")
public class TutorReindexRunner implements ApplicationRunner {
    private static final int PAGE_SIZE = 100;

    private final TutorRepository tutorRepository;
    private final TutorVectorIndexer indexer;

    @Override
    public void run(ApplicationArguments args) {
        Pageable pageable = PageRequest.ofSize(PAGE_SIZE);
        Slice<Tutor> slice;
        do {
            slice = tutorRepository.findAll(pageable);
            slice.forEach(tutor -> indexer.index(tutor.getId()));
            pageable = slice.nextPageable();
        } while (slice.hasNext());
        log.info("[TUTOR_REINDEX] Reindexed {} tutors", tutorRepository.count());
    }
}