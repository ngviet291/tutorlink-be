package org.group3.tutorlink.features.tracking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.tracking.dto.request.TrackEventsRequest;
import org.group3.tutorlink.features.tracking.entity.UserEvent;
import org.group3.tutorlink.features.tracking.mapper.UserEventMapper;
import org.group3.tutorlink.features.tracking.repo.UserEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final UserEventRepository userEventRepository;
    private final UserEventMapper userEventMapper;
    private final AppUtil appUtil;

    /**
     * Lưu một lô event từ FE.
     * <b>userId</b> null nếu chưa đăng nhập
     *
     * @param request request chứa các event
     * @return số lượng event đã được lưu
     */
    @Transactional
    public int trackEvents(TrackEventsRequest request) {
        Instant receivedAt = Instant.now();

        UUID userId = appUtil.userIdFromAuthenticationOrNull();
        List<UserEvent> entities = request.getEvents().stream()
                .map(item -> userEventMapper.toUserEvent(item, request, userId, receivedAt, appUtil.generateUUID()))
                .peek(e -> {
                    // Đồng hồ trình duyệt có thể lệch -> không cho occurredAt vượt quá thời điểm server nhận
                    if (e.getOccurredAt().isAfter(receivedAt)) {
                        e.setOccurredAt(receivedAt);
                    }
                })
                .toList();

        if (entities.isEmpty()) {
            return 0;
        }
        return userEventRepository.saveAll(entities).size();
    }
}