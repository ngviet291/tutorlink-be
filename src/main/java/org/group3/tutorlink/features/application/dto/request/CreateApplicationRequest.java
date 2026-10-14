/*
 * @ (#) CreateApplicationRequest,java       1.0    29/09/2026
 *
 * Copyright (c) 2026 IUH. All righta reserved.
 */

package org.group3.tutorlink.features.application.dto.request;

/*
 * @description:
 * @author: Ho Thi Kim Xuyen
 * @version:     1.0
 * @date: 29/09/2026 14:56
 */
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request used to apply to a post")
public class CreateApplicationRequest {

    @NotNull(message = "Post ID is required")
    @Schema(
            description = "ID of the post to apply to",
            example = "123e4567-e89b-12d3-a456-426614174000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID postId;

    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    @Schema(
            description = "Optional message to the post owner",
            example = "Em có kinh nghiệm dạy Toán lớp 12.",
            maxLength = 1000
    )
    private String message;
}