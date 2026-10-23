package org.group3.tutorlink.features.payment.repository;

import org.group3.tutorlink.features.payment.entity.RevenueReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RevenueReportRepository extends JpaRepository<RevenueReport, UUID> {
}
