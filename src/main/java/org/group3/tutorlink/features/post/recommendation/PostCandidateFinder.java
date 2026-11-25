package org.group3.tutorlink.features.post.recommendation;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.config.MatchingProperties;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.post.specification.PostSpecification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Lấy tập bài ứng viên để chấm điểm: bài mới nhất mà người dùng thực sự ứng tuyển được. */
@Component
@RequiredArgsConstructor
public class PostCandidateFinder {

    private final PostRepository postRepository;
    private final MatchingProperties properties;

    /**
     * Bài PUBLISHED còn hạn, đúng loại, không phải của người dùng và người dùng chưa ứng tuyển.
     *
     * @param subjectIds giới hạn theo môn; null là không giới hạn
     */
    public List<Post> find(PostType type, UUID userId, Collection<UUID> subjectIds) {
        Specification<Post> spec = Specification
                .where(PostSpecification.hasStatus(PostStatus.PUBLISHED))
                .and(PostSpecification.hasType(type))
                .and(PostSpecification.notExpired(Instant.now()))
                .and(PostSpecification.notAuthoredBy(userId))
                .and(PostSpecification.notAppliedBy(userId))
                .and(PostSpecification.subjectIdIn(subjectIds))
                .and(PostSpecification.fetchSubject())
                .and(PostSpecification.fetchAuthor());

        Pageable pageable = PageRequest.of(
                0,
                properties.getPostFeed().getCandidatePoolSize(),
                Sort.by(Sort.Order.desc("id")));

        return postRepository.findAll(spec, pageable).getContent();
    }
}