package org.group3.tutorlink.features.notification.enums;

/**
 * Đích đến khi người dùng click vào thông báo. BE chỉ mô tả "đi đâu" (loại + id),
 * còn URL cụ thể do FE tự map để BE không phụ thuộc route của FE.
 */
public enum NotificationTarget {
    /** Chi tiết bài viết, targetId = postId */
    POST,
    /** Danh sách bài viết của tôi */
    MY_POSTS,
    /** Danh sách đơn ứng tuyển của tôi */
    MY_APPLICATIONS,
    /** Hồ sơ gia sư */
    TUTOR_PROFILE,
    /** Ví */
    WALLET,
    /** Chat, targetId = channelId (nếu có) */
    CHAT,
    /** Không điều hướng */
    NONE
}
