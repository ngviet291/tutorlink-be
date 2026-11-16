package org.group3.tutorlink.features.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.payment.enums.PaymentMethod;
import org.group3.tutorlink.features.payment.enums.TransactionStatus;
import org.group3.tutorlink.features.payment.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin giao dịch.")
public class TransactionResponse {

    @Schema(description = "Mã giao dịch.")
    private UUID id;

    @Schema(description = "Loại giao dịch.", example = "COMMISSION")
    private TransactionType type;

    @Schema(description = "Số tiền giao dịch, đơn vị VND.", example = "30000.00")
    private BigDecimal amount;

    @Schema(description = "Trạng thái giao dịch.", example = "SUCCESS")
    private TransactionStatus status;

    @Schema(description = "Phương thức thanh toán.", example = "WALLET")
    private PaymentMethod paymentMethod;

    @Schema(description = "Thời điểm giao dịch.")
    private Instant transactionDate;

    @Schema(description = "Mã gia sư sở hữu ví.")
    private UUID tutorId;
    
    @Schema(description = "Tên gia sư sở hữu ví.")
    private String tutorName;

    @Schema(description = "Mã đơn ứng tuyển, chỉ có ở giao dịch phí nhận lớp.")
    private UUID applicationId;

    @Schema(description = "Mã đơn của cổng thanh toán, chỉ dùng khi nạp ví qua cổng.")
    private String providerOrderId;

    @Schema(description = "Mã giao dịch của cổng, có sau khi thanh toán hoàn tất.")
    private String providerTransId;

    @Schema(description = "Liên kết tới trang thanh toán MoMo, trả về khi tạo yêu cầu nạp ví.")
    private String paymentLink;

    @Schema(description = "Nội dung dùng để tạo mã QR quét bằng ứng dụng MoMo.")
    private String qrCodeUrl;

    @Schema(description = "Liên kết mở trực tiếp ứng dụng MoMo.")
    private String deeplink;

    @Schema(description = "Số tài khoản ngân hàng, chỉ có ở giao dịch rút tiền.")
    private String bankAccountNumber;

    @Schema(description = "Tên ngân hàng, chỉ có ở giao dịch rút tiền.")
    private String bankName;

    @Schema(description = "Tên chủ tài khoản ngân hàng, chỉ có ở giao dịch rút tiền.")
    private String accountHolderName;
}
