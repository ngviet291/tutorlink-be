package org.group3.tutorlink.features.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.enums.Role;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.post.dto.request.CreatePostRequest;
import org.group3.tutorlink.features.post.dto.response.PostResponse;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.enums.TeachingMode;
import org.group3.tutorlink.features.post.exception.AddressNotFoundException;
import org.group3.tutorlink.features.post.exception.PostErrorCode;
import org.group3.tutorlink.features.post.exception.PostValidationException;
import org.group3.tutorlink.features.post.mapper.PostMapper;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.exception.SubjectNotFoundException;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final SubjectRepository subjectRepository;
    private final PostMapper postMapper;
    private final AppUtil appUtil;
    private final UserRepository userRepository;

    @Transactional
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
        System.out.println("Post to be saved: " + post.getAuthor().getEmail());
        Post savedPost = postRepository.saveAndFlush(post);
        return postMapper.toPostResponse(savedPost);
    }

    private void validateAuthor(PostType type, User user) {

        log.info("Validating author for post type: {} and user role: {}", type, user.getRole().getName());

        if (type == PostType.FIND_TUTOR
                && !Role.STUDENT.name().equals(user.getRole().getName())) {

            throw new PostValidationException(
                    PostErrorCode.AUTHOR_MUST_BE_STUDENT_FOR_FIND_TUTOR
            );
        }

        if (type == PostType.FIND_STUDENT
                && !Role.TUTOR.name().equals(user.getRole().getName())) {

            throw new PostValidationException(
                    PostErrorCode.AUTHOR_MUST_BE_TUTOR_FOR_FIND_STUDENT
            );
        }
    }
}
