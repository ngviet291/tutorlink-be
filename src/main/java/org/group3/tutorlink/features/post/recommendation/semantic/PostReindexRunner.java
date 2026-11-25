package org.group3.tutorlink.features.post.recommendation.semantic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostStatus;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.group3.tutorlink.features.post.specification.PostSpecification;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** Index lại các bài đang PUBLISHED khi khởi động, cùng cờ với TutorReindexRunner. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "matching.semantic", name = "reindex-on-startup", havingValue = "true")
public class PostReindexRunner implements ApplicationRunner {
    private static final int PAGE_SIZE = 100;

    private final PostRepository postRepository;
    private final PostVectorIndexer indexer;

    @Override
    public void run(ApplicationArguments args) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("id"));
        int total = 0;
        try {
            Page<Post> page;
            do {
                page = postRepository.findAll(PostSpecification.hasStatus(PostStatus.PUBLISHED), pageable);
                page.forEach(post -> indexer.index(post.getId()));
                total += page.getNumberOfElements();
                pageable = page.nextPageable();
            } while (page.hasNext());
            log.info("[POST_REINDEX] Reindexed {} posts", total);
        } catch (RuntimeException e) {
            // Lỗi embedding không được làm app không khởi động được
            log.error("[POST_REINDEX] Stopped after {} posts", total, e);
        }
    }
}