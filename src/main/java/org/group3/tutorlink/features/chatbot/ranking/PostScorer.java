package org.group3.tutorlink.features.chatbot.ranking;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.chatbot.ai.PostSearchFilters;
import org.group3.tutorlink.features.chatbot.config.ChatbotProperties;
import org.group3.tutorlink.features.chatbot.config.Scoring;
import org.group3.tutorlink.features.post.entity.Post;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

/**
 * Chấm điểm một bài đăng theo mức phù hợp với yêu cầu (0..100).
 *
 * Điểm = tổng (độ khớp từng tiêu chí × trọng số) / tổng trọng số của các tiêu chí đang áp dụng.
 * Tiêu chí người dùng không nhắc tới thì bỏ qua, không làm giảm điểm bài nào.
 * Trọng số và ngưỡng chỉnh ở {@link Scoring}.
 */
@Component
@RequiredArgsConstructor
public class PostScorer {

    private final ChatbotProperties properties;
    private final SearchAliasService aliases;

    // Điểm giảm một nửa sau mỗi chu kỳ bán rã
    private static final double HALF = 0.5;

    /** Độ giống tên môn (0..1), dùng cả để loại bài lệch môn quá xa. */
    public double subjectSimilarity(Post post, PostSearchFilters filters) {
        return TextSimilarity.similarity(
                aliases.subject(filters.getSubjectName()),
                aliases.subject(post.getSubject().getName()),
                properties.getScoring().getContainsSimilarity());
    }

    /**
     * @param distanceKm khoảng cách tới điểm tham chiếu; null nếu không có điểm tham chiếu;
     *                   NaN nếu bài không phụ thuộc vị trí (online / không có địa chỉ)
     * @param radiusKm   bán kính dùng để quy đổi khoảng cách ra điểm (gần hơn thì cao hơn)
     */
    public double score(Post post, PostSearchFilters filters, Double distanceKm, double radiusKm, Instant now) {
        Scoring w = properties.getScoring();
        double total = 0;
        double weights = 0;

        if (StringUtils.hasText(filters.getSubjectName())) {
            total += w.getSubjectWeight() * subjectSimilarity(post, filters);
            weights += w.getSubjectWeight();
        }

        if (distanceKm != null) {
            // NaN = bài không phụ thuộc vị trí (dạy online / chưa có địa chỉ): được nửa điểm, không bị loại
            double distanceScore = distanceKm.isNaN() ? w.getLocationFreeScore() : Math.max(0, 1 - distanceKm / radiusKm);
            total += w.getDistanceWeight() * distanceScore;
            weights += w.getDistanceWeight();
        }

        if (filters.getEducationLevel() != null) {
            total += w.getLevelWeight() * (filters.getEducationLevel() == post.getEducationLevel() ? 1 : 0);
            weights += w.getLevelWeight();
        }

        // Độ mới luôn áp dụng: bài đăng lâu thì điểm giảm dần theo nửa vòng đời
        total += w.getRecencyWeight() * recency(post, now, w.getRecencyHalfLifeDays());
        weights += w.getRecencyWeight();

        return weights == 0 ? 0 : 100 * total / weights;
    }

    private double recency(Post post, Instant now, double halfLifeDays) {
        Instant created = post.getCreatedAt();
        if (created == null || halfLifeDays <= 0) {
            return properties.getScoring().getUnknownRecencyScore();
        }
        double ageDays = Math.max(0, Duration.between(created, now).toHours() / 24.0);
        return Math.pow(HALF, ageDays / halfLifeDays);
    }
}
