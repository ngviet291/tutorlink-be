# Luồng rút tiền từ ví TutorLink

## Tổng quan

Tính năng rút tiền cho phép Tutor chuyển số dư từ ví TutorLink về tài khoản ngân hàng cá nhân. Quy trình yêu cầu duyệt từ Admin để đảm bảo an toàn và tránh gian lận.

---

## Các bên tham gia

- **Tutor**: Người dùng có tiền trong ví, muốn rút về ngân hàng
- **Admin**: Người duyệt yêu cầu rút tiền và thực hiện chuyển khoản
- **System**: Quản lý trạng thái giao dịch và số dư ví

---

## Luồng nghiệp vụ

### 1. Tutor gửi yêu cầu rút tiền

**Endpoint:** `POST /v1/payments/wallet/withdraw`

**Request Body:**
```json
{
  "amount": 500000,
  "bankAccountNumber": "1234567890",
  "bankName": "Vietcombank",
  "accountHolderName": "NGUYEN VAN A"
}
```

**Validation:**
- `amount >= 10,000 VND` (số tiền tối thiểu)
- `amount <= wallet.balance` (số dư phải đủ)
- Các trường thông tin ngân hàng không được trống

**Xử lý:**
1. Kiểm tra số dư ví
2. **TRỪ VÍ NGAY LẬP TỨC** (tránh rút trùng khi spam request)
3. Tạo `Transaction`:
   - `type = WITHDRAW`
   - `status = PENDING`
   - `paymentMethod = WALLET`
   - Lưu thông tin ngân hàng
4. Trả về transaction với status PENDING

**Response:**
```json
{
  "code": "PAYMENT_201",
  "message": "Withdrawal request submitted successfully!",
  "data": {
    "id": "uuid",
    "type": "WITHDRAW",
    "amount": 500000,
    "status": "PENDING",
    "transactionDate": "2026-11-15T10:00:00Z"
  }
}
```

---

### 2. Admin xem danh sách yêu cầu rút tiền

**Endpoint:** `GET /v1/payments?status=PENDING`

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "type": "WITHDRAW",
      "amount": 500000,
      "status": "PENDING",
      "tutorId": "uuid",
      "bankAccountNumber": "1234567890",
      "bankName": "Vietcombank",
      "accountHolderName": "NGUYEN VAN A",
      "transactionDate": "2026-11-15T10:00:00Z"
    }
  ]
}
```

---

### 3a. Admin duyệt yêu cầu (SUCCESS)

**Endpoint:** `POST /v1/payments/{id}/approve-withdrawal`

**Xử lý:**
1. Kiểm tra transaction:
   - `type = WITHDRAW`
   - `status = PENDING`
2. **KHÔNG hoàn tiền** (tiền đã bị trừ từ bước 1)
3. Cập nhật `status = SUCCESS`
4. Admin thực hiện chuyển khoản thủ công hoặc qua API ngân hàng (ngoài hệ thống)

**Response:**
```json
{
  "code": "PAYMENT_200",
  "message": "Withdrawal approved successfully!",
  "data": {
    "id": "uuid",
    "status": "SUCCESS"
  }
}
```

---

### 3b. Admin từ chối yêu cầu (FAILED + REFUND)

**Endpoint:** `POST /v1/payments/{id}/reject-withdrawal`

**Xử lý:**
1. Kiểm tra transaction:
   - `type = WITHDRAW`
   - `status = PENDING`
2. **HOÀN LẠI VÍ**: `wallet.balance += amount`
3. Cập nhật `status = FAILED`

**Response:**
```json
{
  "code": "PAYMENT_200",
  "message": "Withdrawal rejected and refunded successfully!",
  "data": {
    "id": "uuid",
    "status": "FAILED"
  }
}
```

---

## Trạng thái Transaction

| Status | Ý nghĩa | Ví Tutor |
|--------|---------|----------|
| `PENDING` | Đang chờ Admin duyệt | Đã bị trừ |
| `SUCCESS` | Admin đã duyệt, tiền đã chuyển | Đã bị trừ |
| `FAILED` | Admin từ chối | Đã được hoàn lại |

---

## Database Schema

### Transaction entity

```java
@Entity
@Table(name = "transactions")
public class Transaction {
    private UUID id;
    private TransactionType type;           // WITHDRAW
    private TransactionStatus status;       // PENDING → SUCCESS/FAILED
    private BigDecimal amount;
    private PaymentMethod paymentMethod;    // WALLET
    private Instant transactionDate;
    
