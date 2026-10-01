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
import org.group3.tutorlink.features.application.dto.request.CreateApplicationRequest;
import org.group3.tutorlink.features.application.dto.response.ApplicationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

public interface ApplicationService {

    ApplicationResponse createApplication(
            CreateApplicationRequest request
    );

    ApplicationResponse getApplicationById(UUID applicationId);

    Page<ApplicationResponse> getMyApplications(
            String role,
            Pageable pageable
    );
   @PreAuthorize("hasAuthority('ADMIN')")
    Page<ApplicationResponse> getApplications(
            String status,
            Pageable pageable
    );

    ApplicationResponse confirmApplication(UUID applicationId);

    ApplicationResponse finishApplication(UUID applicationId);

    ApplicationResponse approveApplication(UUID applicationId);

    ApplicationResponse rejectApplication(UUID applicationId);
}