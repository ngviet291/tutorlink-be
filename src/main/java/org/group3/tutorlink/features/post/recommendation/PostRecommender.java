package org.group3.tutorlink.features.post.recommendation;

import org.group3.tutorlink.features.user.entity.User;

import java.util.List;

public interface PostRecommender {
    boolean supports(User user);

    List<ScoredPost> recommend(User user, int limit);
}
