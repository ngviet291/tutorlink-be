package org.group3.tutorlink.features.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.group3.tutorlink.common.service.MediaStorageService;
import org.group3.tutorlink.features.ai.dto.CertificateSnapshot;
import org.group3.tutorlink.features.ai.dto.TutorReviewSnapshot;
import org.group3.tutorlink.features.ai.service.CertificateReviewAiService;
import org.group3.tutorlink.features.auth.dto.CertificateCheck;
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

    private final TutorRepository tutorRepository;
    private final MediaStorageService mediaStorageService;
    private final CertificateReviewAiService aiService;
    private final TransactionTemplate tx;
    private final double minConfidence;

    public TutorCertificateReviewService(
            TutorRepository tutorRepository,
            MediaStorageService mediaStorageService,
            CertificateReviewAiService aiService,
            TransactionTemplate tx,
            @Value("${app.verification.min-confidence}") double minConfidence) {
        this.tutorRepository = tutorRepository;
        this.mediaStorageService = mediaStorageService;
        this.aiService = aiService;
        this.tx = tx;
        this.minConfidence = minConfidence;
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
                // Lỗi kỹ thuật không được coi là chứng chỉ không hợp lệ: giữ PENDING
                log.error("Certificate AI check failed, keep PENDING. tutor={}", tutorId, e);
                return;
            }

            if (isAccepted(check)) {
                accepted.put(cert.getId(), check);
            } else {
                invalidIds.add(cert.getId());
                reasons.add(check.getReason());
            }
        }

        // 3) Ghi kết quả, 4) dọn ảnh không hợp lệ trên Cloudinary
        applyResult(tutorId, accepted, invalidIds, reasons);
        deleteRejectedFiles(snapshot, invalidIds);
    }

    private boolean isAccepted(CertificateCheck check) {
        return check.isValid() && check.getConfidence() >= minConfidence;
    }

    private TutorReviewSnapshot loadSnapshot(UUID tutorId) {
        return tx.execute(status -> tutorRepository.findById(tutorId)
                .filter(t -> t.getVerificationStatus() == VerificationStatus.PENDING) // idempotent
                .map(t -> new TutorReviewSnapshot(
                        t.getFullName(),
                        t.getCertificates().stream()
                                .map(c -> new CertificateSnapshot(c.getId(), c.getFileUrl(), c.getCloudinaryPublicId()))
                                .toList()))
                .orElse(null));
    }

    private void applyResult(UUID tutorId, Map<UUID, CertificateCheck> accepted,
                             Set<UUID> invalidIds, List<String> reasons) {
        tx.executeWithoutResult(status -> {
            Tutor tutor = tutorRepository.findById(tutorId).orElseThrow();
            if (tutor.getVerificationStatus() != VerificationStatus.PENDING) {
                return;
            }

            tutor.getCertificates().removeIf(c -> invalidIds.contains(c.getId()));
            tutor.getCertificates().forEach(c -> {
                CertificateCheck check = accepted.get(c.getId());
                if (check == null) {
                    return;
                }
                c.setName(blankToNull(check.getName()));
                c.setIssuedBy(blankToNull(check.getIssuedBy()));
                c.setIssuedDate(parseDate(check.getIssuedDate()));
                c.setExpiryDate(parseDate(check.getExpiryDate()));
                c.setVerified(true);
            });

            boolean approved = !tutor.getCertificates().isEmpty();
            tutor.setVerificationStatus(approved ? VerificationStatus.APPROVED : VerificationStatus.REJECTED);
            tutor.setVerificationNote(approved ? null : joinReasons(reasons));
        });
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
}