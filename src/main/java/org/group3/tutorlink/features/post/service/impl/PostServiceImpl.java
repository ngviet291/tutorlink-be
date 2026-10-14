package org.group3.tutorlink.features.post.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.post.dto.request.CreatePostRequest;
import org.group3.tutorlink.features.post.dto.request.UpdatePostRequest;
import org.group3.tutorlink.features.post.dto.response.PostResponse;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.post.exception.PostNotFoundException;
import org.group3.tutorlink.features.post.mapper.PostMapper;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.post.specification.PostSpecification;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.exception.SubjectNotFoundException;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements org.group3.tutorlink.features.post.service.PostService {
    private final PostRepository postRepository;
    private final SubjectRepository subjectRepository;
    private final PostMapper postMapper;
    private final AppUtil appUtil;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public PostResponse createPost(CreatePostRequest request) {
        User user = userRepository.findById(appUtil.userIdFromAuthentication())
                .orElseThrow(UserNotFoundException::new);
        /*
            type = FIND_TUTOR → author phải là Student
            type = FIND_STUDENT → author phải là Tutor
         */
        validateAuthor(request.getType(), user);

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(SubjectNotFoundException::new);


        Post post = postMapper.toPost(appUtil.generateUUID(), user, subject, request);
        Post savedPost = postRepository.saveAndFlush(post);
        return postMapper.toPostResponse(savedPost);
    }

    @Transactional(readOnly = true)
    @Override
    public PostResponse getPostByIdAndMe(UUID postId) {
        UUID currentUserId = appUtil.userIdFromAuthentication();

        Post post = postRepository.findByIdWithSubjectAndWithAuthor(postId, currentUserId)
                .orElseThrow(PostNotFoundException::new);

        return postMapper.toPostResponse(post);
    }

    @Transactional(readOnly = true)
    @Override
    public PostResponse getPostById(UUID postId) {
        Post post = postRepository.findByIdAndStatusNot(postId)
                .orElseThrow(PostNotFoundException::new);

        return postMapper.toPostResponse(post);
    }


    @Transactional(readOnly = true)
    @Override
    public CursorResponse<PostResponse> getAllPosts(
            String keyword,
            PostType type,
            String subjectName,
            TeachingMode teachingMode,
            EducationLevel educationLevel,
            BigDecimal maxBudget,
            BigDecimal minBudget,
            UUID cursor,
            int limit
    ) {

        Specification<Post> spec = Specification
                .where(PostSpecification.keyword(keyword))
                .and(PostSpecification.hasStatus(PostStatus.PUBLISHED))
                .and(PostSpecification.hasType(type))
                .and(PostSpecification.hasSubject(subjectName))
                .and(PostSpecification.hasTeachingMode(teachingMode))
                .and(PostSpecification.hasEducationLevel(educationLevel))
                .and(PostSpecification.maxBudgetLessThanOrEqual(maxBudget))
                .and(PostSpecification.minBudgetGreaterThanOrEqual(minBudget))
                .and(PostSpecification.fetchSubject())
                .and(PostSpecification.cursor(cursor));

        Pageable pageable = PageRequest.of(
                0,
                limit + 1,
                Sort.by(Sort.Order.desc("id"))
        );

        List<Post> posts = postRepository.findAll(spec, pageable).getContent();

        return appUtil.buildCursorResponse(
                posts,
                limit,
                Post::getId,
                postMapper::toPostResponse
        );

    }

    @Transactional
    @Override
    public PostResponse updatePost(UUID postId, UpdatePostRequest request) {
        Post post = postRepository.findByIdWithSubjectAndWithAuthor(postId, appUtil.userIdFromAuthentication())
                .orElseThrow(PostNotFoundException::new);


        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(SubjectNotFoundException::new);

        validateAuthor(request.getType(), post.getAuthor());

        postMapper.updatePostFromRequest(post, request);

        post.setSubject(subject);

        Post updatedPost = postRepository.save(post);
        return postMapper.toPostResponse(updatedPost);
    }

    @Override
    public void closePost(UUID postId){
        Post post = postRepository.findByIdWithSubjectAndWithAuthor(postId, appUtil.userIdFromAuthentication())
                .orElseThrow(PostNotFoundException::new);
        post.setStatus(PostStatus.CLOSED);
    }


    @Override
    public void removePost(UUID postId){
        Post post = postRepository.findByIdWithSubjectAndWithAuthor(postId, appUtil.userIdFromAuthentication())
                .orElseThrow(PostNotFoundException::new);
        post.setStatus(PostStatus.REMOVED);
    }

    @Override
    public CursorResponse<PostResponse> getCurrentUserPosts(String keyword, PostType type, String subjectName, TeachingMode teachingMode, EducationLevel educationLevel, BigDecimal maxBudget, BigDecimal minBudget, UUID cursor, PostStatus postStatus, int limit) {
        Specification<Post> spec = Specification
                .where(PostSpecification.keyword(keyword))
                .and(PostSpecification.hasStatus(PostStatus.PUBLISHED))
                .and(PostSpecification.hasType(type))
                .and(PostSpecification.hasSubject(subjectName))
                .and(PostSpecification.hasTeachingMode(teachingMode))
                .and(PostSpecification.hasEducationLevel(educationLevel))
                .and(PostSpecification.maxBudgetLessThanOrEqual(maxBudget))
                .and(PostSpecification.minBudgetGreaterThanOrEqual(minBudget))
                .and(PostSpecification.fetchSubject())
                .and(PostSpecification.cursor(cursor))
                .and(PostSpecification.hasStatusNotRemoved(postStatus))
                .and(PostSpecification.hasAuthorId(appUtil.userIdFromAuthentication()))
                ;

        Pageable pageable = PageRequest.of(
                0,
                limit + 1,
                Sort.by(Sort.Order.desc("id"))
        );

        List<Post> posts = postRepository.findAll(spec, pageable).getContent();

        return appUtil.buildCursorResponse(
                posts,
                limit,
                Post::getId,
                postMapper::toPostResponse
        );
    }


}
