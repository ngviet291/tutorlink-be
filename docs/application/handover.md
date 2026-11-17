# TutorLink - Tài liệu bàn giao: hạn xác nhận đơn ứng tuyển (expiry countdown)

Phạm vi: module `application` (BE), một thay đổi nhỏ ở module `post` (BE), và các thay đổi ở frontend.

## 1. Tổng quan

### 1.1. Bài toán

Sau khi chủ bài chọn một ứng viên (`ACCEPTED`), flow yêu cầu tutor trả phí hoa hồng rồi **applicant** (người được chọn, không phải chủ bài) xác nhận để chốt lớp (`CONFIRMED`). Hai bước này có thể bị bỏ quên, nên cần cơ chế tự hủy đơn quá hạn và hiển thị countdown cho người dùng.

### 1.2. Hai cửa sổ hết hạn

| Cửa sổ | Bắt đầu | Điều kiện hết hạn | Kết quả |
| --- | --- | --- | --- |
| **W1 — chờ thanh toán** | `selectApplication` (`ACCEPTED`, `Post=PENDING`) | Quá `payment-window` mà chưa có `transaction SUCCESS` | Đơn `CANCELLED`; `Post → PUBLISHED`; đơn `REJECTED → PENDING` |
| **W2 — chờ xác nhận** | `transaction SUCCESS` (đã trả, chờ applicant confirm) | Quá `confirm-window` mà applicant không confirm | Như W1 + **auto-refund tutor** qua `RefundService` |

- W1 neo vào `Application.selectedAt` (field mới, set lúc select).
- W2 neo vào `transaction.transactionDate` — không cần field mới.
- Trạng thái "đã trả chờ confirm" không có enum riêng: nhận biết bằng `applicationStatus=ACCEPTED` + `transactionStatus=SUCCESS`.

### 1.3. Ai xác nhận

- `FIND_TUTOR` (student đăng, tutor apply) → **tutor applicant** confirm.
- `FIND_STUDENT` (tutor đăng, student apply) → **student applicant** confirm.
- Tutor luôn là người trả phí (`payCommission`), cả 2 chiều.

## 2. Thay đổi BE — module application

| File | Thay đổi |
| --- | --- |
| `entity/Application.java` | Thêm `Instant selectedAt` (nullable) |
| `dto/response/ApplicationResponse.java` | Thêm `selectedAt`, `expiresAt` |
| `mapper/ApplicationMapper.java` | `@AfterMapping fillExpiresAt`: chỉ đơn `ACCEPTED` có hạn — đã trả thì `transactionDate + confirm-window`, chưa trả thì `selectedAt + payment-window` |
| `service/impl/ApplicationServiceImpl.java` | `selectApplication` set `selectedAt`; `confirmApplication` yêu cầu applicant + `transaction=SUCCESS`; `unselectApplication` mới; `cancelApplication` revert `Post → PUBLISHED` + restore `REJECTED → PENDING` |
| `repository/ApplicationRepository.java` | Thêm `findExpiredSelections` (W1), `findExpiredConfirmations` (W2), `existsParticipantByPostIdAndUserId` (cho post visibility) |
| `controller/ApplicationController.java` | Thêm `POST /v1/applications/{id}/unselect` |
| `scheduler/ApplicationExpiryScheduler.java` *(mới)* | `@Scheduled` quét W1/W2 định kỳ, revert đơn + refund W2 |
| `enums/ApplicationResponseCode.java` | Thêm `UNSELECT_SUCCESS` |

### 2.1. Cấu hình (`application.properties`)

```properties
application.expiry.payment-window-minutes=1440   # 24h (đã đổi từ 2 phút test, 15/11/2026)
application.expiry.confirm-window-minutes=1440   # 24h
application.expiry.interval-ms=10000             # chu kỳ quét scheduler
```

`ApplicationMapper` đọc cùng 2 khóa window — **phải khớp scheduler**, nếu không `expiresAt` FE hiển thị sẽ lệch với thời điểm đơn thật sự bị hủy.

