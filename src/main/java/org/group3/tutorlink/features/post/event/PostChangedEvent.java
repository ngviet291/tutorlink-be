package org.group3.tutorlink.features.post.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Nội dung hoặc trạng thái bài đăng vừa đổi, cần đồng bộ lại vector store. */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostChangedEvent {
    private UUID postId;
}