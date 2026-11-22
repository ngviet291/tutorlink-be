package org.group3.tutorlink.features.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Chứng chỉ AI không duyệt được, hiển thị cho admin duyệt tay. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCertificateResponse {
    private UUID id;
    private UUID tutorId;
    private String tutorName;
    private UUID subjectId;
    private String subjectName;
    private String name;
    private String fileUrl;
    // Lý do AI không tự duyệt được
    private String reviewNote;
    private Instant submittedAt;
}
