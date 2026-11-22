package org.group3.tutorlink.features.application.matching;

import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.user.entity.AvailableSlot;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class AvailabilityChecker {

    /**
     * Gia sư có đủ chỗ trống trong tuần cho số buổi và thời lượng bài đăng yêu cầu không.
     * Thiếu dữ liệu thì coi là trung tính (trả true), không phạt.
     */
    public boolean hasCapacity(Tutor tutor, Post post) {
        List<AvailableSlot> slots = tutor.getAvailableSlots();
        Integer sessionsPerWeek = post.getSessionsPerWeek();
        Integer sessionMinutes = post.getDurationMinutes();

        if (slots == null || slots.isEmpty()
                || sessionsPerWeek == null
                || sessionMinutes == null || sessionMinutes <= 0) {
            return true;
        }

        long capacity = slots.stream()
                .mapToLong(slot -> sessionsFitting(slot, sessionMinutes))
                .sum();
        return capacity >= sessionsPerWeek;
    }

    // Một khung rảnh dài 3 giờ chứa được 2 buổi 90 phút
    private long sessionsFitting(AvailableSlot slot, int sessionMinutes) {
        if (slot.getStartTime() == null || slot.getEndTime() == null) {
            return 0;
        }
        long slotMinutes = Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes();
        return Math.max(slotMinutes, 0) / sessionMinutes;
    }
}