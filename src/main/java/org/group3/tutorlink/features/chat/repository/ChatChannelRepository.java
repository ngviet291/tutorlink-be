package org.group3.tutorlink.features.chat.repository;

import org.group3.tutorlink.features.chat.entity.ChatChannel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatChannelRepository extends JpaRepository<ChatChannel, UUID> {

        @Query("""
        SELECT c FROM ChatChannel c
        JOIN FETCH c.sender JOIN FETCH c.receiver
        WHERE (c.sender.id = :userA AND c.receiver.id = :userB)
           OR (c.sender.id = :userB AND c.receiver.id = :userA)
    """)
        Optional<ChatChannel> findBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);

        @Query("""
        SELECT c FROM ChatChannel c
        JOIN FETCH c.sender JOIN FETCH c.receiver
        WHERE c.id = :id
    """)
        Optional<ChatChannel> findWithParticipantsById(@Param("id") UUID id);

        @Query("""
        SELECT c FROM ChatChannel c
        JOIN FETCH c.sender JOIN FETCH c.receiver
        WHERE c.sender.id = :userId OR c.receiver.id = :userId
        ORDER BY c.lastMessageAt DESC, c.id DESC
    """)
        List<ChatChannel> findFirstPage(@Param("userId") UUID userId, Pageable pageable);

        @Query("""
        SELECT c FROM ChatChannel c
        JOIN FETCH c.sender JOIN FETCH c.receiver
        WHERE (c.sender.id = :userId OR c.receiver.id = :userId)
          AND (c.lastMessageAt < :cursorTime
               OR (c.lastMessageAt = :cursorTime AND c.id < :cursorId))
        ORDER BY c.lastMessageAt DESC, c.id DESC
    """)
        List<ChatChannel> findNextPage(@Param("userId") UUID userId,
                                       @Param("cursorTime") Instant cursorTime,
                                       @Param("cursorId") UUID cursorId,
                                       Pageable pageable);
}