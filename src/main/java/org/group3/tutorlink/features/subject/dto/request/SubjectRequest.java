package org.group3.tutorlink.features.subject.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request used to create or update a subject!")
public class SubjectRequest {

    @NotBlank(message = "Subject name must not be blank!")
    @Size(max = 255, message = "Subject name must not exceed 255 characters!")
    @Schema(
            description = "Name of the subject!",
            example = "Toán học",
            maxLength = 255,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters!")
    @Schema(
            description = "Short description of the subject!",
            example = "Toán học phổ thông",
            maxLength = 255
    )
    private String description;

    @NotBlank(message = "Category must not be blank!")
    @Size(max = 255, message = "Category must not exceed 255 characters!")
    @Schema(
            description = "Category of the subject!",
            example = "Khoa học tự nhiên",
            maxLength = 255,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String category;
}
