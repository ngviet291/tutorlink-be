package org.group3.tutorlink.features.application.service;

import org.group3.tutorlink.features.application.dto.response.RankedApplicationsResponse;

import java.util.UUID;

public interface ApplicationRankingService {

    /**
     * Xếp hạng các đơn đang chờ (PENDING) vào bài đăng của người gọi:
     * gia sư ứng tuyển bài FIND_TUTOR (học viên xem) hoặc học viên ứng tuyển bài FIND_STUDENT (gia sư xem).
     * Chỉ tác giả bài đăng được gọi.
     */
    RankedApplicationsResponse rankApplications(UUID postId);
}