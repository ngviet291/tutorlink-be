package org.group3.tutorlink.features.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.group3.tutorlink.features.chat.validation.annotation.ValidMessageContent;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidMessageContent
@Schema(description = "Request to send a message")
public class SendMessageRequest {
    @Schema(description = "The ID of the receiver", example = "123e4567-e89b-12d3-a456-426614174001")
    @NotNull(message = "receiverId cannot be null")
    private UUID receiverId;

    @Schema(description = "The content of the message. Can be empty if media is provided.", example = "Hello, how are you?")
    private String content; // có thể rỗng nếu có media

    @Schema(description = "List of media attached to the message. Maximum 10 items.", example = "[{\"publicId\": \"media1\", \"url\": \"http://example.com/media1.jpg\", \"mediaType\": \"IMAGE\"}]")
    @Valid
    @Size(max = 10)
    private List<MediaRequest> mediaList;
}