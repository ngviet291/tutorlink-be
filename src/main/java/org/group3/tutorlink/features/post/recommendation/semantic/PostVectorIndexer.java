package org.group3.tutorlink.features.post.recommendation.semantic;

import java.util.UUID;

public interface PostVectorIndexer {

    /** Index (hoặc cập nhật) bài đăng; bài không còn tồn tại hoặc đã bị gỡ thì xóa khỏi vector store. */
    void index(UUID postId);

    void remove(UUID postId);
}