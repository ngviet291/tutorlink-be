package org.group3.tutorlink.features.chatbot.repository;

import org.group3.tutorlink.features.chatbot.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;


public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    @EntityGraph(attributePaths = "postIds")
    List<ChatMessage> findAllByConversation_IdOrderByCreatedAtAscIdAsc(UUID conversationId);

    List<ChatMessage> findByConversation_IdOrderByCreatedAtDescIdDesc(UUID conversationId, Pageable pageable);

    void deleteAllByConversation_Id(UUID conversationId);
}