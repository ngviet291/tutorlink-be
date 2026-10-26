package org.group3.tutorlink.features.review.repository;

import org.group3.tutorlink.features.review.entity.ReviewComplaint;
import org.group3.tutorlink.features.review.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewComplaintRepository extends JpaRepository<ReviewComplaint, UUID> {
    List<ReviewComplaint> findByStatus(ComplaintStatus status);
}
