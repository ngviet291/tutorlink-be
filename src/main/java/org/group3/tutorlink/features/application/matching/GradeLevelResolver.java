package org.group3.tutorlink.features.application.matching;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.service.LocationNormalizationService;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Quy "Lớp 10", "Năm 2", "THPT"... của học viên về cấp học; không nhận ra thì rỗng. */
@Component
@RequiredArgsConstructor
public class GradeLevelResolver {

    private static final Pattern NUMBER = Pattern.compile("(\\d{1,2})");

    private final LocationNormalizationService normalizer;

    public Optional<EducationLevel> resolve(String grade) {
        String text = normalizer.normalizeLocationText(grade);
        if (text == null) {
            return Optional.empty();
        }
        if (text.contains("dai hoc") || text.contains("cao dang") || text.contains("sinh vien")
                || text.matches("^nam\\s*\\d.*")) {
            return Optional.of(EducationLevel.UNIVERSITY);
        }
        if (text.contains("tieu hoc")) return Optional.of(EducationLevel.PRIMARY);
        if (text.contains("thcs") || text.contains("trung hoc co so")) return Optional.of(EducationLevel.SECONDARY);
        if (text.contains("thpt") || text.contains("trung hoc pho thong")) return Optional.of(EducationLevel.HIGH_SCHOOL);

        Matcher m = NUMBER.matcher(text);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n >= 1 && n <= 5) return Optional.of(EducationLevel.PRIMARY);
            if (n >= 6 && n <= 9) return Optional.of(EducationLevel.SECONDARY);
            if (n >= 10 && n <= 12) return Optional.of(EducationLevel.HIGH_SCHOOL);
        }
        return Optional.empty();
    }
}