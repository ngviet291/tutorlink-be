package org.group3.tutorlink.features.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.post.enums.PostStatus;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PostStatisticsByStatus {

    private PostStatus status;

    private Long count;
}
