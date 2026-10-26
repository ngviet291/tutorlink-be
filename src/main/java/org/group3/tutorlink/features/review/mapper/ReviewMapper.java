package org.group3.tutorlink.features.review.mapper;

import org.group3.tutorlink.features.review.dto.response.ReviewResponse;
import org.group3.tutorlink.features.review.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "tutorId", source = "tutor.id")
    ReviewResponse toResponse(Review review);
}
