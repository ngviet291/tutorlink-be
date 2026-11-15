package org.group3.tutorlink.features.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin cấu hình phí nhận lớp.")
public class FeeConfigResponse {

    @Schema(description = "Mã cấu hình phí.", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Tỉ lệ phí, từ 0 đến 1.", example = "0.20")
    private double feePercentage;

    @Schema(description = "Ngày bắt đầu hiệu lực.", example = "2024-01-01")
    private Instant effectiveFrom;

    @Schema(description = "Ngày kết thúc hiệu lực; null nếu chưa xác định ngày kết thúc.", example = "2024-12-31")
    private Instant effectiveTo;

    @Schema(description = "Quản trị viên tạo cấu hình.")
    private UUID createdByAdminId;

    @Schema(description = "Thời điểm tạo.")
    private java.time.Instant createdAt;
}
