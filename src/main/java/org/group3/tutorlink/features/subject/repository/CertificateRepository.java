package org.group3.tutorlink.features.subject.repository;

import org.group3.tutorlink.features.subject.entity.Certificate;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findAllByTutor_IdOrderByCreatedAtDesc(UUID tutorId);
    long countByTutor_Id(UUID tutorId);
    Optional<Certificate> findByIdAndTutor_Id(UUID id, UUID tutorId);
    // Chứng chỉ AI không duyệt được, đang chờ admin (cũ nhất trước)
    List<Certificate> findAllByPendingAdminReviewTrueAndVerifiedFalseOrderByCreatedAtAsc();
    // Chứng chỉ bị kẹt: chưa được AI xử lý xong, cũng chưa chuyển admin
    List<Certificate> findAllByVerifiedFalseAndPendingAdminReviewFalseAndCreatedAtBefore(java.time.Instant cutoff);
    boolean existsByTutor_IdAndSubject_IdAndVerifiedTrue(UUID tutorId, UUID subjectId);
    // Chỉ gia sư đã được duyệt (đang cập nhật hồ sơ); hồ sơ đăng ký thì admin xem trong modal hồ sơ
    List<Certificate> findAllByPendingAdminReviewTrueAndVerifiedFalseAndTutor_VerificationStatusOrderByCreatedAtAsc(
            org.group3.tutorlink.features.user.enums.VerificationStatus status);
}
