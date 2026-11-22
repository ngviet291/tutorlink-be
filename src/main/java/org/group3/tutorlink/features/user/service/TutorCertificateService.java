package org.group3.tutorlink.features.user.service;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.dto.response.UploadMediaResponse;
import org.group3.tutorlink.common.enums.MediaType;
import org.group3.tutorlink.common.exception.AppException;
import org.group3.tutorlink.common.service.MediaStorageService;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.auth.event.TutorCertificatesSubmittedEvent;
import org.group3.tutorlink.features.auth.exception.UserNotFoundException;
import org.group3.tutorlink.features.auth.service.AuthenticationService;
import org.group3.tutorlink.features.notification.enums.NotificationMessage;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.notification.service.NotificationHelper;
import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.subject.mapper.CertificateMapper;
import org.group3.tutorlink.features.subject.repository.CertificateRepository;
import org.group3.tutorlink.features.user.dto.response.CertificateResponse;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.exception.UserErrorCode;
import org.group3.tutorlink.features.user.mapper.CertificateResponseMapper;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
public class TutorCertificateService {

    private final TutorRepository tutorRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;
    private final CertificateResponseMapper certificateResponseMapper;
    private final MediaStorageService mediaStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final AppUtil appUtil;
    private final NotificationHelper notificationHelper;
    private final int maxPerTutor;
    private final String defaultCertificateName;

    public TutorCertificateService(
            TutorRepository tutorRepository,
            CertificateRepository certificateRepository,
            CertificateMapper certificateMapper,
            CertificateResponseMapper certificateResponseMapper,
            MediaStorageService mediaStorageService,
            ApplicationEventPublisher eventPublisher,
            AppUtil appUtil,
            NotificationHelper notificationHelper,
            @Value("${app.certificate.max-per-tutor}") int maxPerTutor,
            @Value("${app.certificate.default-name}") String defaultCertificateName) {
        this.tutorRepository = tutorRepository;
        this.certificateRepository = certificateRepository;
        this.certificateMapper = certificateMapper;
        this.certificateResponseMapper = certificateResponseMapper;
        this.mediaStorageService = mediaStorageService;
        this.eventPublisher = eventPublisher;
        this.appUtil = appUtil;
        this.notificationHelper = notificationHelper;
        this.maxPerTutor = maxPerTutor;
        this.defaultCertificateName = defaultCertificateName;
    }

    @PreAuthorize("hasAuthority('TUTOR')")
    @Transactional(readOnly = true)
    public List<CertificateResponse> getMyCertificates() {
        UUID tutorId = appUtil.userIdFromAuthentication();
        return certificateResponseMapper.toResponses(
                certificateRepository.findAllByTutor_IdOrderByCreatedAtDesc(tutorId));
    }

    @PreAuthorize("hasAuthority('TUTOR')")
    @Transactional
    public List<CertificateResponse> uploadCertificates(List<MultipartFile> files) {
        List<MultipartFile> images = Optional.ofNullable(files).orElse(List.of()).stream()
                .filter(file -> !file.isEmpty())
                .toList();
        if (images.isEmpty()) {
            throw new AppException(UserErrorCode.CERTIFICATE_FILE_REQUIRED);
        }

        Tutor tutor = tutorRepository.findById(appUtil.userIdFromAuthentication())
                .orElseThrow(UserNotFoundException::new);
        if (certificateRepository.countByTutor_Id(tutor.getId()) + images.size() > maxPerTutor) {
            throw new AppException(UserErrorCode.CERTIFICATE_LIMIT_EXCEEDED);
        }

        List<UploadMediaResponse> uploaded = mediaStorageService.uploads(
                images, AuthenticationService.TUTOR_CERTIFICATES + tutor.getId(), MediaType.IMAGE);
        deleteUploadedFilesIfRolledBack(uploaded);

        List<Certificate> certificates = certificateMapper.toCertificates(uploaded, tutor);

        for (int i = 0; i < certificates.size(); i++) {
            MultipartFile image = images.get(i);

            String filename = image.getOriginalFilename();
            String certificateName = extractCertificateName(filename);

            certificates.get(i).setName(certificateName);

            log.info(
                    "Certificate prepared before save: filename={}, name={}, fileUrl={}",
                    filename,
                    certificates.get(i).getName(),
                    certificates.get(i).getFileUrl()
            );
        }

        List<Certificate> saved = certificateRepository.saveAll(certificates);
        resubmitIfRejected(tutor);
        eventPublisher.publishEvent(TutorCertificatesSubmittedEvent.builder().tutorId(tutor.getId()).build());

        log.info("Tutor {} uploaded {} certificates", tutor.getId(), saved.size());
        return certificateResponseMapper.toResponses(saved);
    }

    @PreAuthorize("hasAuthority('TUTOR')")
    @Transactional
    public void deleteCertificate(UUID certificateId) {
        Certificate certificate = certificateRepository
                .findByIdAndTutor_Id(certificateId, appUtil.userIdFromAuthentication())
                .orElseThrow(() -> new AppException(UserErrorCode.CERTIFICATE_NOT_FOUND));

        String publicId = certificate.getCloudinaryPublicId();
        certificateRepository.delete(certificate);
        runAfterCommit(() -> mediaStorageService.delete(publicId));
    }

    private void resubmitIfRejected(Tutor tutor) {
        if (tutor.getVerificationStatus() == VerificationStatus.REJECTED) {
            tutor.setVerificationStatus(VerificationStatus.PENDING);
            tutor.setVerificationNote(null);
            notificationHelper.notifyAdmins(NotificationMessage.TUTOR_RESUBMITTED_ADMIN,
                    NotificationTarget.ADMIN_TUTORS, tutor.getFullName());
        }
    }

    private void deleteUploadedFilesIfRolledBack(List<UploadMediaResponse> uploaded) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    uploaded.forEach(file -> mediaStorageService.delete(file.getPublicId()));
                }
            }
        });
    }

    private void runAfterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private String extractCertificateName(String filename) {
        if (filename == null || filename.isBlank()) {
            return defaultCertificateName;
        }
        String name = filename.trim();
        int slashIndex = Math.max(
                name.lastIndexOf('/'),
                name.lastIndexOf('\\')
        );
        if (slashIndex >= 0) {
            name = name.substring(slashIndex + 1);
        }
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        return name.isBlank() ? defaultCertificateName : name;
    }
}