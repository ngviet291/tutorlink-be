package org.group3.tutorlink.features.favorite.repository;

import org.group3.tutorlink.features.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    boolean existsByUserIdAndTutorId(UUID userId, UUID tutorId);
    void deleteByUserIdAndTutorId(UUID userId, UUID tutorId);
    List<Favorite> findByUserId(UUID userId);
}
