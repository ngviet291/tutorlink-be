package org.group3.tutorlink.features.post.service;

import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.auth.enums.RoleName;
import org.group3.tutorlink.features.post.dto.request.CreatePostRequest;
import org.group3.tutorlink.features.post.dto.request.UpdatePostRequest;
import org.group3.tutorlink.features.post.dto.response.PostResponse;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.post.exception.PostErrorCode;
import org.group3.tutorlink.features.post.exception.PostValidationException;
import org.group3.tutorlink.features.user.entity.User;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

public interface PostService {
    @Transactional
    PostResponse createPost(CreatePostRequest request);

    default void validateAuthor(PostType type, User user) {


        if (type == PostType.FIND_TUTOR
                && !RoleName.STUDENT.name().equals(user.getRole().getName())) {

            throw new PostValidationException(
                    PostErrorCode.AUTHOR_MUST_BE_STUDENT_FOR_FIND_TUTOR
            );
        }

        if (type == PostType.FIND_STUDENT
                && !RoleName.TUTOR.name().equals(user.getRole().getName())) {

            throw new PostValidationException(
                    PostErrorCode.AUTHOR_MUST_BE_TUTOR_FOR_FIND_STUDENT
            );
        }
    }

    @Transactional(readOnly = true)
    PostResponse getPostByIdAndMe(UUID postId);

    @Transactional(readOnly = true)
    PostResponse getPostById(UUID postId);

    @Transactional(readOnly = true)
    CursorResponse<PostResponse> getAllPosts(
            String keyword,
            PostType type,
            String subjectName,
            TeachingMode teachingMode,
            EducationLevel educationLevel,
            BigDecimal maxBudget,
            BigDecimal minBudget,
            UUID cursor,
            int limit
    );

    @Transactional
    PostResponse updatePost(UUID postId, UpdatePostRequest request);
}
