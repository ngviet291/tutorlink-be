package org.group3.tutorlink.features.application.service;

import org.group3.tutorlink.features.application.dto.response.RankedApplicationsResponse;

import java.util.UUID;

public interface ApplicationRankingService {

    /**
     * Xếp hạng các gia sư đang ứng tuyển (PENDING) vào bài đăng FIND_TUTOR.
     * Chỉ tác giả bài đăng được gọi.
     */
    RankedApplicationsResponse rankApplications(UUID postId);
}