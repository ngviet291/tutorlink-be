package org.group3.tutorlink.features.post.mapper;

import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.post.dto.request.AddressRequest;
import org.group3.tutorlink.features.post.dto.request.CreatePostRequest;
import org.group3.tutorlink.features.post.dto.response.PostResponse;
import org.group3.tutorlink.features.post.dto.response.SubjectSummaryResponse;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface PostMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "author", source = "user")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "address", source = "request.address")
    @Mapping(target = "status", constant = "PUBLISHED")
    @Mapping(target = "applications", ignore = true)
    Post toPost(
            UUID id,
            User user,
            Subject subject,
            CreatePostRequest request
    );

    Address toAddress(AddressRequest request);

    SubjectSummaryResponse toSubjectSummaryResponse(Subject subject);
    @Mapping(target = "author.id", source = "post.author.id")
    @Mapping(target = "author.email", source = "post.author.email")
    PostResponse toPostResponse(Post post);
}
