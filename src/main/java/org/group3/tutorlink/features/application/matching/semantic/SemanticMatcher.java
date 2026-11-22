package org.group3.tutorlink.features.application.matching.semantic;

import org.group3.tutorlink.features.post.entity.Post;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface SemanticMatcher {

    /** Độ phù hợp ngữ nghĩa trong [0, 1] giữa bài đăng và từng gia sư. Gia sư chưa được index sẽ vắng mặt trong kết quả. */
    Map<UUID, Double> score(Post post, Collection<UUID> candidateTutorIds);
}