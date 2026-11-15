package org.group3.tutorlink.features.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.service.MediaStorageService;
import org.group3.tutorlink.features.ai.dto.CertificateSnapshot;
import org.group3.tutorlink.features.ai.dto.TutorReviewSnapshot;
import org.group3.tutorlink.features.ai.service.CertificateReviewAiService;
import org.group3.tutorlink.features.auth.dto.CertificateCheck;
import org.group3.tutorlink.features.notification.enums.NotificationType;
import org.group3.tutorlink.features.notification.service.NotificationCreator;
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
    private static final String TITLE_PROFILE_APPROVED = "Hồ sơ gia sư đã được duyệt";
    private static final String CONTENT_PROFILE_APPROVED = "Chứng chỉ của bạn đã được xác thực, bạn có thể bắt đầu đăng ký nhận lớp.";
    private static final String TITLE_PROFILE_REJECTED = "Hồ sơ gia sư bị từ chối";
    private static final String TITLE_CERT_VERIFIED = "Chứng chỉ đã được xác thực";
    private static final String FORMAT_CERT_VERIFIED = "%d chứng chỉ của bạn đã được xác thực.";
    private static final String TITLE_CERT_INVALID = "Chứng chỉ không hợp lệ";
    private static final String FORMAT_CERT_INVALID = "%d chứng chỉ không hợp lệ: %s";
    private static final String TITLE_REVIEW_FAILED = "Chưa kiểm tra được chứng chỉ";
    private static final String CONTENT_REVIEW_FAILED = "Hệ thống chưa thể kiểm tra chứng chỉ lúc này. Chứng chỉ vẫn chờ duyệt và sẽ được kiểm tra lại ở lần tải lên tiếp theo.";
    private static final int MAX_CONTENT_LENGTH = 500;
    private final TutorRepository tutorRepository;
    private final MediaStorageService mediaStorageService;
    private final CertificateReviewAiService aiService;
    private final TransactionTemplate tx;
    private final double minConfidence;
    private final NotificationCreator notificationCreator;

    public TutorCertificateReviewService(
            TutorRepository tutorRepository,
            MediaStorageService mediaStorageService,
            CertificateReviewAiService aiService,
            TransactionTemplate tx,
            @Value("${app.verification.min-confidence}") double minConfidence, NotificationCreator notificationCreator) {
        this.tutorRepository = tutorRepository;
        this.mediaStorageService = mediaStorageService;
        this.aiService = aiService;
        this.tx = tx;
        this.minConfidence = minConfidence;
        this.notificationCreator = notificationCreator;
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
            notifyResult(tutorId, wasPending, hasVerified, accepted.size(), invalidIds.size(), reasons);
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

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
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
    private void notifyResult(UUID tutorId, boolean wasPending, boolean hasVerified,
                              int acceptedCount, int invalidCount, List<String> reasons) {
        if (acceptedCount > 0) {
            if (wasPending && hasVerified) {
                notify(tutorId, TITLE_PROFILE_APPROVED, CONTENT_PROFILE_APPROVED);
            } else {
                notify(tutorId, TITLE_CERT_VERIFIED, String.format(FORMAT_CERT_VERIFIED, acceptedCount));
            }
        }
        if (invalidCount > 0) {
            String title = (wasPending && !hasVerified) ? TITLE_PROFILE_REJECTED : TITLE_CERT_INVALID;
            notify(tutorId, title, String.format(FORMAT_CERT_INVALID, invalidCount, joinReasons(reasons)));
        }
    }

    private void notifyReviewFailed(UUID tutorId) {
        tx.executeWithoutResult(status -> notify(tutorId, TITLE_REVIEW_FAILED, CONTENT_REVIEW_FAILED));
    }

    private void notify(UUID tutorId, String title, String content) {
        String safe = content.length() > MAX_CONTENT_LENGTH ? content.substring(0, MAX_CONTENT_LENGTH) : content;
        notificationCreator.createNotification(tutorId, NotificationType.SYSTEM, title, safe);
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return (preferred != null && !preferred.isBlank()) ? preferred.trim() : fallback;
    }
}