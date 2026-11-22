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
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.repository.CertificateRepository;
import org.group3.tutorlink.features.user.entity.Tutor;
import org.group3.tutorlink.features.user.enums.VerificationStatus;
import org.group3.tutorlink.features.user.repository.TutorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TutorCertificateReviewService {
    // Giới hạn độ dài lý do đưa vào nội dung thông báo
    private static final int MAX_REASONS_LENGTH = 400;
    private static final int MAX_NOTE_LENGTH = 1000;
    private static final String AI_UNAVAILABLE_NOTE = "Hệ thống AI chưa kiểm tra được chứng chỉ này";
    private static final String STUCK_NOTE = "Quá thời gian chờ mà hệ thống chưa kiểm tra xong chứng chỉ này";
    private static final String AI_UNSURE_NOTE = "AI không đủ độ tin cậy để tự duyệt chứng chỉ này";
    private final TutorRepository tutorRepository;
    private final MediaStorageService mediaStorageService;
    private final CertificateReviewAiService aiService;
    private final TransactionTemplate tx;
    private final double minConfidence;
    private final NotificationHelper notificationHelper;
    private final CertificateRepository certificateRepository;
    private final long aiTimeoutSeconds;
    private final long stuckMinutes;
    // Chạy AI trên luồng riêng để đặt được timeout cho từng chứng chỉ
    private final ExecutorService aiExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "certificate-ai");
        t.setDaemon(true);
        return t;
    });

    public TutorCertificateReviewService(
            TutorRepository tutorRepository,
            MediaStorageService mediaStorageService,
            CertificateReviewAiService aiService,
            TransactionTemplate tx,
            @Value("${app.verification.min-confidence}") double minConfidence,
            NotificationHelper notificationHelper,
            CertificateRepository certificateRepository,
            @Value("${app.verification.ai-timeout-seconds:60}") long aiTimeoutSeconds,
            @Value("${app.verification.stuck-minutes:10}") long stuckMinutes) {
        this.tutorRepository = tutorRepository;
        this.mediaStorageService = mediaStorageService;
        this.aiService = aiService;
        this.tx = tx;
        this.minConfidence = minConfidence;
        this.notificationHelper = notificationHelper;
        this.certificateRepository = certificateRepository;
        this.aiTimeoutSeconds = aiTimeoutSeconds;
        this.stuckMinutes = stuckMinutes;
    }

    public void review(UUID tutorId) {
        review(tutorId, false);
    }

    /**
     * @param escalateToAdmin true (gia sư cập nhật hồ sơ): chứng chỉ AI không duyệt được thì giữ lại
     *                        và chuyển admin duyệt tay, thay vì xóa. false (đăng ký): giữ hành vi cũ.
     */
    public void review(UUID tutorId, boolean escalateToAdmin) {
        // 1) Đọc nhanh trong transaction ngắn, không giữ transaction lúc gọi AI
        TutorReviewSnapshot snapshot = loadSnapshot(tutorId);
        if (snapshot == null || snapshot.getCertificates().isEmpty()) {
            return;
        }

        // 2) Gọi AI ngoài transaction
        Map<UUID, CertificateCheck> accepted = new HashMap<>();
        Set<UUID> invalidIds = new HashSet<>();
        Map<UUID, String> escalated = new LinkedHashMap<>();
        List<String> reasons = new ArrayList<>();

        for (CertificateSnapshot cert : snapshot.getCertificates()) {
            CertificateCheck check;
            try {
                check = checkWithTimeout(snapshot.getFullName(), cert.getFileUrl());
                log.info("Certificate AI check result for tutor {}: cert={} valid={} confidence={} reason={}", tutorId, cert.getId(), check.isValid(), check.getConfidence(), check.getReason());
            } catch (Exception e) {
                log.error("Certificate AI check failed. tutor={} escalate={}", tutorId, escalateToAdmin, e);
                if (escalateToAdmin) {
                    // AI lỗi thì cũng chuyển admin, không để chứng chỉ treo vô thời hạn
                    Set<UUID> ids = snapshot.getCertificates().stream()
                            .map(CertificateSnapshot::getId).collect(Collectors.toSet());
                    sendToAdmin(tutorId, ids, AI_UNAVAILABLE_NOTE);
                } else {
                    notifyReviewFailed(tutorId);
                }
                return;
            }
            if (isAccepted(check)) {
                accepted.put(cert.getId(), check);
            } else if (escalateToAdmin) {
                escalated.put(cert.getId(), notBlank(check.getReason(), AI_UNSURE_NOTE));
            } else {
                invalidIds.add(cert.getId());
                reasons.add(check.getReason());
            }
        }
        applyResult(tutorId, accepted, invalidIds, escalated, reasons);
        deleteRejectedFiles(snapshot, invalidIds);
    }

    /** Quá aiTimeoutSeconds mà AI chưa trả lời thì hủy và coi như AI lỗi. */
    private CertificateCheck checkWithTimeout(String fullName, String fileUrl) throws Exception {
        Future<CertificateCheck> future = aiExecutor.submit(() -> aiService.check(fullName, fileUrl));
        try {
            return future.get(aiTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    /**
     * Chứng chỉ chưa xác thực, chưa chờ admin mà quá stuckMinutes (server restart, luồng bị mất...)
     * thì chuyển admin. Chỉ xử lý gia sư còn PENDING/APPROVED.
     */
    public void sweepStuck() {
        Instant cutoff = Instant.now().minus(stuckMinutes, ChronoUnit.MINUTES);
        tx.executeWithoutResult(status -> {
            Map<UUID, List<Certificate>> byTutor = certificateRepository
                    .findAllByVerifiedFalseAndPendingAdminReviewFalseAndCreatedAtBefore(cutoff).stream()
                    .collect(Collectors.groupingBy(c -> c.getTutor().getId()));
            byTutor.values().forEach(certs -> {
                Tutor tutor = certs.get(0).getTutor();
                if (!isReviewable(tutor.getVerificationStatus())) {
                    return;
                }
                Map<UUID, String> notes = certs.stream()
                        .collect(Collectors.toMap(Certificate::getId, c -> STUCK_NOTE));
                notifyEscalated(tutor, markPendingAdmin(tutor, notes));
            });
        });
    }

    /** Đánh dấu chứng chỉ chờ admin duyệt (AI không dùng được), thông báo gia sư và admin. */
    private void sendToAdmin(UUID tutorId, Set<UUID> certIds, String note) {
        tx.executeWithoutResult(status -> {
            Tutor tutor = tutorRepository.findById(tutorId).orElse(null);
            if (tutor == null || !isReviewable(tutor.getVerificationStatus())) {
                return;
            }
            Map<UUID, String> notes = certIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> note));
            int count = markPendingAdmin(tutor, notes);
            notifyEscalated(tutor, count);
        });
    }

    private int markPendingAdmin(Tutor tutor, Map<UUID, String> notes) {
        int count = 0;
        for (Certificate c : tutor.getCertificates()) {
            String note = notes.get(c.getId());
            if (note != null && !c.isVerified()) {
                c.setPendingAdminReview(true);
                c.setReviewNote(truncate(note, MAX_NOTE_LENGTH));
                count++;
            }
        }
        return count;
    }

    private void notifyEscalated(Tutor tutor, int count) {
        if (count == 0) {
            return;
        }
        notificationHelper.notify(tutor, NotificationMessage.CERTIFICATE_SENT_TO_ADMIN,
                NotificationTarget.TUTOR_PROFILE, null, count);
        notificationHelper.notifyAdmins(NotificationMessage.CERTIFICATE_REVIEW_NEEDED_ADMIN,
                NotificationTarget.ADMIN_TUTORS, tutor.getFullName(), count);
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
                                .filter(c -> !c.isVerified() && !c.isPendingAdminReview())
                                .map(c -> new CertificateSnapshot(c.getId(), c.getFileUrl(), c.getCloudinaryPublicId()))
                                .toList()))
                .orElse(null));
    }

    private void applyResult(UUID tutorId, Map<UUID, CertificateCheck> accepted,
                             Set<UUID> invalidIds, Map<UUID, String> escalated, List<String> reasons) {
        tx.executeWithoutResult(status -> {
            Tutor tutor = tutorRepository.findById(tutorId).orElseThrow();
            VerificationStatus current = tutor.getVerificationStatus();
            if (!isReviewable(current)) {
                return;
            }

            // Các môn có chứng chỉ bị loại
            Set<UUID> affectedSubjects = tutor.getCertificates().stream()
                    .filter(c -> invalidIds.contains(c.getId()))
                    .map(Certificate::getSubject)
                    .filter(Objects::nonNull)
                    .map(Subject::getId)
                    .collect(Collectors.toSet());

            tutor.getCertificates().removeIf(c -> invalidIds.contains(c.getId()));

            // Môn nào mất hết chứng chỉ thì bỏ khỏi hồ sơ
            Set<UUID> stillCovered = tutor.getCertificates().stream()
                    .map(Certificate::getSubject)
                    .filter(Objects::nonNull)
                    .map(Subject::getId)
                    .collect(Collectors.toSet());
            if (tutor.getSubjects() != null) {
                tutor.getSubjects().removeIf(
                        s -> affectedSubjects.contains(s.getId()) && !stillCovered.contains(s.getId()));
            }

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
                c.setPendingAdminReview(false);
                c.setReviewNote(null);
            });
            int escalatedCount = markPendingAdmin(tutor, escalated);

            boolean wasPending = current == VerificationStatus.PENDING;
            boolean hasVerified = tutor.getCertificates().stream().anyMatch(Certificate::isVerified);
            if (wasPending) {
                if (hasVerified) {
                    tutor.setVerificationStatus(VerificationStatus.APPROVED);
                    tutor.setVerificationNote(null);
                } else if (escalatedCount > 0) {
                    // Còn chứng chỉ chờ admin: hồ sơ giữ PENDING để admin quyết định, không tự từ chối
                    tutor.setVerificationNote(null);
                } else {
                    tutor.setVerificationStatus(VerificationStatus.REJECTED);
                    tutor.setVerificationNote(joinReasons(reasons));
                }
            }
            notifyResult(tutor, wasPending, hasVerified, accepted.size(), invalidIds.size(), reasons);
            notifyEscalated(tutor, escalatedCount);
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

    private static String notBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
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