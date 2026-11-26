package org.group3.tutorlink.features.post.recommendation.semantic;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface PostSemanticMatcher {

    /**
     * Độ phù hợp ngữ nghĩa [0, 1] giữa hồ sơ người dùng và từng bài đăng.
     * Bài chưa được index vắng mặt trong kết quả; vector store lỗi hoặc hồ sơ trống thì trả map rỗng.
     */
    Map<UUID, Double> score(String profileText, Collection<UUID> candidatePostIds);
}