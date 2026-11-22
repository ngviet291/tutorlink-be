package org.group3.tutorlink.features.auth.event;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TutorCertificatesSubmittedEvent {
    private UUID tutorId;
    // true khi gia sư cập nhật hồ sơ: chứng chỉ AI không duyệt được thì chuyển cho admin thay vì xóa
    private boolean escalateToAdmin;

}