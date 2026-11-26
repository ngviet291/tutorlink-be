package org.group3.tutorlink.features.post.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.application.enums.MatchWarning;
import org.group3.tutorlink.features.post.dto.response.PostResponse;

import java.util.List;
import java.util.Map;

/**
 * Một bài đăng được gợi ý kèm điểm phù hợp.
 * score nằm trong [0, 100]; breakdown là điểm từng tiêu chí trong [0, 1]
 * (tiêu chí thiếu dữ liệu thì không có mặt).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedPostResponse {
    private PostResponse post;
    private double score;
    private Map<String, Double> breakdown;
    private List<MatchWarning> warnings;
}