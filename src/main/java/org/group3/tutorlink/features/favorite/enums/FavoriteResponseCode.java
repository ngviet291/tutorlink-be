package org.group3.tutorlink.features.favorite.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.enums.ResponseCode;

@Getter
@RequiredArgsConstructor
public enum FavoriteResponseCode implements ResponseCode {
    ADD_SUCCESS("FAVORITE_201", "Tutor added to favorites"),
    REMOVE_SUCCESS("FAVORITE_204", "Tutor removed from favorites"),
    GET_SUCCESS("FAVORITE_200", "Favorites retrieved successfully");

    private final String code;
    private final String message;
}
