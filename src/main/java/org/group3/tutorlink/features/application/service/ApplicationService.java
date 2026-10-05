/*
 * @ (#) ApplicationService,java       1.0    29/09/2026
 *
 * Copyright (c) 2026 IUH. All righta reserved.
 */

package org.group3.tutorlink.features.application.service;

/*
 * @description:
 * @author: Ho Thi Kim Xuyen
 * @version:     1.0
 * @date: 29/09/2026 00
 */
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.features.application.dto.request.CreateApplicationRequest;
import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ApplicationService {

    ApplicationResponse createApplication(
            CreateApplicationRequest request
    );

    ApplicationResponse getApplicationById(
            UUID applicationId
    );

    CursorResponse<ApplicationResponse> getMyApplications(
            UUID cursor,
            int limit
    );

    CursorResponse<ApplicationResponse> getApplications(
            ApplicationStatus status,
            UUID cursor,
            int limit
    );

    ApplicationResponse selectApplication(
            UUID applicationId
    );

    ApplicationResponse confirmApplication(
            UUID applicationId
    );

    ApplicationResponse cancelApplication(
            UUID applicationId
    );
}