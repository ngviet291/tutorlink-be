package org.group3.tutorlink.features.chatbot.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageRequest {

    private UUID conversationId;

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 1000, message = "Message is too long")
    private String message;
    //Tùy chọn
    @Valid
    private UserLocation location;
}
