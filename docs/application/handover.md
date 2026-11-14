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
application.expiry.payment-window-minutes=2      # test; prod nên 1440 (24h)
application.expiry.confirm-window-minutes=1440   # 24h
application.expiry.interval-ms=3600000            # chu kỳ quét scheduler
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
- `MyApplicationsPage` / `OfferList`: render countdown + nút Xác nhận/Thanh toán theo `applicationStatus` + `paid` + role.
- `navItems.ts`: thêm "Đơn đã ứng tuyển" cho student.
- `router.tsx`: route `MY_APPLICATIONS` chuyển ra khỏi `RoleGuard TUTOR` (cả student lẫn tutor dùng chung).
- `DashboardPage.tsx`: stat "Đơn đã ứng tuyển" link đúng `MY_APPLICATIONS`.

## 5. Việc còn lại / lưu ý

- [ ] `payment-window-minutes` đang `2` cho test — **nhớ đổi 1440 trước khi demo/production**.
- [ ] W2 refund phụ thuộc `RefundService.refundCommission` (module payment) — cần xác nhận refund chạy đúng khi scheduler gọi.
- [ ] `payCommission` nên siết chỉ cho pay khi `ACCEPTED` (hiện có thể nhận `FINISHED` — việc của module payment).
- [ ] `existsByPostIdAndStudentId/TutorId` đang chặn apply lại kể cả đơn `CANCELLED/REJECTED` — cân nhắc nới chỉ tính đơn "sống".
- [ ] Post `PENDING`/`CLOSED` giờ participant xem được — nếu sau này muốn chặt hơn (chỉ applicant của đơn ACCEPTED trở lên) thì siết `existsParticipantByPostIdAndUserId` thêm filter status.

## 6. Cách test nhanh

1. Student đăng bài `FIND_TUTOR` → tutor apply → student select.
2. Post chuyển `PENDING`; tutor thấy offer + countdown W1 (2 phút nếu config test).
3. Tutor trả phí → `transactionStatus=SUCCESS`; đơn hiện countdown W2 (24h).
4. **Student KHÔNG confirm** (vì FIND_TUTOR thì tutor applicant confirm) — tutor bấm "Xác nhận" → `CONFIRMED`, post `CLOSED`.
5. Với `FIND_STUDENT`: student applicant mới là người confirm.
6. Để đơn quá hạn: chỉnh `payment-window-minutes`/`confirm-window-minutes` nhỏ, đợi scheduler chạy → đơn `CANCELLED`, post về `PUBLISHED`, W2 có refund.
