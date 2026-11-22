package org.group3.tutorlink.features.application.matching.semantic;

import java.util.UUID;

public interface TutorVectorIndexer {
    void index(UUID tutorId);

    void remove(UUID tutorId);
}