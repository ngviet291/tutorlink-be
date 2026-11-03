package org.group3.tutorlink.features.tracking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "DTO representing an event item for tracking purposes")
public class TrackEventItem {

    @Schema(
            description = "Name of the event",
            example = "page_view",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Size(max = 64)
    private String name;

    @Schema(
            description = "Properties of the event",
            example = "{\"user_id\": \"123\", \"page\": \"/home\"}",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Map<String, Object> props;

    @Schema(
            description = "Timestamp of the event in ISO 8601 format",
            example = "2023-08-01T12:34:56Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    private Instant ts;
}

