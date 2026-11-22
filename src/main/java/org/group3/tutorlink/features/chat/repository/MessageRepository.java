package org.group3.tutorlink.features.chat.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.group3.tutorlink.features.chat.dto.projection.UnreadCountView;
import org.group3.tutorlink.features.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query(value = """
    SELECT m FROM Message m
    WHERE m.chatChannel.id = :channelId
    AND (:cursor IS NULL OR m.id < :cursor)
    ORDER BY m.id DESC
    """)
    List<Message> findPage(@Param("channelId") UUID channelId,
                           @Param("cursor") UUID cursor,
                           Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Message m SET m.isRead = true
        WHERE m.chatChannel.id = :channelId AND m.sender.id <> :userId AND m.isRead = false
    """)
    int markAllAsRead(@Param("channelId") UUID channelId, @Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(m) FROM Message m
        JOIN m.chatChannel c
        WHERE (c.sender.id = :userId OR c.receiver.id = :userId)
          AND m.sender.id <> :userId AND m.isRead = false
    """)
    long countUnreadByUser(@Param("userId") UUID userId);

    @Query("""
        SELECT m.chatChannel.id AS channelId, COUNT(m) AS total
        FROM Message m
        WHERE m.chatChannel.id IN :channelIds
          AND m.sender.id <> :userId
          AND m.isRead = false
        GROUP BY m.chatChannel.id
        """)
    List<UnreadCountView> countUnreadByChannels(@Param("channelIds") Collection<UUID> channelIds,
                                                @Param("userId") UUID userId);

    @Query("""
        SELECT DISTINCT m FROM Message m
        JOIN FETCH m.sender
        LEFT JOIN FETCH m.mediaList
        WHERE m.chatChannel.id IN :channelIds
          AND m.sentAt = (SELECT MAX(m2.sentAt) FROM Message m2
                          WHERE m2.chatChannel.id = m.chatChannel.id)
        """)
    List<Message> findLatestByChannels(@Param("channelIds") Collection<UUID> channelIds);
}