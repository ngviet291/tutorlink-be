package org.group3.tutorlink.features.tracking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.ApiResponse;
import org.group3.tutorlink.features.tracking.dto.request.TrackEventsRequest;
import org.group3.tutorlink.features.tracking.enums.TrackingResponseCode;
import org.group3.tutorlink.features.tracking.service.TrackingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tracking")
@RequiredArgsConstructor
public class TrackingController {
    private final TrackingService trackingService;

    @PostMapping("/events")
    public ApiResponse<Integer> track(@Valid @RequestBody TrackEventsRequest request) {
        return ApiResponse.<Integer>builder()
                .code(TrackingResponseCode.TRACKING_SUCCESS.getCode())
                .message(TrackingResponseCode.TRACKING_SUCCESS.getMessage())
                .data(trackingService.trackEvents(request))
                .build();
    }
}
