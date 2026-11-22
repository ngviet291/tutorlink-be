package org.group3.tutorlink.features.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.service.MediaStorageService;
import org.group3.tutorlink.features.application.event.TutorProfileChangedEvent;
import org.group3.tutorlink.features.notification.enums.NotificationMessage;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.notification.service.NotificationHelper;
import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.repository.CertificateRepository;
import org.group3.tutorlink.features.user.dto.request.ApproveTutorRequest;
import org.group3.tutorlink.features.user.dto.response.AdminCertificateResponse;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.exception.UserErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

/** Admin duyệt tay các chứng chỉ mà AI không tự duyệt được khi gia sư cập nhật hồ sơ. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCertificateService {

    private final CertificateRepository certificateRepository;
    private final MediaStorageService mediaStorageService;
    private final NotificationHelper notificationHelper;
    private final ApplicationEventPublisher eventPublisher;

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(readOnly = true)
    public List<AdminCertificateResponse> getPending() {
        return certificateRepository.findAllByPendingAdminReviewTrueAndVerifiedFalseOrderByCreatedAtAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public void review(UUID certificateId, ApproveTutorRequest request) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new AppException(UserErrorCode.CERTIFICATE_NOT_FOUND));
        if (!certificate.isPendingAdminReview() || certificate.isVerified()) {
            throw new AppException(UserErrorCode.CERTIFICATE_NOT_PENDING_ADMIN);
        }
        Tutor tutor = certificate.getTutor();

        if (Boolean.TRUE.equals(request.getApproved())) {
            approve(certificate, tutor);
        } else {
            reject(certificate, tutor, request.getReason());
        }
        eventPublisher.publishEvent(TutorProfileChangedEvent.builder().tutorId(tutor.getId()).build());
    }

    private void approve(Certificate certificate, Tutor tutor) {
        certificate.setVerified(true);
        certificate.setPendingAdminReview(false);
        certificate.setReviewNote(null);
        notificationHelper.notify(tutor, NotificationMessage.CERTIFICATE_VERIFIED,
                NotificationTarget.TUTOR_PROFILE, null, 1);
    }

    private void reject(Certificate certificate, Tutor tutor, String reason) {
        Subject subject = certificate.getSubject();
        String publicId = certificate.getCloudinaryPublicId();

        // Cùng cách xóa với TutorCertificateService: gỡ khỏi collection để orphanRemoval xóa dòng
        tutor.getCertificates().remove(certificate);

        // Môn nào hết chứng chỉ thì bỏ khỏi hồ sơ
        if (subject != null && tutor.getCertificates().stream().noneMatch(c ->
                c.getSubject() != null && c.getSubject().getId().equals(subject.getId()))) {
            UUID subjectId = subject.getId();
            if (tutor.getSubjects() != null) {
                tutor.getSubjects().removeIf(s -> s.getId().equals(subjectId));
            }
            if (tutor.getSubjectProfiles() != null) {
                tutor.getSubjectProfiles().removeIf(p -> p.getSubject().getId().equals(subjectId));
            }
        }

        String shownReason = (reason == null || reason.isBlank()) ? "-" : reason.trim();
        notificationHelper.notify(tutor, NotificationMessage.CERTIFICATE_INVALID,
                NotificationTarget.TUTOR_PROFILE, null, 1, shownReason);
        if (publicId != null) {
            runAfterCommit(() -> mediaStorageService.delete(publicId));
        }
    }

    private AdminCertificateResponse toResponse(Certificate c) {
        Tutor tutor = c.getTutor();
        Subject subject = c.getSubject();
        return AdminCertificateResponse.builder()
                .id(c.getId())
                .tutorId(tutor.getId())
                .tutorName(tutor.getFullName())
                .subjectId(subject != null ? subject.getId() : null)
                .subjectName(subject != null ? subject.getName() : null)
                .name(c.getName())
                .fileUrl(c.getFileUrl())
                .reviewNote(c.getReviewNote())
                .submittedAt(c.getCreatedAt())
                .build();
    }

    private void runAfterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
