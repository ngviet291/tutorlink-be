package org.group3.tutorlink.features.application.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {
    private UUID id;
    private String fullName;
    private String avatarUrl;
    
    // Thông tin thêm cho Tutor (null nếu là Student)
    private Double averageRating;
    private Long totalReviews;
}