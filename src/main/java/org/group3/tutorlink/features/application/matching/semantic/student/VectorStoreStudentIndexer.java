package org.group3.tutorlink.features.application.matching.semantic.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.user.entity.Student;
import org.group3.tutorlink.features.user.repository.StudentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Index hồ sơ học viên vào cùng vector store với gia sư (id học viên khác id gia sư nên không trùng). */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreStudentIndexer implements StudentVectorIndexer {
    private final StudentRepository studentRepository;
    private final VectorStore vectorStore;
    private final StudentProfileTextBuilder textBuilder;

    @Override
    @Transactional
    public void index(UUID studentId) {
        studentRepository.findById(studentId).ifPresentOrElse(this::indexOrRemove,
                () -> log.warn("Skip indexing, student {} not found", studentId));
    }

    @Override
    @Transactional
    public void remove(UUID studentId) {
        vectorStore.delete(List.of(studentId.toString()));
    }

    private void indexOrRemove(Student student) {
        String text = textBuilder.build(student);
        String id = student.getId().toString();
        if (text.isBlank()) {
            // Hồ sơ trống (hoặc vừa bị xóa trắng): bỏ document cũ, không nhúng chuỗi rỗng
            vectorStore.delete(List.of(id));
            return;
        }
        vectorStore.add(List.of(Document.builder()
                .id(id)
                .text(text)
                .metadata(Map.of(StudentDocumentFields.STUDENT_ID, id))
                .build()));
    }
}