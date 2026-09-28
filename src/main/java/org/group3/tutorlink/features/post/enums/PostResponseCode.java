package org.group3.tutorlink.features.post.enums;

import lombok.Getter;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
public enum PostResponseCode implements ResponseCode {
    POST_CREATED("POST_201", "Post created successfully"),
    POST_UPDATED("POST_200", "Post updated successfully"),
    POST_DELETED("POST_200", "Post deleted successfully"),
    POST_NOT_FOUND("POST_404", "Post not found"),
    POST_LIST_RETRIEVED("POST_200", "Post list retrieved successfully");

    private final String code;
    private final String message;

    PostResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }


}