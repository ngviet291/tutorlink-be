package org.group3.tutorlink.features.chatbot.ranking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quy tên gọi tiếng Anh / viết liền của môn học và tỉnh thành về tên tiếng Việt không dấu,
 * để người dùng (kể cả người nước ngoài) gõ "Math", "Hanoi", "Saigon"... vẫn tìm được.
 * Danh sách nằm trong file JSON (mặc định data/search_aliases.json), thêm bớt không cần sửa code:
 * {"subjects": {"math": "toan"}, "provinces": {"hanoi": "ha noi"}}
 */
@Slf4j
@Component
public class SearchAliasService {

    private static final TypeReference<Map<String, Map<String, String>>> FILE_TYPE = new TypeReference<>() {
    };
    private static final String SUBJECTS = "subjects";
    private static final String PROVINCES = "provinces";

    @Value("${app.chatbot.aliases-file:classpath:data/search_aliases.json}")
    private Resource source;

    // alias dài xếp trước để "ho chi minh city" thắng "hcm"
    private Map<String, String> subjects = Map.of();
    private Map<String, String> provinces = Map.of();

    @PostConstruct
    void load() {
        try (InputStream in = source.getInputStream()) {
            Map<String, Map<String, String>> data = new ObjectMapper().readValue(in, FILE_TYPE);
            subjects = prepare(data.get(SUBJECTS));
            provinces = prepare(data.get(PROVINCES));
            log.info("Đã nạp {} tên gọi môn học và {} tên gọi tỉnh thành", subjects.size(), provinces.size());
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được file tên gọi môn học/tỉnh thành", e);
        }
    }

    /** "Mathematics" -> "toan", "Advanced Math" -> "advanced toan"; tên tiếng Việt giữ nguyên (không dấu). */
    public String subject(String value) {
        return replace(TextSimilarity.normalize(value), subjects);
    }

    /** "Hanoi" -> "ha noi". Trả nguyên văn nếu không phải tên gọi quen thuộc. */
    public String province(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String normalized = TextSimilarity.normalize(value);
        String mapped = replace(normalized, provinces);
        return mapped.equals(normalized) ? value : mapped;
    }

    // Thay theo từ nguyên vẹn
    private String replace(String text, Map<String, String> aliases) {
        String result = " " + text + " ";
        for (Map.Entry<String, String> e : aliases.entrySet()) {
            result = result.replace(" " + e.getKey() + " ", " " + e.getValue() + " ");
        }
        return result.trim();
    }

    private Map<String, String> prepare(Map<String, String> raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        raw.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed())
                .forEach(e -> out.put(TextSimilarity.normalize(e.getKey()), TextSimilarity.normalize(e.getValue())));
        return out;
    }
}
