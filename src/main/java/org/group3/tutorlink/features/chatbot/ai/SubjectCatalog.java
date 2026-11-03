package org.group3.tutorlink.features.chatbot.ai;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.chatbot.config.ChatbotProperties;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Danh sách tên môn học để AI chọn đúng tên khi phân tích tin nhắn. Cache vài phút, không query DB mỗi tin nhắn. */
@Component
@RequiredArgsConstructor
public class SubjectCatalog {

    private final SubjectRepository subjectRepository;
    private final ChatbotProperties properties;

    // Chỉ giữ 1 biến volatile trỏ tới bản chụp bất biến nên không bao giờ đọc phải dữ liệu nửa cũ nửa mới
    private volatile Snapshot snapshot;

    public List<String> names() {
        Snapshot current = snapshot;
        if (current != null && !current.isExpired(properties.getSubjectsCacheTtl())) {
            return current.names;
        }

        Snapshot fresh = new Snapshot(loadNames());
        // DB còn trống (dữ liệu mẫu có thể nạp sau khi app chạy) thì không cache để lần sau đọc lại
        if (!fresh.names.isEmpty()) {
            snapshot = fresh;
        }
        return fresh.names;
    }

    private List<String> loadNames() {
        return subjectRepository.findAll().stream()
                .map(Subject::getName)
                .sorted()
                .toList();
    }

    private static final class Snapshot {
        private final List<String> names;
        private final Instant loadedAt = Instant.now();

        private Snapshot(List<String> names) {
            this.names = names;
        }

        private boolean isExpired(Duration ttl) {
            return loadedAt.plus(ttl).isBefore(Instant.now());
        }
    }
}