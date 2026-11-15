package org.group3.tutorlink.features.favorite.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class FavoriteResponse {
    UUID tutorId;
    String fullName;
    String avatarUrl;
    String bio;
    int experienceYears;
    BigDecimal hourlyRate;
    double averageRating;
    String verificationStatus;
    String province;
    List<FavoriteSubjectResponse> subjects;
}