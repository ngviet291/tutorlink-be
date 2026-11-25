package org.group3.tutorlink.features.application.matching.semantic.student;

import java.util.UUID;

public interface StudentVectorIndexer {
    void index(UUID studentId);

    void remove(UUID studentId);
}