package org.group3.tutorlink.features.favorite.service.impl;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.favorite.dto.response.FavoriteResponse;
import org.group3.tutorlink.features.favorite.entity.Favorite;
import org.group3.tutorlink.features.favorite.repository.FavoriteRepository;
import org.group3.tutorlink.features.favorite.service.FavoriteService;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.entity.User;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final AppUtil appUtil;

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public FavoriteResponse add(UUID tutorId) {
        UUID userId = appUtil.userIdFromAuthentication();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Tutor tutor = tutorRepository.findById(tutorId).orElseThrow(UserNotFoundException::new);
        if (!favoriteRepository.existsByUserIdAndTutorId(userId, tutorId)) {
            favoriteRepository.save(Favorite.builder().id(appUtil.generateUUID()).user(user).tutor(tutor).build());
        }
        return FavoriteResponse.builder().tutorId(tutorId).build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void remove(UUID tutorId) {
        UUID userId = appUtil.userIdFromAuthentication();
        favoriteRepository.deleteByUserIdAndTutorId(userId, tutorId);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<FavoriteResponse> findMine() {
        UUID userId = appUtil.userIdFromAuthentication();
        return favoriteRepository.findByUserId(userId).stream()
                .map(f -> FavoriteResponse.builder().tutorId(f.getTutor().getId()).build()).toList();
    }
}
