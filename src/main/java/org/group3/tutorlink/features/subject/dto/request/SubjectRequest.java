package org.group3.tutorlink.features.subject.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectRequest {
    @NotBlank(message = "Tên môn học không được để trống")
    private String name;

    private String description;

    @NotBlank(message = "Danh mục không được để trống")
    private String category;
}