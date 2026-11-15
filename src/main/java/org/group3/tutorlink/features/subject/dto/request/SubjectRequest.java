package org.group3.tutorlink.features.subject.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.subject.constant.SubjectConstraints;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu tạo hoặc cập nhật môn học.")
public class SubjectRequest {

    @NotBlank(message = "Subject name must not be blank!")
    @Size(max = SubjectConstraints.TEXT_MAX_LENGTH, message = "Subject name must not exceed 255 characters!")
    @Schema(
            description = "Tên môn học.",
            example = "Toán học",
            maxLength = SubjectConstraints.TEXT_MAX_LENGTH,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Size(max = SubjectConstraints.TEXT_MAX_LENGTH, message = "Description must not exceed 255 characters!")
    @Schema(
            description = "Mô tả ngắn về môn học.",
            example = "Toán học phổ thông",
            maxLength = SubjectConstraints.TEXT_MAX_LENGTH
    )
    private String description;

    @NotBlank(message = "Category must not be blank!")
    @Size(max = SubjectConstraints.TEXT_MAX_LENGTH, message = "Category must not exceed 255 characters!")
    @Schema(
            description = "Nhóm môn học.",
            example = "Khoa học tự nhiên",
            maxLength = SubjectConstraints.TEXT_MAX_LENGTH,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String category;
}
