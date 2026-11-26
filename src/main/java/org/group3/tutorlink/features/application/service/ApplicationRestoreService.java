package org.group3.tutorlink.features.application.service;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.application.enums.ApplicationStatus;
import org.group3.tutorlink.features.application.repository.ApplicationRepository;
import org.group3.tutorlink.features.application.util.ApplicationParties;
import org.group3.tutorlink.features.notification.enums.NotificationMessage;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.notification.service.NotificationHelper;
import org.group3.tutorlink.features.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationRestoreService {

    private final ApplicationRepository applicationRepository;
    private final NotificationHelper notificationHelper;

    public void restoreRejectedApplications(Post post) {
        for (Application application : applicationRepository
                .findByPostIdAndApplicationStatus(
                        post.getId(),
                        ApplicationStatus.REJECTED,
                        Pageable.unpaged()
                )
                .getContent()) {

            application.setApplicationStatus(ApplicationStatus.PENDING);

            notificationHelper.notify(
                    ApplicationParties.applicantOf(application),
                    NotificationMessage.APPLICATION_REOPENED,
                    NotificationTarget.POST,
                    post.getId(),
                    post.getTitle()
            );
        }
    }
}