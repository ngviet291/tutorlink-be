package org.group3.tutorlink.features.review.mapper;

import org.group3.tutorlink.features.review.dto.response.ReviewComplaintResponse;
import org.group3.tutorlink.features.review.entity.ReviewComplaint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewComplaintMapper {
    @Mapping(target = "reviewId", source = "review.id")
    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "tutorName", source = "tutor.fullName")
    @Mapping(target = "adminId", source = "admin.id")
    ReviewComplaintResponse toResponse(ReviewComplaint complaint);
}