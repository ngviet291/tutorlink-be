package org.group3.tutorlink.features.application.matching.semantic.student;

import org.group3.tutorlink.features.post.entity.Post;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface StudentSemanticMatcher {

    /** Độ phù hợp ngữ nghĩa [0, 1] giữa bài FIND_STUDENT và từng học viên; học viên chưa được index sẽ vắng mặt. */
    Map<UUID, Double> score(Post post, Collection<UUID> candidateStudentIds);
}