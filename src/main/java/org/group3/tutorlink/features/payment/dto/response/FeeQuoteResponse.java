package org.group3.tutorlink.features.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeQuoteResponse {
    private UUID applicationId;
    private BigDecimal baseAmount;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
}