    // Thông tin ngân hàng (chỉ cho WITHDRAW)
    private String bankAccountNumber;
    private String bankName;
    private String accountHolderName;
    
    @ManyToOne
    private Tutor tutor;
}
```

---

## Security

### Authorization

- **Tutor**: Chỉ rút từ ví của mình
  - `@PreAuthorize("hasAuthority('TUTOR')")`
  - Kiểm tra `tutorId == currentUser.id`

- **Admin**: Duyệt/từ chối mọi yêu cầu
  - `@PreAuthorize("hasAuthority('ADMIN')")`

### Bảo mật giao dịch

1. **Trừ ví ngay**: Tránh spam request rút cùng lúc
2. **Lock wallet**: `findForUpdateByTutorId()` đảm bảo xử lý tuần tự
3. **Validate status**: Chỉ xử lý transaction PENDING
4. **Idempotent**: Không thể approve/reject 2 lần

---

## Error Codes

| Code | Message | HTTP Status |
|------|---------|-------------|
| `WITHDRAW_AMOUNT_TOO_LOW` | Minimum withdrawal amount is 10,000 VND! | 400 |
| `INSUFFICIENT_BALANCE_FOR_WITHDRAWAL` | Wallet balance is not enough for withdrawal! | 400 |
| `WITHDRAW_NOT_PENDING` | Only pending withdrawal can be processed! | 409 |
| `TRANSACTION_NOT_FOUND` | Transaction not found! | 404 |

---

## Frontend Flow

### Tutor UI

1. **WalletPage**: Hiển thị button "Rút tiền"
   - Disabled nếu balance < 10,000 VND
2. **WithdrawModal**: Form nhập thông tin
   - Số tiền rút
   - Số tài khoản
   - Tên ngân hàng
   - Tên chủ tài khoản
3. **TransactionTable**: Hiển thị trạng thái WITHDRAW
   - PENDING: "Đang chờ duyệt"
   - SUCCESS: "Đã chuyển khoản"
   - FAILED: "Bị từ chối"

### Admin UI

1. **Transaction List**: Filter `type=WITHDRAW` và `status=PENDING`
2. **Transaction Detail**: Hiển thị thông tin ngân hàng
3. **Action buttons**:
   - "Duyệt" → approve
   - "Từ chối" → reject

---

## API Endpoints Summary

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/v1/payments/wallet/withdraw` | TUTOR | Gửi yêu cầu rút tiền |
| GET | `/v1/payments?status=PENDING` | ADMIN | Xem danh sách yêu cầu |
| POST | `/v1/payments/{id}/approve-withdrawal` | ADMIN | Duyệt yêu cầu |
| POST | `/v1/payments/{id}/reject-withdrawal` | ADMIN | Từ chối yêu cầu |

---

## Testing

### Test Cases

1. **Rút tiền thành công**
   - Balance = 100,000 → Withdraw 50,000 → Balance = 50,000
   - Status = PENDING

2. **Rút tiền quá số dư**
   - Balance = 30,000 → Withdraw 50,000 → Error 400

3. **Rút tiền dưới ngưỡng**
   - Withdraw 5,000 → Error 400

4. **Admin duyệt**
   - Status PENDING → SUCCESS
   - Balance không thay đổi

5. **Admin từ chối**
   - Status PENDING → FAILED
   - Balance tăng trở lại

6. **Không thể approve/reject 2 lần**
   - Approve SUCCESS → Approve lại → Error 409

---

## Migration SQL

```sql
-- Thêm cột thông tin ngân hàng vào bảng transactions
ALTER TABLE transactions 
ADD COLUMN bank_account_number VARCHAR(50),
ADD COLUMN bank_name VARCHAR(100),
ADD COLUMN account_holder_name VARCHAR(100);

-- Index để query nhanh withdrawal PENDING
CREATE INDEX idx_transactions_type_status 
ON transactions(type, status);
```

---

## Notes

- Tiền được trừ ngay khi Tutor gửi yêu cầu (không phải khi Admin duyệt)
- Admin phải thực hiện chuyển khoản thủ công sau khi approve
- Nếu muốn tích hợp API ngân hàng tự động, cần thêm payment gateway
- Recommend: Gửi email/notification cho Tutor khi Admin xử lý xong
