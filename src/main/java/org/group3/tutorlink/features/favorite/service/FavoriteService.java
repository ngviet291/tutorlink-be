package org.group3.tutorlink.features.favorite.service;

import org.group3.tutorlink.features.favorite.dto.response.FavoriteResponse;

import java.util.List;
import java.util.UUID;

public interface FavoriteService {
    FavoriteResponse add(UUID tutorId);
    void remove(UUID tutorId);
    List<FavoriteResponse> findMine();
}
