package org.group3.tutorlink.features.favorite.repository;

import org.group3.tutorlink.features.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    boolean existsByUserIdAndTutorId(UUID userId, UUID tutorId);
    void deleteByUserIdAndTutorId(UUID userId, UUID tutorId);
    @Query("select f from Favorite f join fetch f.tutor where f.user.id = :userId")
    List<Favorite> findByUserId(@Param("userId") UUID userId);
}
