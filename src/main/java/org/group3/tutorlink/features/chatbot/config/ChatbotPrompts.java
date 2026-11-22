package org.group3.tutorlink.features.chatbot.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatbotPrompts {

    @NotBlank
    private String system;

    @NotBlank
    private String user;
}