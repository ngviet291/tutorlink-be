/*
 * @ (#) ApplicationRepository,java       1.0    29/09/2026
 *
 * Copyright (c) 2026 IUH. All righta reserved.
 */

package org.group3.tutorlink.features.application.repository;

/*
 * @description:
 * @author: Ho Thi Kim Xuyen
 * @version:     1.0
 * @date: 29/09/2026 14:57
 */
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByPostIdAndTutorId(UUID postId, UUID tutorId);

    boolean existsByPostIdAndStudentId(UUID postId, UUID studentId);

    Page<Application> findByTutorId(UUID tutorId, Pageable pageable);

    Page<Application> findByStudentId(UUID studentId, Pageable pageable);

    Page<Application> findByApplicationStatus(
            ApplicationStatus applicationStatus,
            Pageable pageable
    );

    Page<Application> findAll(Pageable pageable);
}