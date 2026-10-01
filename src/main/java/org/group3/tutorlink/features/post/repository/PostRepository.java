package org.group3.tutorlink.features.post.repository;

import org.group3.tutorlink.features.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> , JpaSpecificationExecutor<Post> {

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



}
