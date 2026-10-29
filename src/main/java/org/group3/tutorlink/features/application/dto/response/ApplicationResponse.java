/*
 * @ (#) ApplicationResponse,java       1.0    29/09/2026
 *
 * Copyright (c) 2026 IUH. All righta reserved.
 */

package org.group3.tutorlink.features.application.dto.response;

/*
 * @description:
 * @author: Ho Thi Kim Xuyen
 * @version:     1.0
 * @date: 29/09/2026 14:56
 */
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {

    private UUID id;

    private UUID postId;

    private UUID studentId;

    private UUID tutorId;

    private String applicationStatus;

    private String message;

    private Instant appliedAt;

    private UUID processedByAdminId;

    private UUID transactionId;
}