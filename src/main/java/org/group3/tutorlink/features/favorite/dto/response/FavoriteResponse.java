package org.group3.tutorlink.features.favorite.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class FavoriteResponse {
    UUID tutorId;
}
