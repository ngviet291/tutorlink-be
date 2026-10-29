package org.group3.tutorlink.features.post.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.post.dto.request.CreatePostRequest;
import org.group3.tutorlink.features.post.dto.request.UpdatePostRequest;
import org.group3.tutorlink.features.post.dto.response.PostApplicationResponse;
import org.group3.tutorlink.features.post.dto.response.PostResponse;
import org.group3.tutorlink.features.post.dto.response.PostStatisticsByStatus;
import org.group3.tutorlink.features.post.enums.*;
import org.group3.tutorlink.features.post.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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

    @GetMapping("/admin")
    public ApiResponse<CursorResponse<PostResponse>> getAllPostsAdmin(
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
                .data(postService.getAllPostsAdmin(keyword,
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


    @GetMapping("/{postId}/applications")
    public ApiResponse<CursorResponse<PostApplicationResponse>> getPostApplications(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) UUID cursor
    ) {
        return ApiResponse.<CursorResponse<PostApplicationResponse>>builder()
                .code(PostResponseCode.GET_POST_APPLICATIONS.getCode())
                .message(PostResponseCode.GET_POST_APPLICATIONS.getMessage())
                .data(postService.getPostApplications(postId, limit, cursor))
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

    @PatchMapping("{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closePost(@PathVariable UUID id) {
        postService.closePost(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable UUID id) {
        postService.removePost(id);
    }

    @GetMapping("/me")
    public ApiResponse<CursorResponse<PostResponse>> getMyPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) TeachingMode teachingMode,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false) EducationLevel educationLevel,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(required = false) PostStatus postStatus,
            @RequestParam(defaultValue = "10") int limit
    ) {

        return ApiResponse.<CursorResponse<PostResponse>>builder()
                .code(PostResponseCode.GET_POSTS.getCode())
                .message(PostResponseCode.GET_POSTS.getMessage())
                .data(postService.getCurrentUserPosts(keyword,
                        type,
                        subjectName,
                        teachingMode,
                        educationLevel,
                        minBudget,
                        maxBudget,
                        cursor,
                        postStatus,
                        limit)
                )
                .build();
    }


    @PatchMapping("/{postId}/publish")
    public ApiResponse<PostResponse> publishPost(@PathVariable UUID postId) {
        return ApiResponse.<PostResponse>builder()
                .code(PostResponseCode.POST_PUBLISHED.getCode())
                .message(PostResponseCode.POST_PUBLISHED.getMessage())
                .data(postService.publishPost(postId))
                .build();
    }

    @PatchMapping("/{postId}/remove")
    public void removePost(@PathVariable UUID postId) {
        postService.removePostAdmin(postId);
    }

    @GetMapping("/statistics/posts-by-status")
    public ApiResponse<List<PostStatisticsByStatus>> getStatisticsByStatus() {
        return ApiResponse.<List<PostStatisticsByStatus>>builder()
                .code(PostResponseCode.GET_STATISTICS_BY_STATUS.getCode())
                .message(PostResponseCode.GET_STATISTICS_BY_STATUS.getMessage())
                .data(postService.getStatisticsByStatus())
                .build();
    }
}