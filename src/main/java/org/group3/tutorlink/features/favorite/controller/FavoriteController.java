package org.group3.tutorlink.features.favorite.controller;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.favorite.dto.response.FavoriteResponse;
import org.group3.tutorlink.features.favorite.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/favorites")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping("/{tutorId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoriteResponse> add(@PathVariable UUID tutorId) {
        return ApiResponse.<FavoriteResponse>builder().code("FAVORITE_201")
                .message("Tutor added to favorites").data(favoriteService.add(tutorId)).build();
    }

    @DeleteMapping("/{tutorId}")
    public ApiResponse<Void> remove(@PathVariable UUID tutorId) {
        favoriteService.remove(tutorId);
        return ApiResponse.<Void>builder().code("FAVORITE_204")
                .message("Tutor removed from favorites").build();
    }

    @GetMapping
    public ApiResponse<List<FavoriteResponse>> findMine() {
        return ApiResponse.<List<FavoriteResponse>>builder().code("FAVORITES_200")
                .message("Favorites retrieved successfully").data(favoriteService.findMine()).build();
    }
}