### 2.2. Contract cho FE

- `ApplicationResponse.selectedAt`: thời điểm được chọn (null nếu chưa select/đã revert).
- `ApplicationResponse.expiresAt`: hạn cuối bước đang chờ — FE dùng để countdown. `null` với trạng thái khác `ACCEPTED`.
- `paid` (FE tự suy ra): `transactionStatus === "SUCCESS"`.
- Nút hiển thị: `ACCEPTED && paid && isApplicant` → "Xác nhận"; `ACCEPTED && !paid && isFeePayer` → "Thanh toán phí".

## 3. Thay đổi BE — module post (code người khác, đã comment [OLD]/[NEW])

**Bug**: `selectApplication` set `Post = PENDING` → public `GET /v1/posts/{id}` chỉ serve `PUBLISHED` → 404 → FE `enrichMany` không load được post → **đơn bị drop khỏi "Đơn đã ứng tuyển"** của applicant. Post `CLOSED` (sau confirm) cũng 404 → mất luôn đơn `CONFIRMED`/`FINISHED`.

**Fix**: `PostServiceImpl.getPostById` phân quyền theo status:

| Status | Ai xem được |
| --- | --- |
| `PUBLISHED` | Tất cả (kể cả anonymous) |
| `PENDING` / `CLOSED` | Author + applicant (ai có application trên bài, mọi status đơn) |
| `DRAFT` / `REMOVED` | 404 với non-author (author dùng `/posts/{id}/me`) |

Files sửa (code cũ được comment `[OLD]`, code mới `[NEW]` bên dưới):

- `post/repository/PostRepository.java`: `findByIdForVisibilityCheck` (mọi status trừ REMOVED)
- `post/service/impl/PostServiceImpl.java`: `getPostById` mới + `isPostParticipant`
- `application/repository/ApplicationRepository.java`: `existsParticipantByPostIdAndUserId`

## 4. Thay đổi FE

- `ExpiryCountdown` component: đếm ngược tới `expiresAt`, `onExpire` → refetch.
- `ApplyModal`: **form dùng chung cho cả student và tutor** — cùng nhập `proposedFee` + `message` (15/11/2026 chốt lại, không tách flow student).
- `MyApplicationsPage` / `OfferList`: render countdown + nút Xác nhận/Thanh toán theo `applicationStatus` + `paid` + role.
  - `OfferList` chỉ render cho `role === 'TUTOR'` (banner trả phí chỉ dành cho tutor).
  - Đơn `ACCEPTED && paid` phía tutor hiển thị "Đã thanh toán — chờ học viên xác nhận" + countdown "để học viên xác nhận".
  - Countdown label: `paid` → "học viên xác nhận"; `!paid` + fee payer → "thanh toán"; `!paid` + applicant → "xác nhận".
- `PostDetailPage`: nút "Hủy đăng ký" guard thêm `!isAuthor` — trước đây author bài `FIND_STUDENT` bị `myApplication` match nhầm phía `tutor` nên thấy nút hủy trên bài của chính mình.
- `i18n`: bổ sung `post.status.PENDING` ("Chờ duyệt"), `application.appliedAgo`/`rank`/`matchScore` (trước hiện raw key).
- `navItems.ts`: thêm "Đơn đã ứng tuyển" cho student.
- `router.tsx`: route `MY_APPLICATIONS` chuyển ra khỏi `RoleGuard TUTOR` (cả student lẫn tutor dùng chung).
- `DashboardPage.tsx`: stat "Đơn đã ứng tuyển" link đúng `MY_APPLICATIONS`.

## 5. Việc còn lại / lưu ý

