package org.group3.tutorlink.features.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new fee configuration")
public class CreateFeeConfigRequest {

    @Schema(description = "Fee percentage (0.0 - 1.0)", example = "0.20", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Fee percentage is required")
    @DecimalMin(value = "0.0", message = "Fee percentage must be at least 0")
    @DecimalMax(value = "1.0", message = "Fee percentage must not exceed 1.0")
    private Double feePercentage;

    @Schema(description = "Fixed fee amount (VND)", example = "0.00")
    @DecimalMin(value = "0.0", message = "Fixed fee must be at least 0")
    private Double fixedFee;

    @Schema(description = "Effective start date (defaults to today if not provided)", example = "2024-01-01")
    @FutureOrPresent(message = "Effective date must be today or in the future")
    private LocalDate effectiveFrom;
}
