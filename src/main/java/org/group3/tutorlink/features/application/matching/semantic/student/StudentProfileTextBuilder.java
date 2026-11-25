package org.group3.tutorlink.features.application.matching.semantic.student;

import org.group3.tutorlink.common.utils.SemanticText;
import org.group3.tutorlink.features.user.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentProfileTextBuilder {

    /** Rỗng khi học viên chưa khai cấp lớp lẫn mục tiêu học, không có gì để so ngữ nghĩa. */
    public String build(Student student) {
        return SemanticText.create()
                .line("Cấp lớp", student.getGrade())
                .line("Mục tiêu học", student.getLearningGoal())
                .build();
    }
}