- [x] `payment-window-minutes` đã đổi `1440` (24h) — 15/11/2026.
- [ ] W2 refund phụ thuộc `RefundService.refundCommission` (module payment) — cần xác nhận refund chạy đúng khi scheduler gọi.
- [ ] `payCommission` nên siết chỉ cho pay khi `ACCEPTED` (hiện có thể nhận `FINISHED` — việc của module payment).
- [ ] `existsByPostIdAndStudentId/TutorId` đang chặn apply lại kể cả đơn `CANCELLED/REJECTED` — cân nhắc nới chỉ tính đơn "sống".
- [ ] Post `PENDING`/`CLOSED` giờ participant xem được — nếu sau này muốn chặt hơn (chỉ applicant của đơn ACCEPTED trở lên) thì siết `existsParticipantByPostIdAndUserId` thêm filter status.

## 6. Cập nhật 17/11/2026 — paidAt, thông báo nhắc thanh toán, fix lỗi liên quan

### 6.1. `ApplicationResponse.paidAt` (BE + FE)

- `ApplicationResponse` thêm `Instant paidAt` — thời điểm thanh toán thành công.
- `ApplicationMapper`: `paidAt = transaction.transactionDate` khi `transaction.status = SUCCESS`, ngược lại `null`.
- FE `application.types.ts` / `application.dto.ts` / `application.service.ts build()` truyền `selectedAt` + `paidAt` xuống `ApplicationResponse`.

### 6.2. Thông báo nhắc thanh toán gửi đúng người (`ApplicationServiceImpl.selectApplication`)

Trước đây mọi đơn `ACCEPTED` đều bắn notification "vui lòng thanh toán" cho applicant — sai với `FIND_STUDENT` (student apply, tutor là chủ bài và là người trả phí).

| Post type | Người trả phí | Notification |
| --- | --- | --- |
| `FIND_TUTOR` (student đăng, tutor apply) | Tutor applicant | Applicant nhận "vui lòng thanh toán phí trong thời hạn quy định" |
| `FIND_STUDENT` (tutor đăng, student apply) | Tutor = post author | Post author nhận nhắc trả phí; student applicant nhận "chờ gia sư thanh toán" |

### 6.3. Fix 500 khi tutor apply (`ChatService.createChannel`)

- **Bug**: `selectApplication`/payment flow tạo chat channel; nếu channel đã tồn tại → `DataIntegrityViolationException` mark transaction rollback-only → commit ném `UnexpectedRollbackException` → **500**, thanh toán fail riêng với case tutor apply (`FIND_TUTOR`).
- **Fix**: `createChannel` check `chatChannelRepository.findBetween(sender, receiver)` trước — có rồi trả về luôn; giữ nguyên `try/catch DataIntegrityViolationException` làm dự phòng.

### 6.4. Fix 500 `/v1/notifications/unread-count` (lỗi ngắt quãng)

- **Bug**: derived query `countByIsReadFalseAndRecipient(UUID)` bind UUID vào association `User` → `IllegalArgumentException` → 500, gây "An unexpected error occurred" ngắt quãng trên FE.
- **Fix**: đổi thành `countByIsReadFalseAndRecipient_Id(UUID)` trong `NotificationRepository` + caller `NotificationServiceImpl`.

### 6.5. FE liên quan

- `ApplicantRow.tsx`: countdown label phân biệt người trả — `FIND_TUTOR` hiển thị "gia sư thanh toán" thay "thanh toán".
- `application.service.ts`: thêm `getApplicationById(id)` (dùng cho admin complaint modal).

## 7. Cách test nhanh

1. Student đăng bài `FIND_TUTOR` → tutor apply → student select.
2. Post chuyển `PENDING`; tutor thấy offer + countdown W1 (24h).
3. Tutor trả phí → `transactionStatus=SUCCESS`; đơn hiện countdown W2 (24h).
4. **Student KHÔNG confirm** (vì FIND_TUTOR thì tutor applicant confirm) — tutor bấm "Xác nhận" → `CONFIRMED`, post `CLOSED`.
5. Với `FIND_STUDENT`: student applicant mới là người confirm.
6. Để đơn quá hạn: chỉnh `payment-window-minutes`/`confirm-window-minutes` nhỏ, đợi scheduler chạy → đơn `CANCELLED`, post về `PUBLISHED`, W2 có refund.
