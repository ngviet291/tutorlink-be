package org.group3.tutorlink.features.post.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.post.dto.request.CreatePostRequest;
import org.group3.tutorlink.features.post.dto.request.UpdatePostRequest;
import org.group3.tutorlink.features.post.dto.response.PostResponse;
import org.group3.tutorlink.features.post.enums.*;
import org.group3.tutorlink.features.post.service.PostService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ApiResponse<PostResponse> createPost(@Valid @RequestBody CreatePostRequest createPostRequest) {
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.POST_CREATED.getCode())
                .message(PostResponseCode.POST_CREATED.getMessage())
                .data(postService.createPost(createPostRequest))
                .build();
    }

    @GetMapping("/{postId}/me")
    public ApiResponse<PostResponse> getPostByIdAndMe(@PathVariable UUID postId) {
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.POST_FOUND.getCode())
                .message(PostResponseCode.POST_FOUND.getMessage())
                .data(postService.getPostByIdAndMe(postId))
                .build();
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPostById(@PathVariable UUID postId) {
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.POST_FOUND.getCode())
                .message(PostResponseCode.POST_FOUND.getMessage())
                .data(postService.getPostById(postId))
                .build();
    }

    @GetMapping
    public ApiResponse<CursorResponse<PostResponse>> getAllPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) TeachingMode teachingMode,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false) EducationLevel educationLevel,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(defaultValue = "10") int limit
    ) {

        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .code(PostResponseCode.GET_POSTS.getCode())
                .message(PostResponseCode.GET_POSTS.getMessage())
                .data(postService.getAllPosts(keyword,
                        type,
                        subjectName,
                        teachingMode,
                        educationLevel,
                        minBudget,
                        maxBudget,
                        cursor,
                        limit)
                )
                .build();
    }

    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest updatePostRequest
    ) {
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.POST_UPDATED.getCode())
                .message(PostResponseCode.POST_UPDATED.getMessage())
                .data(postService.updatePost(postId, updatePostRequest))
                .build();
    }

}
