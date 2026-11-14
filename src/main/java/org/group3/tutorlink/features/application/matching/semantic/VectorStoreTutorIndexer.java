package org.group3.tutorlink.features.application.matching.semantic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreTutorIndexer implements TutorVectorIndexer {
    private final TutorRepository tutorRepository;
    private final VectorStore vectorStore;
    private final TutorProfileTextBuilder textBuilder;

    @Override
    @Transactional
    public void index(UUID tutorId) {
        tutorRepository.findById(tutorId)
                .map(this::toDocument)
                .ifPresentOrElse(
                        document -> vectorStore.add(List.of(document)),
                        () -> log.warn("Skip indexing, tutor {} not found", tutorId));
    }

    @Override
    @Transactional
    public void remove(UUID tutorId) {
        vectorStore.delete(List.of(tutorId.toString()));
    }

    private Document toDocument(Tutor tutor) {
        String id = tutor.getId().toString();
        return Document.builder()
                .id(id)
                .text(textBuilder.build(tutor))
                .metadata(Map.of(TutorDocumentFields.TUTOR_ID, id))
                .build();
    }
}