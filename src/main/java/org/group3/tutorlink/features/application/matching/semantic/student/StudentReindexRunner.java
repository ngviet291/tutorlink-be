package org.group3.tutorlink.features.application.matching.semantic.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.repository.StudentRepository;
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
public class StudentReindexRunner implements ApplicationRunner {
    private static final int PAGE_SIZE = 100;

    private final StudentRepository studentRepository;
    private final StudentVectorIndexer indexer;

    @Override
    public void run(ApplicationArguments args) {
        Pageable pageable = PageRequest.ofSize(PAGE_SIZE);
        Slice<Student> slice;
        do {
            slice = studentRepository.findAll(pageable);
            slice.forEach(student -> indexer.index(student.getId()));
            pageable = slice.nextPageable();
        } while (slice.hasNext());
        log.info("[STUDENT_REINDEX] Reindexed {} students", studentRepository.count());
    }
}