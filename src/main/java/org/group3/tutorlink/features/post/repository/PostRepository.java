package org.group3.tutorlink.features.post.repository;

import org.group3.tutorlink.features.post.dto.response.PostApplicationResponse;
import org.group3.tutorlink.features.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {

    @Query("""
                  SELECT p FROM Post p
                  JOIN FETCH p.subject
                  JOIN FETCH p.author
                  WHERE p.id = :id AND p.status != 'REMOVED' AND p.author.id = :authorId
            """)
    Optional<Post> findByIdWithSubjectAndWithAuthor(UUID id, UUID authorId);

    @Query("""
                  SELECT p FROM Post p
                  JOIN FETCH p.subject
                  JOIN FETCH p.author
                  WHERE p.id = :id AND p.status = 'PUBLISHED'
            """)
    Optional<Post> findByIdAndStatusNot(UUID id);

    @Query("""
                SELECT new org.group3.tutorlink.features.post.dto.response.PostApplicationResponse(
                    app.id,
                    CAST(
                        CASE
                            WHEN p.type = org.group3.tutorlink.features.post.enums.PostType.FIND_TUTOR
                            THEN tutor.id
                            ELSE student.id
                        END AS String
                    ),
                    CASE
                        WHEN p.type = org.group3.tutorlink.features.post.enums.PostType.FIND_TUTOR
                        THEN tutor.fullName
                        ELSE student.fullName
                    END,
                    CASE
                        WHEN p.type = org.group3.tutorlink.features.post.enums.PostType.FIND_TUTOR
                        THEN tutor.email
                        ELSE student.email
                    END,
                    CASE
                        WHEN p.type = org.group3.tutorlink.features.post.enums.PostType.FIND_TUTOR
                        THEN tutor.avatarUrl
                        ELSE student.avatarUrl
                    END,
                    CAST(app.applicationStatus AS String),
                    CAST(app.appliedAt AS String)
                )
                FROM Post p
                JOIN p.applications app
                LEFT JOIN app.tutor tutor
                LEFT JOIN app.student student
                WHERE p.id = :id
                  AND p.status <> org.group3.tutorlink.features.post.enums.PostStatus.REMOVED
                  AND p.author.id = :authorId
                  AND (:cursor IS NULL OR app.id < :cursor)
            """)
    List<PostApplicationResponse> findPostApplications(
            UUID id,
            UUID authorId, Pageable pageable, UUID cursor
    );

    @Query("""
                SELECT new org.group3.tutorlink.features.post.dto.response.PostStatisticsByStatus(
                    p.status,
                    COUNT(p)
                )
                FROM Post p
                GROUP BY p.status
            """)
    List<org.group3.tutorlink.features.post.dto.response.PostStatisticsByStatus> getStatisticsByStatus();
}
