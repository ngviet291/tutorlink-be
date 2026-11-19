package org.group3.tutorlink.features.chatbot.repository;

import org.group3.tutorlink.features.chatbot.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    Optional<ChatConversation> findByIdAndUser_Id(UUID id, UUID userId);

    List<ChatConversation> findAllByUser_IdOrderByLastMessageAtDesc(UUID userId);
}
