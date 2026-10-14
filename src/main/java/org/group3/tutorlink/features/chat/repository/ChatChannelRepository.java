package org.group3.tutorlink.features.chat.repository;

import org.group3.tutorlink.features.chat.entity.ChatChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatChannelRepository extends JpaRepository<ChatChannel, UUID> {


        @Query("""
        SELECT c FROM ChatChannel c
        WHERE (c.sender.id = :userA AND c.receiver.id = :userB)
           OR (c.sender.id = :userB AND c.receiver.id = :userA)
    """)
        Optional<ChatChannel> findBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);
}