package org.group3.tutorlink.features.post.service;

import org.group3.tutorlink.features.post.recommendation.dto.RecommendedPostResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PostRecommendationService {

    /**
     * Top bài đăng phù hợp nhất với người dùng hiện tại (gia sư hoặc học viên).
     *
     * @param limit số bài tối đa; null thì dùng mặc định trong cấu hình, vượt mức tối đa thì cắt về mức tối đa
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('STUDENT', 'TUTOR')")
    List<RecommendedPostResponse> recommend(Integer limit);
}