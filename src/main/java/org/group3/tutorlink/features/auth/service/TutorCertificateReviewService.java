package org.group3.tutorlink.features.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.service.MediaStorageService;
import org.group3.tutorlink.features.ai.dto.CertificateSnapshot;
import org.group3.tutorlink.features.ai.dto.TutorReviewSnapshot;
import org.group3.tutorlink.features.ai.service.CertificateReviewAiService;
import org.group3.tutorlink.features.auth.dto.CertificateCheck;
import org.group3.tutorlink.features.notification.enums.NotificationMessage;
import org.group3.tutorlink.features.notification.enums.NotificationTarget;
import org.group3.tutorlink.features.notification.service.NotificationHelper;
import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TutorCertificateReviewService {
    // Giới hạn độ dài lý do đưa vào nội dung thông báo
    private static final int MAX_REASONS_LENGTH = 400;
    private final TutorRepository tutorRepository;
    private final MediaStorageService mediaStorageService;
    private final CertificateReviewAiService aiService;
    private final TransactionTemplate tx;
    private final double minConfidence;
    private final NotificationHelper notificationHelper;

    public TutorCertificateReviewService(
            TutorRepository tutorRepository,
            MediaStorageService mediaStorageService,
            CertificateReviewAiService aiService,
            TransactionTemplate tx,
            @Value("${app.verification.min-confidence}") double minConfidence,
            NotificationHelper notificationHelper) {
        this.tutorRepository = tutorRepository;
        this.mediaStorageService = mediaStorageService;
        this.aiService = aiService;
        this.tx = tx;
        this.minConfidence = minConfidence;
        this.notificationHelper = notificationHelper;
    }

    public void review(UUID tutorId) {
        // 1) Đọc nhanh trong transaction ngắn, không giữ transaction lúc gọi AI
        TutorReviewSnapshot snapshot = loadSnapshot(tutorId);
        if (snapshot == null || snapshot.getCertificates().isEmpty()) {
            return;
        }

        // 2) Gọi AI ngoài transaction
        Map<UUID, CertificateCheck> accepted = new HashMap<>();
        Set<UUID> invalidIds = new HashSet<>();
        List<String> reasons = new ArrayList<>();

        for (CertificateSnapshot cert : snapshot.getCertificates()) {
            CertificateCheck check;
            try {
                check = aiService.check(snapshot.getFullName(), cert.getFileUrl());
                log.info("Certificate AI check result for tutor {}: cert={} valid={} confidence={} reason={}", tutorId, cert.getId(), check.isValid(), check.getConfidence(), check.getReason());
            } catch (Exception e) {
                log.error("Certificate AI check failed, keep as is. tutor={}", tutorId, e);
                notifyReviewFailed(tutorId);
                return;
            }
            if (isAccepted(check)) {
                accepted.put(cert.getId(), check);
            } else {
                invalidIds.add(cert.getId());
                reasons.add(check.getReason());
            }
        }
        applyResult(tutorId, accepted, invalidIds, reasons);
        deleteRejectedFiles(snapshot, invalidIds);
    }

    private boolean isAccepted(CertificateCheck check) {
        return check.isValid() && check.getConfidence() >= minConfidence;
    }

    private TutorReviewSnapshot loadSnapshot(UUID tutorId) {
        return tx.execute(status -> tutorRepository.findById(tutorId)
                .filter(t -> isReviewable(t.getVerificationStatus()))
                .map(t -> new TutorReviewSnapshot(
                        t.getFullName(),
                        t.getCertificates().stream()
                                .filter(c -> !c.isVerified())
                                .map(c -> new CertificateSnapshot(c.getId(), c.getFileUrl(), c.getCloudinaryPublicId()))
                                .toList()))
                .orElse(null));
    }

    private void applyResult(UUID tutorId, Map<UUID, CertificateCheck> accepted,
                             Set<UUID> invalidIds, List<String> reasons) {
        tx.executeWithoutResult(status -> {
            Tutor tutor = tutorRepository.findById(tutorId).orElseThrow();
            VerificationStatus current = tutor.getVerificationStatus();
            if (!isReviewable(current)) {
                return;
            }

            tutor.getCertificates().removeIf(c -> invalidIds.contains(c.getId()));
            tutor.getCertificates().forEach(c -> {
                CertificateCheck check = accepted.get(c.getId());
                if (check == null) {
                    return;
                }
                c.setName(firstNonBlank(check.getName(), c.getName()));
                c.setIssuedBy(firstNonBlank(check.getIssuedBy(), c.getIssuedBy()));
                c.setIssuedDate(parseDate(check.getIssuedDate()));
                c.setExpiryDate(parseDate(check.getExpiryDate()));
                c.setVerified(true);
            });

            boolean wasPending = current == VerificationStatus.PENDING;
            boolean hasVerified = tutor.getCertificates().stream().anyMatch(Certificate::isVerified);
            if (wasPending) {
                tutor.setVerificationStatus(hasVerified ? VerificationStatus.APPROVED : VerificationStatus.REJECTED);
                tutor.setVerificationNote(hasVerified ? null : joinReasons(reasons));
            }
            notifyResult(tutor, wasPending, hasVerified, accepted.size(), invalidIds.size(), reasons);
        });
    }

    private boolean isReviewable(VerificationStatus status) {
        return status == VerificationStatus.PENDING || status == VerificationStatus.APPROVED;
    }

    private void deleteRejectedFiles(TutorReviewSnapshot snapshot, Set<UUID> invalidIds) {
        snapshot.getCertificates().stream()
                .filter(c -> invalidIds.contains(c.getId()))
                .forEach(c -> mediaStorageService.delete(c.getCloudinaryPublicId()));
    }

    private String joinReasons(List<String> reasons) {
        return reasons.stream().filter(Objects::nonNull).collect(Collectors.joining("; "));
    }

    private String shortReasons(List<String> reasons) {
        String joined = joinReasons(reasons);
        return joined.length() > MAX_REASONS_LENGTH ? joined.substring(0, MAX_REASONS_LENGTH) : joined;
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            return null; // AI trả sai định dạng thì bỏ trống thay vì làm hỏng cả lượt duyệt
        }
    }

    private void notifyResult(Tutor tutor, boolean wasPending, boolean hasVerified,
                              int acceptedCount, int invalidCount, List<String> reasons) {
        if (acceptedCount > 0) {
            if (wasPending && hasVerified) {
                notificationHelper.notify(tutor, NotificationMessage.TUTOR_APPROVED_BY_AI,
                        NotificationTarget.TUTOR_PROFILE, null);
            } else {
                notificationHelper.notify(tutor, NotificationMessage.CERTIFICATE_VERIFIED,
                        NotificationTarget.TUTOR_PROFILE, null, acceptedCount);
            }
        }
        if (invalidCount > 0) {
            NotificationMessage message = (wasPending && !hasVerified)
                    ? NotificationMessage.TUTOR_REJECTED_BY_AI
                    : NotificationMessage.CERTIFICATE_INVALID;
            notificationHelper.notify(tutor, message, NotificationTarget.TUTOR_PROFILE, null,
                    invalidCount, shortReasons(reasons));
        }
    }

    private void notifyReviewFailed(UUID tutorId) {
        tx.executeWithoutResult(status -> {
            Tutor tutor = tutorRepository.findById(tutorId).orElse(null);
            if (tutor != null) {
                notificationHelper.notify(tutor, NotificationMessage.CERTIFICATE_REVIEW_FAILED,
                        NotificationTarget.TUTOR_PROFILE, null);
            }
            // Không tìm thấy tên thì dùng id để admin vẫn tra được
            String tutorName = tutor != null ? tutor.getFullName() : tutorId.toString();
            notificationHelper.notifyAdmins(NotificationMessage.TUTOR_REVIEW_FAILED_ADMIN,
                    NotificationTarget.ADMIN_TUTORS, tutorName);
        });
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return (preferred != null && !preferred.isBlank()) ? preferred.trim() : fallback;
    }
}