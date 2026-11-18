package org.group3.tutorlink.features.post.mapper;

import org.group3.tutorlink.common.entity.Address;
import org.group3.tutorlink.features.post.dto.request.AddressRequest;
import org.group3.tutorlink.features.post.dto.request.CreatePostRequest;
import org.group3.tutorlink.features.post.dto.request.UpdatePostRequest;
import org.group3.tutorlink.features.post.dto.response.PostResponse;
import org.group3.tutorlink.features.post.dto.response.SubjectSummaryResponse;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.user.entity.User;
import org.mapstruct.*;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class PostMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "author", source = "user")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "address", source = "request.address")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "removalReason", ignore = true)
    public abstract Post toPost(
            UUID id,
            User user,
            Subject subject,
            CreatePostRequest request
    );

    public abstract Address toAddress(AddressRequest request);

    public abstract SubjectSummaryResponse toSubjectSummaryResponse(Subject subject);
    
    @Mapping(target = "author.id", source = "post.author.id")
    @Mapping(target = "author.email", source = "post.author.email")
    @Mapping(target = "author.fullName", source = "post.author.fullName")
    @Mapping(target = "author.avatarUrl", source = "post.author.avatarUrl")
    @Mapping(target = "author.role", source = "post.author.role.name")
    @Mapping(target = "applicationCount", ignore = true)
    public abstract PostResponse toPostResponse(Post post);
    
    @AfterMapping
    protected void fillApplicationCount(Post post, @MappingTarget PostResponse response) {
        try {
            if (post != null && post.getApplications() != null) {
                response.setApplicationCount(post.getApplications().size());
            } else {
                response.setApplicationCount(0);
            }
        } catch (Exception e) {
            // Fallback nếu lazy loading fails
            response.setApplicationCount(0);
        }
    }

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract void updatePostFromRequest(
            @MappingTarget Post post,
            UpdatePostRequest request
    );
}
