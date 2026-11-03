package org.group3.tutorlink.features.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.group3.tutorlink.features.chat.enums.MediaType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request object for media upload")
public class MediaRequest {
    @Schema(description = "Public ID of the media", example = "sample_public_id")
    @NotBlank(message = "Public ID cannot be blank")
    private String publicId;
    @Schema(description = "URL of the media", example = "https://example.com/media/sample.jpg")
    @NotBlank(message = "URL cannot be blank")
    private String url;
    @Schema(description = "Type of the media", example = "IMAGE")
    @NotNull(message = "Media type cannot be null")
    private MediaType mediaType;
}