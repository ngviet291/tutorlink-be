package org.group3.tutorlink.features.chat.repository;

import org.group3.tutorlink.features.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
}