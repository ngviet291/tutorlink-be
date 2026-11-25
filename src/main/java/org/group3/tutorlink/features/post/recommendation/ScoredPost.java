
package org.group3.tutorlink.features.post.recommendation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.application.enums.MatchWarning;
import org.group3.tutorlink.features.post.entity.Post;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Bài đăng đã được chấm điểm cho một người dùng cụ thể. */
@Getter
@RequiredArgsConstructor
public class ScoredPost {

    /** Điểm cao trước; bằng điểm thì bài mới hơn (id UUIDv7 lớn hơn) trước. */
    public static final Comparator<ScoredPost> BY_RELEVANCE = Comparator
            .comparingDouble(ScoredPost::getScore).reversed()
            .thenComparing(scored -> scored.getPost().getId(), Comparator.reverseOrder());

    private final Post post;
    /** Điểm 0..100. */
    private final double score;
    private final Map<String, Double> breakdown;
    private final List<MatchWarning> warnings;
}