package org.group3.tutorlink.features.post.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.post.dto.request.CreatePostRequest;
import org.group3.tutorlink.features.post.dto.response.PostResponse;
import org.group3.tutorlink.features.post.enums.PostResponseCode;
import org.group3.tutorlink.features.post.service.PostService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
