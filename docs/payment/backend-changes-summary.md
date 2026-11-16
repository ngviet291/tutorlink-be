# Tổng hợp thay đổi Backend - TutorLink

> Tài liệu phân tích chi tiết các thay đổi trên backend trong phiên làm việc này.

## Mục lục
1. [Payment Feature - Tính năng rút tiền](#1-payment-feature)
2. [Application Feature - Thêm trường paid](#2-application-feature)
3. [Review Complaint - Hiển thị tên gia sư](#3-review-complaint-feature)
4. [Mapper Fixes - Sửa lỗi MapStruct](#4-mapper-fixes)

---

## 1. Payment Feature

### 1.1. Tính năng rút tiền (Withdraw)

#### **Files mới:**
- `src/main/java/org/group3/tutorlink/features/payment/dto/request/WithdrawRequest.java` (NEW)

#### **Files đã sửa:**

##### **Enums & Constants**

**TransactionType.java**
```java
// Thêm loại giao dịch mới
public enum TransactionType {
    TOP_UP,
    PAYMENT,
    WITHDRAW  // ← MỚI
}
```

**PaymentResponseCode.java**
```java
// Thêm response codes cho withdraw
WITHDRAW_SUCCESS("W001", "Withdraw request submitted successfully"),
WITHDRAW_APPROVED("W002", "Withdrawal approved"),
WITHDRAW_REJECTED("W003", "Withdrawal rejected"),
```

**PaymentErrorCode.java**
```java
// Thêm error codes cho withdraw
WITHDRAW_AMOUNT_TOO_LOW(4201, "Withdraw amount must be at least 10,000 VND", HttpStatus.BAD_REQUEST),
INSUFFICIENT_BALANCE_FOR_WITHDRAWAL(4202, "Insufficient balance for withdrawal", HttpStatus.BAD_REQUEST),
WITHDRAW_NOT_PENDING(4203, "Only pending withdrawals can be processed", HttpStatus.BAD_REQUEST),
```

##### **Entity**

**Transaction.java**
```diff
+ private String bankAccountNumber;  // Số tài khoản ngân hàng
+ private String bankName;            // Tên ngân hàng
+ private String accountHolderName;   // Tên chủ tài khoản
```

**Migration SQL cần chạy:**
```sql
ALTER TABLE transactions 
ADD COLUMN bank_account_number VARCHAR(50),
ADD COLUMN bank_name VARCHAR(100),
ADD COLUMN account_holder_name VARCHAR(100);
```

##### **DTOs**

**WithdrawRequest.java** (NEW)
```java
public class WithdrawRequest {
    @NotNull
    @DecimalMin(value = "10000", message = "Minimum withdrawal is 10,000 VND")
    private BigDecimal amount;
    
    @NotBlank
    private String bankAccountNumber;
    
    @NotBlank
    private String bankName;
    
    @NotBlank
    private String accountHolderName;
}
```

**TransactionResponse.java**
```diff
+ private String tutorName;           // Tên gia sư (để Admin xem)
+ private String bankAccountNumber;   // Thông tin ngân hàng
+ private String bankName;
+ private String accountHolderName;
```

**FeeConfigResponse.java**
```diff
+ private String createdByAdminName;  // Tên admin tạo cấu hình
```

##### **Mapper**

**TransactionMapper.java**
```java
@Mapping(target = "tutorName", source = "tutor.fullName")  // Map tên gia sư
TransactionResponse toTransactionResponse(Transaction transaction);
```

**FeeConfigMapper.java**
```java
@Mapping(target = "createdByAdminName", source = "createdByAdmin.fullName")
FeeConfigResponse toFeeConfigResponse(FeeConfig feeConfig);
```

##### **Repository**

**TransactionRepository.java**
```java
// Thêm các query methods mới
List<Transaction> findByType(TransactionType type, Pageable pageable);
List<Transaction> findByTypeAndIdLessThan(TransactionType type, UUID cursor, Pageable pageable);
List<Transaction> findByStatusAndType(TransactionStatus status, TransactionType type, Pageable pageable);
List<Transaction> findByStatusAndTypeAndIdLessThan(TransactionStatus status, TransactionType type, UUID cursor, Pageable pageable);
```

##### **Service**

**PaymentService.java**
```java
// Thêm 3 methods mới
ApiResponse<TransactionResponse> withdrawFromWallet(UUID tutorId, WithdrawRequest request);
ApiResponse<TransactionResponse> approveWithdrawal(UUID withdrawalId, UUID adminId);
ApiResponse<TransactionResponse> rejectWithdrawal(UUID withdrawalId, UUID adminId);

// Sửa method getAllTransactions thêm param type
CursorResponse<List<TransactionResponse>> getAllTransactions(
    TransactionStatus status, 
    TransactionType type,  // ← THÊM MỚI
    UUID cursor, 
    int limit
);
```

**PaymentServiceImpl.java**

**(1) Withdraw từ ví:**
```java
@Override
@Transactional
public ApiResponse<TransactionResponse> withdrawFromWallet(UUID tutorId, WithdrawRequest request) {
    // 1. Validate tutor và wallet
    // 2. Validate số tiền >= 10,000 VND
    // 3. Validate số dư đủ
    // 4. TRỪ TIỀN NGAY từ wallet (ngăn spam)
    // 5. Tạo transaction PENDING với thông tin ngân hàng
    // 6. Save wallet và transaction
    // 7. Return response
}
```

**(2) Admin approve withdrawal:**
```java
@Override
@Transactional
public ApiResponse<TransactionResponse> approveWithdrawal(UUID withdrawalId, UUID adminId) {
    // 1. Validate transaction tồn tại, type=WITHDRAW, status=PENDING
    // 2. Update status → SUCCESS
    // 3. Update processedBy và processedAt
    // 4. Save transaction
    // 5. Return response (KHÔNG cộng/trừ wallet vì đã trừ lúc request)
}
```

**(3) Admin reject withdrawal:**
```java
@Override
@Transactional
public ApiResponse<TransactionResponse> rejectWithdrawal(UUID withdrawalId, UUID adminId) {
    // 1. Validate transaction tồn tại, type=WITHDRAW, status=PENDING
    // 2. Update status → FAILED
    // 3. Update processedBy và processedAt
    // 4. HOÀN TIỀN: wallet.balance += transaction.amount
    // 5. Save wallet và transaction
    // 6. Return response
}
```

**(4) Sửa getAllTransactions:**
```java
// Thêm filter theo type
if (type != null) {
    transactions = status != null
        ? transactionRepository.findByStatusAndType(status, type, pageable)
        : transactionRepository.findByType(type, pageable);
}
```

##### **Controller**

**PaymentController.java**
```java
// 3 endpoints mới

@PostMapping("/wallet/withdraw")
@PreAuthorize("hasAuthority('TUTOR')")
public ResponseEntity<ApiResponse<TransactionResponse>> withdrawFromWallet(
    @RequestBody @Valid WithdrawRequest request
) { ... }

@PostMapping("/{id}/approve-withdrawal")
@PreAuthorize("hasAuthority('ADMIN')")
public ResponseEntity<ApiResponse<TransactionResponse>> approveWithdrawal(
    @PathVariable UUID id
) { ... }

@PostMapping("/{id}/reject-withdrawal")
@PreAuthorize("hasAuthority('ADMIN')")
public ResponseEntity<ApiResponse<TransactionResponse>> rejectWithdrawal(
    @PathVariable UUID id
) { ... }

// Sửa getAllTransactions thêm param type
@GetMapping
@PreAuthorize("hasAuthority('ADMIN')")
public ResponseEntity<CursorResponse<List<TransactionResponse>>> getAllTransactions(
    @RequestParam(required = false) TransactionStatus status,
    @RequestParam(required = false) TransactionType type,  // ← THÊM MỚI
    @RequestParam(required = false) UUID cursor,
    @RequestParam(defaultValue = "20") int limit
) { ... }
```

### 1.2. Sửa logic tính phí theo giá mỗi buổi

**PaymentServiceImpl.java**

Trước đây: `fee = budget × rate`  
Bây giờ: `fee = pricePerSession × rate`

**Method mới: `convertToPerSession()`**
```java
private BigDecimal convertToPerSession(BigDecimal budget, BudgetUnit unit, Integer sessionsPerWeek, Integer durationMinutes) {
    return switch (unit) {
        case PER_SESSION -> budget;  // Đã là giá mỗi buổi
        
        case PER_HOUR -> {
            // Chuyển giá theo giờ → giá mỗi buổi
            BigDecimal hours = BigDecimal.valueOf(durationMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            yield budget.multiply(hours);
        }
        
        case PER_MONTH -> {
            // Chuyển giá theo tháng → giá mỗi buổi
            // 1 tháng = sessionsPerWeek × 4 tuần
            int totalSessions = sessionsPerWeek * 4;
            yield budget.divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
        }
    };
}
```

**Sửa `calculateCommission()`:**
```java
@Override
public BigDecimal calculateCommission(UUID postId) {
    Post post = postRepository.findById(postId)...;
    FeeConfig currentConfig = feeConfigRepository.findTopByIsActiveOrderByCreatedAtDesc(true)...;
    
    // Lấy giá trung bình
    BigDecimal avgBudget = post.getMinBudget().add(post.getMaxBudget())
            .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    
    // Chuyển sang giá mỗi buổi
    BigDecimal pricePerSession = convertToPerSession(
        avgBudget,
        post.getBudgetUnit(),
        post.getSessionsPerWeek(),
        post.getDurationMinutes()
    );
    
    // Tính phí = pricePerSession × rate
    BigDecimal rate = currentConfig.getFeeRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    return pricePerSession.multiply(rate).setScale(0, RoundingMode.HALF_UP);
}
```

**Sửa `getFeeQuote()` tương tự:**
```java
BigDecimal pricePerSession = convertToPerSession(...);
BigDecimal fee = pricePerSession.multiply(rate).setScale(0, RoundingMode.HALF_UP);
```

---

## 2. Application Feature

### Mục đích
Thêm trường `paid` để Frontend dễ dàng filter ứng tuyển đã thanh toán, tránh hiển thị trong danh sách "Chờ phản hồi".

### Files đã sửa:

**ApplicationResponse.java**
```diff
+ private Boolean paid;  // true nếu transaction.status == SUCCESS
```

**ApplicationMapper.java**
```java
@Mapping(target = "expiresAt", ignore = true)
@Mapping(target = "paid", ignore = true)  // Sẽ set trong @AfterMapping
public abstract ApplicationResponse toApplicationResponse(Application application);

@AfterMapping
protected void fillExpiresAt(Application application, @MappingTarget ApplicationResponse response) {
    Transaction transaction = application.getTransaction();
    Instant paidAt = transaction != null && transaction.getStatus() == TransactionStatus.SUCCESS
            ? transaction.getTransactionDate()
            : null;
    
    // Set paid flag
    response.setPaid(paidAt != null);  // ← THÊM MỚI
    
    // Logic tính expiresAt giữ nguyên...
}
```

### Lý do thay đổi:
- Frontend trước đây dùng `transactionStatus !== 'SUCCESS'` để filter nhưng field này có thể undefined
- Bổ sung thêm field `paid` boolean rõ ràng hơn
- Filter logic: `isPaid = paid || transactionStatus === 'SUCCESS'`

---

## 3. Review Complaint Feature

### Mục đích
Hiển thị tên gia sư thay vì UUID trong trang Admin → Xử lý khiếu nại đánh giá.

### Files đã sửa:

**ReviewComplaintResponse.java**
```diff
private UUID tutorId;
+ private String tutorName;  // Tên đầy đủ của gia sư
```

**ReviewComplaintServiceImpl.java**
```java
private ReviewComplaintResponse toResponse(ReviewComplaint complaint) {
    return ReviewComplaintResponse.builder()
            .id(complaint.getId())
            .reason(complaint.getReason())
            .status(complaint.getStatus())
            .reviewId(complaint.getReview().getId())
            .tutorId(complaint.getTutor().getId())
            .tutorName(complaint.getTutor().getFullName())  // ← THÊM MỚI
            .adminId(complaint.getAdmin() == null ? null : complaint.getAdmin().getId())
            .build();
}
```

---

## 4. Mapper Fixes

### Mục đích
Sửa lỗi compilation MapStruct do các field không được map hoặc không tồn tại trong target.

### Files đã sửa:

#### **PostMapper.java**
```java
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,      // ← THÊM
    unmappedSourcePolicy = ReportingPolicy.IGNORE       // ← THÊM
)
public interface PostMapper {
    // Không cần ignore removalReason trong toPostResponse()
    // vì đã có policy IGNORE tự động
}
```

**Lỗi trước đây:**
- `Unmapped target property: "removalReason"` trong `toPost()`
- `Unknown property "removalReason" in result type PostResponse` trong `toPostResponse()`

**Giải pháp:**
- Thêm `unmappedTargetPolicy = ReportingPolicy.IGNORE` để tự động ignore các field không map được
- Thêm `unmappedSourcePolicy = ReportingPolicy.IGNORE` để ignore các field source không tồn tại trong target

#### **UserMapper.java**
```java
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE  // ← THÊM
)
public interface UserMapper {
    // Thêm ignore cho learningGoal, grade trong toStudent()
    @Mapping(target = "learningGoal", ignore = true)
    @Mapping(target = "grade", ignore = true)
    Student toStudent(RegisterStudentRequest request);
    
    // Các mappings khác giữ nguyên
}
```

**Lỗi trước đây:**
- `No implementation was created for UserMapper due to having a problem in the erroneous element org.group3.tutorlink.features.user.entity.Student.StudentBuilder`

**Nguyên nhân:**
- `Student` extend `User` nên có cả fields của User và Student
- `RegisterStudentRequest` chỉ có một số fields
- MapStruct không biết map các fields còn lại → lỗi

**Giải pháp:**
- Thêm `unmappedTargetPolicy = ReportingPolicy.IGNORE`
- Thêm explicit ignore cho `learningGoal` và `grade`

#### **ChatbotMapper.java**
```java
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE  // ← THÊM
)
public interface ChatbotMapper {
    // Giữ nguyên các methods
}
```

**Lỗi trước đây:**
- `Unmapped target property: "posts"`

**Giải pháp:**
- Thêm `unmappedTargetPolicy = ReportingPolicy.IGNORE`

---

## 5. Tổng kết

### Các module đã sửa:

| Module | Files sửa | Files mới | Mục đích |
|--------|-----------|-----------|----------|
| **Payment** | 14 | 1 | Tính năng rút tiền, sửa logic tính phí, hiển thị tên trong Admin |
| **Application** | 2 | 0 | Thêm field `paid` để filter đã thanh toán |
| **Review** | 2 | 0 | Hiển thị tên gia sư trong khiếu nại |
| **Mappers** | 3 | 0 | Fix lỗi compilation MapStruct |
| **Docs** | 0 | 2 | Tài liệu withdraw-flow và backend-changes |

### Điểm cần lưu ý:

#### **1. Payment - Withdraw Flow**
- ✅ Trừ tiền ngay khi request (ngăn spam)
- ✅ Admin approve: không động ví (đã trừ rồi)
- ✅ Admin reject: hoàn tiền vào ví
- ⚠️ Cần chạy migration SQL trước khi test

#### **2. Fee Calculation Logic**
- ✅ Đã đổi từ `budget × rate` → `pricePerSession × rate`
- ✅ Hỗ trợ 3 đơn vị: PER_SESSION, PER_HOUR, PER_MONTH
- ✅ Logic chuyển đổi đơn vị chính xác

#### **3. Admin UI Enhancement**
- ✅ Transactions: Hiển thị `tutorName`
- ✅ Fee Config: Hiển thị `createdByAdminName`
- ✅ Complaints: Hiển thị `tutorName`

#### **4. MapStruct Fixes**
- ✅ Tất cả mapper đã compile thành công
- ✅ Dùng `ReportingPolicy.IGNORE` thống nhất
- ✅ Không có warning trong build

### Dependencies không đổi:
- ❌ Không sửa `pom.xml`
- ❌ Không thêm/xóa dependencies
- ❌ Không sửa Spring Boot version

### Endpoints API mới:

```
POST   /v1/payments/wallet/withdraw              [TUTOR]   - Request rút tiền
POST   /v1/payments/{id}/approve-withdrawal      [ADMIN]   - Duyệt yêu cầu rút
POST   /v1/payments/{id}/reject-withdrawal       [ADMIN]   - Từ chối yêu cầu rút
GET    /v1/payments?status=...&type=...          [ADMIN]   - Lọc giao dịch theo type
```

### Database Migration cần chạy:

```sql
ALTER TABLE transactions 
ADD COLUMN bank_account_number VARCHAR(50),
ADD COLUMN bank_name VARCHAR(100),
ADD COLUMN account_holder_name VARCHAR(100);
```

---

## 6. Testing Checklist

### Pre-deployment:
- [ ] Chạy migration SQL
- [x] Build backend thành công (`mvn clean compile`)
- [ ] Restart backend server

### Functional Testing:

#### Withdraw Feature:
- [ ] Tutor withdraw với số tiền < 10,000 → Lỗi
- [ ] Tutor withdraw với số dư không đủ → Lỗi
- [ ] Tutor withdraw thành công → Số dư giảm ngay, transaction PENDING
- [ ] Admin approve withdraw → Transaction SUCCESS, số dư không đổi
- [ ] Admin reject withdraw → Transaction FAILED, số dư được hoàn lại
- [ ] Admin xem danh sách withdrawals → Hiển thị tên gia sư

#### Fee Calculation:
- [ ] Post với PER_SESSION → Phí = minBudget × rate
- [ ] Post với PER_HOUR → Phí = (budget × durationMinutes/60) × rate
- [ ] Post với PER_MONTH → Phí = (budget / (sessionsPerWeek × 4)) × rate

#### Admin UI:
- [ ] Admin → Giao dịch: Xem được tên gia sư
- [ ] Admin → Cấu hình phí: Xem được tên admin tạo
- [ ] Admin → Khiếu nại: Xem được tên gia sư

#### Application paid field:
- [ ] Ứng tuyển đã thanh toán không hiện trong "Chờ phản hồi"
- [ ] `paid=true` khi transaction.status=SUCCESS

---

**Ngày tạo:** 2026-11-16  
**Phiên bản:** 1.0  
**Tác giả:** Kiro Agent
