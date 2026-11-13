package org.group3.tutorlink.features.application.matching.semantic;

import org.group3.tutorlink.common.utils.SemanticText;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TutorProfileTextBuilder {

    public String build(Tutor tutor) {
        return SemanticText.create()
                .line("Môn dạy", joinSubjects(tutor))
                .line("Cấp học", join(tutor.getTeachingLevels(), Enum::name))
                .line("Kinh nghiệm", tutor.getExperienceYears() + " năm")
                .line("Giới thiệu", tutor.getBio()) // [kiểm tra] tên field bio
                .build();
    }

    private String joinSubjects(Tutor tutor) {
        return join(tutor.getSubjects(), Subject::getName);
    }

    private <T> String join(Collection<T> items, Function<T, String> label) {
        if (items == null) return null;
        return items.stream().map(label).collect(Collectors.joining(", "));
    }
}