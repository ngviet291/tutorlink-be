package org.group3.tutorlink.features.favorite.mapper;

import org.group3.tutorlink.features.favorite.dto.response.FavoriteResponse;
import org.group3.tutorlink.features.favorite.dto.response.FavoriteSubjectResponse;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FavoriteMapper {

    @Mapping(target = "tutorId", source = "id")
    @Mapping(target = "province", source = "address.province")
    FavoriteResponse toFavoriteResponse(Tutor tutor);

    FavoriteSubjectResponse toFavoriteSubjectResponse(Subject subject);
}