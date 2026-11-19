package org.group3.tutorlink.features.favorite.controller;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.favorite.dto.response.FavoriteResponse;
import org.group3.tutorlink.features.favorite.enums.FavoriteResponseCode;
import org.group3.tutorlink.features.favorite.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public ApiResponse<FavoriteResponse> addTutorToFavorites(@PathVariable UUID tutorId) {
        return ApiResponse.<FavoriteResponse>builder()
                .code(FavoriteResponseCode.ADD_SUCCESS.getCode())
                .message(FavoriteResponseCode.ADD_SUCCESS.getMessage())
                .data(favoriteService.add(tutorId))
                .build();
    }

    @DeleteMapping("/{tutorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> removeTutorFromFavorites(@PathVariable UUID tutorId) {
        favoriteService.remove(tutorId);
        return ApiResponse.<Void>builder()
                .code(FavoriteResponseCode.REMOVE_SUCCESS.getCode())
                .message(FavoriteResponseCode.REMOVE_SUCCESS.getMessage())
                .build();
    }

    @GetMapping
    public ApiResponse<List<FavoriteResponse>> getMyFavorites() {
        return ApiResponse.<List<FavoriteResponse>>builder()
                .code(FavoriteResponseCode.GET_SUCCESS.getCode())
                .message(FavoriteResponseCode.GET_SUCCESS.getMessage())
                .data(favoriteService.findMine())
                .build();
    }
}
