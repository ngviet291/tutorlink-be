package org.group3.tutorlink.features.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Matching fee configuration information")
public class FeeConfigResponse {

    @Schema(description = "Fee configuration ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Fee percentage (0.0 - 1.0)", example = "0.20")
    private double feePercentage;

    @Schema(description = "Fixed fee amount (VND)", example = "0.00")
    private double fixedFee;

    @Schema(description = "Effective start date", example = "2024-01-01")
    private LocalDate effectiveFrom;

    @Schema(description = "Effective end date (null if currently active)", example = "2024-12-31")
    private LocalDate effectiveTo;

    @Schema(description = "Admin who created this configuration")
    private UUID createdByAdminId;

    @Schema(description = "Creation timestamp")
    private java.time.Instant createdAt;
}
