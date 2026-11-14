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
import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.subject.mapper.CertificateMapper;
import org.group3.tutorlink.features.subject.repository.CertificateRepository;
import org.group3.tutorlink.features.user.dto.response.CertificateResponse;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.exception.UserErrorCode;
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
    private final MediaStorageService mediaStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final AppUtil appUtil;
    private final int maxPerTutor;

    public TutorCertificateService(
            TutorRepository tutorRepository,
            CertificateRepository certificateRepository,
            CertificateMapper certificateMapper,
            MediaStorageService mediaStorageService,
            ApplicationEventPublisher eventPublisher,
            AppUtil appUtil,
            @Value("${app.certificate.max-per-tutor}") int maxPerTutor) {
        this.tutorRepository = tutorRepository;
        this.certificateRepository = certificateRepository;
        this.certificateMapper = certificateMapper;
        this.mediaStorageService = mediaStorageService;
        this.eventPublisher = eventPublisher;
        this.appUtil = appUtil;
        this.maxPerTutor = maxPerTutor;
    }

    @PreAuthorize("hasAuthority('TUTOR')")
    @Transactional(readOnly = true)
    public List<CertificateResponse> getMyCertificates() {
        UUID tutorId = appUtil.userIdFromAuthentication();
        return certificateRepository.findAllByTutor_IdOrderByCreatedAtDesc(tutorId).stream()
                .map(this::toResponse)
                .toList();
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

        List<Certificate> saved = certificateRepository.saveAll(certificateMapper.toCertificates(uploaded, tutor));
        resubmitIfRejected(tutor);
        eventPublisher.publishEvent(TutorCertificatesSubmittedEvent.builder().tutorId(tutor.getId()).build());

        log.info("Tutor {} uploaded {} certificates", tutor.getId(), saved.size());
        return saved.stream().map(this::toResponse).toList();
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

    private CertificateResponse toResponse(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .name(certificate.getName())
                .issuedBy(certificate.getIssuedBy())
                .issuedDate(certificate.getIssuedDate())
                .expiryDate(certificate.getExpiryDate())
                .fileUrl(certificate.getFileUrl())
                .verified(certificate.isVerified())
                .build();
    }
}