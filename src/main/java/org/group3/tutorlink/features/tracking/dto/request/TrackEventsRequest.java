package org.group3.tutorlink.features.tracking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;


@Schema(description = "Request used to track multiple events")
@Getter
@Setter
@NoArgsConstructor
public class TrackEventsRequest {

    @Schema(
            description = "Anonymous ID of the user",
            example = "anon-12345"
    )
    @NotBlank
    @Size(max = 64)
    private String anonId;

    @Schema(
            description = "User ID of the logged-in user (if available)",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    @NotBlank
    @Size(max = 64)
    private String sessionId;

    @Schema(
            description = "List of events to be tracked",
            required = true
    )
    @NotEmpty
    @Size(max = 50)           // giới hạn số sự kiện mỗi lô
    private List<@Valid TrackEventItem> events;
}