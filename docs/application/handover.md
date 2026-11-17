# TutorLink - Tài liệu bàn giao: hạn xác nhận đơn ứng tuyển (expiry countdown)

Phạm vi: module `application` (BE), một thay đổi nhỏ ở module `post` (BE), module `payment` (hook thanh toán), và các thay đổi ở frontend.

## 1. Tổng quan

### 1.1. Bài toán

Sau khi chủ bài chọn một ứng viên (`ACCEPTED`), flow yêu cầu tutor trả phí hoa hồng rồi **applicant** (người được chọn, không phải chủ bài) xác nhận để chốt lớp (`CONFIRMED`). Hai bước này có thể bị bỏ quên, nên cần cơ chế tự hủy đơn quá hạn và hiển thị countdown cho người dùng.

### 1.2. Hai cửa sổ hết hạn

| Cửa sổ | Bắt đầu | Điều kiện hết hạn | Kết quả |
| --- | --- | --- | --- |
| **W1 — chờ thanh toán** | `selectApplication` (`ACCEPTED`, `Post=PENDING`) | Quá `payment-window` mà chưa có `transaction SUCCESS` | Đơn `CANCELLED`; `Post → PUBLISHED` (nếu đang PENDING); đơn `REJECTED → PENDING` |
| **W2 — chờ xác nhận** | `transaction SUCCESS` (đã trả, chờ applicant confirm) | Quá `confirm-window` mà applicant không confirm | Như W1 + **auto-refund tutor** qua `RefundService` |

- W1 neo vào `Application.selectedAt` (set lúc select).
- W2 neo vào `transaction.transactionDate`.
- Trạng thái "đã trả chờ confirm" không có enum riêng: nhận biết bằng `applicationStatus=ACCEPTED` + `transactionStatus=SUCCESS`.

### 1.3. Ai xác nhận

- `FIND_TUTOR` (student đăng, tutor apply) → **tutor applicant** confirm.
- `FIND_STUDENT` (tutor đăng, student apply) → **student applicant** confirm.
- Tutor luôn là người trả phí (`payCommission`), cả 2 chiều.

## 2. Kiến trúc expiry — Redis ZSET delay queue

Mỗi đơn có một "hẹn giờ" trong Redis: `member = applicationId` (UUID string), `score = epoch ms thời điểm hết hạn`. Hai key:

| Key | Ý nghĩa | Score |
| --- | --- | --- |
| `application:expiry:payment` | Đơn `ACCEPTED`, chờ thanh toán (W1) | `selectedAt + paymentWindow` |
| `application:expiry:confirm` | Đơn đã thanh toán, chờ xác nhận (W2) | `transactionDate + confirmWindow` |

- `ApplicationExpiryQueue.claimDue` dùng Lua script `ZRANGEBYSCORE` + `ZREM` atomic → nhiều instance không nhận trùng item. ZADD cùng member ghi đè score → đổi hạn là tự cập nhật.
- `ApplicationExpiryScheduler` poll Redis mỗi `interval-ms` (30s), **không `@Transactional`**; mỗi đơn xử lý trong transaction riêng qua `ApplicationExpiryService.expire` → lỗi 1 đơn không ảnh hưởng đơn khác; đơn lỗi được schedule lại sau `retry-delay-seconds`.
- **Redis chỉ là gợi ý**: `expire()` luôn re-check DB — status phải còn `ACCEPTED`, paid khớp stage, và deadline thật (`selectedAt`/`transactionDate` + window) phải đã qua. Reconciler chạy trùng vẫn an toàn.

### 2.1. Vòng đời lịch trong Redis

| Sự kiện | Hành động (đều sau commit, qua `AfterCommit`) |
| --- | --- |
| `selectApplication` → `ACCEPTED` | `schedule(PAYMENT, id, selectedAt + paymentWindow)` |
| `payCommission` → transaction `SUCCESS` | `cancel(PAYMENT)` + `schedule(CONFIRM, id, transactionDate + confirmWindow)` |
| `confirmApplication` / `unselectApplication` / `cancelApplication` (từ ACCEPTED) | `cancel` cả PAYMENT + CONFIRM |
| Đến hạn | Scheduler claim → `expire(id, stage)` |

Mọi `schedule`/`cancel` bọc try/catch — **lỗi Redis không làm fail request**; reconciler sẽ vớt.

### 2.2. Lưới an toàn & backfill

- `ApplicationExpiryReconciler` (`reconcile.enabled=true`, mặc định 1h/lần) chạy lại 2 query `findExpiredSelections`/`findExpiredConfirmations` → vớt đơn mất khỏi Redis (flush, mất AOF, process chết sau claim).
- `ExpiryBackfillRunner` chỉ chạy khi `backfill=true`: nạp mọi đơn `ACCEPTED` — paid → CONFIRM theo `transactionDate` thật (fallback `now + window`), chưa paid → PAYMENT theo `selectedAt`. **Cần chạy 1 lần sau deploy** nếu có đơn ACCEPTED tồn tại từ trước (vd: seed data), nếu không các đơn đó chỉ được reconciler dọn sau tối đa 1h.
- Nếu chạy multi-instance: Redis claim đã an toàn, nhưng reconciler nên thêm ShedLock để tránh quét trùng (không bắt buộc về đúng đắn).

## 3. Thay đổi BE — module application

| File | Vai trò |
| --- | --- |
| `entity/Application.java` | `Instant selectedAt` (nullable) — mốc chọn đơn, neo W1 |
| `dto/response/ApplicationResponse.java` | `selectedAt`, `expiresAt`, `paidAt` |
| `mapper/ApplicationMapper.java` | `@AfterMapping fillExpiresAt`: chỉ đơn `ACCEPTED` có hạn — đã trả thì `transactionDate + confirm-window`, chưa trả thì `selectedAt + payment-window`; `paidAt = transactionDate` khi `SUCCESS` |
| `service/impl/ApplicationServiceImpl.java` | `selectApplication` set `selectedAt` + schedule PAYMENT; `confirmApplication` yêu cầu applicant + `transaction=SUCCESS`; `unselectApplication`; `cancelApplication` revert `Post → PUBLISHED` + restore `REJECTED → PENDING`; các transition gọi `cancelExpiry` |
| `service/ApplicationExpiryService.java` | `@Transactional expire(id, stage)` — re-check DB rồi hủy đơn, revert post, refund (W2), notify applicant + author |
| `scheduler/ApplicationExpiryQueue.java` | ZSET delay queue (`schedule`/`cancel`/`claimDue`) |
| `scheduler/ApplicationExpiryScheduler.java` | Poll Redis, gọi `expire` từng đơn, retry khi lỗi |
| `scheduler/ApplicationExpiryReconciler.java` | Quét DB dự phòng mỗi `reconcile.interval-ms` |
| `scheduler/ExpiryBackfillRunner.java` | Nạp đơn `ACCEPTED` cũ vào Redis (chạy 1 lần) |
| `repository/ApplicationRepository.java` | `findExpiredSelections` (W1), `findExpiredConfirmations` (W2), `findAllByApplicationStatus` (backfill), `existsParticipantByPostIdAndUserId` (post visibility) |
| `controller/ApplicationController.java` | `POST /v1/applications/{id}/unselect` |
| `common/utils/AfterCommit.java` | Helper chạy side-effect sau commit |
| `enums/ApplicationResponseCode.java` | `UNSELECT_SUCCESS` |

### 3.1. Module payment (hook)

- `PaymentServiceImpl.payCommission`: sau khi transaction `SUCCESS` → `cancel(PAYMENT)` + `schedule(CONFIRM, id, transactionDate + confirmWindow)` (sau commit). Chỉ cho pay khi đơn `ACCEPTED` + post `PENDING` + chưa quá payment-window.

### 3.2. Cấu hình (`application.properties`)

```properties
application.expiry.interval-ms=30000             # chu kỳ poll Redis
application.expiry.payment-window-minutes=1440    # W1: 24h
application.expiry.confirm-window-minutes=1440    # W2: 24h
application.expiry.batch-size=100
application.expiry.retry-delay-seconds=60
application.expiry.reconcile.enabled=true
application.expiry.reconcile.interval-ms=3600000  # 1 giờ
application.expiry.backfill=false                # bật 1 lần để nạp đơn ACCEPTED cũ
```

`ApplicationMapper` đọc cùng 2 khóa window — **phải khớp scheduler/service**, nếu không `expiresAt` FE hiển thị sẽ lệch với thời điểm đơn thật sự bị hủy.

**Redis bắt buộc bật AOF** (`appendonly yes`, `appendfsync everysec`) để không mất lịch hết hạn khi restart.

### 3.3. Contract cho FE

- `ApplicationResponse.selectedAt`: thời điểm được chọn (null nếu chưa select/đã revert).
- `ApplicationResponse.expiresAt`: hạn cuối bước đang chờ — FE dùng để countdown. `null` với trạng thái khác `ACCEPTED`.
- `ApplicationResponse.paidAt`: thời điểm thanh toán thành công (null nếu chưa trả).
- `paid` (FE tự suy ra): `transactionStatus === "SUCCESS"`.
- Nút hiển thị: `ACCEPTED && paid && isApplicant` → "Xác nhận"; `ACCEPTED && !paid && isFeePayer` → "Thanh toán phí".

## 4. Post visibility (module post)

`PostServiceImpl.getPostById` phân quyền theo status — cần thiết vì đơn `ACCEPTED` làm post chuyển `PENDING`/`CLOSED` nhưng applicant vẫn phải xem được bài:

| Status | Ai xem được |
| --- | --- |
| `PUBLISHED` | Tất cả (kể cả anonymous) |
| `PENDING` / `CLOSED` | Author + applicant (ai có application trên bài, mọi status đơn) |
| `DRAFT` / `REMOVED` | 404 với non-author (author dùng `/posts/{id}/me`) |

Files liên quan:

- `post/repository/PostRepository.java`: `findByIdForVisibilityCheck` (mọi status trừ REMOVED)
- `post/service/impl/PostServiceImpl.java`: `getPostById` + `isPostParticipant`
- `application/repository/ApplicationRepository.java`: `existsParticipantByPostIdAndUserId`

## 5. Thay đổi FE

- `ExpiryCountdown` component: đếm ngược tới `expiresAt`, `onExpire` → refetch.
- `ApplyModal`: **form dùng chung cho cả student và tutor** — cùng nhập `proposedFee` + `message`.
- `MyApplicationsPage` / `OfferList`: render countdown + nút Xác nhận/Thanh toán theo `applicationStatus` + `paid` + role.
  - `OfferList` chỉ render cho `role === 'TUTOR'` (banner trả phí chỉ dành cho tutor).
  - Đơn `ACCEPTED && paid` phía tutor hiển thị "Đã thanh toán — chờ học viên xác nhận" + countdown "để học viên xác nhận".
  - Countdown label: `paid` → "học viên xác nhận"; `!paid` + fee payer → "thanh toán"; `!paid` + applicant → "xác nhận".
  - `ApplicantRow`: `FIND_TUTOR` hiển thị "gia sư thanh toán" thay "thanh toán".
- `PostDetailPage`: nút "Hủy đăng ký" guard `!isAuthor`.
- `i18n`: `post.status.PENDING` ("Chờ duyệt"), `application.appliedAgo`/`rank`/`matchScore`.
- `navItems.ts` / `router.tsx` / `DashboardPage.tsx`: route "Đơn đã ứng tuyển" dùng chung cho student + tutor.
- `application.service.ts`: `getApplicationById(id)` (admin complaint modal), truyền `selectedAt`/`paidAt` xuống response.

## 6. Thông báo khi chọn đơn (`selectApplication`)

| Post type | Người trả phí | Notification |
| --- | --- | --- |
| `FIND_TUTOR` (student đăng, tutor apply) | Tutor applicant | Applicant nhận "vui lòng thanh toán phí trong thời hạn quy định" |
| `FIND_STUDENT` (tutor đăng, student apply) | Tutor = post author | Post author nhận nhắc trả phí; student applicant nhận "chờ gia sư thanh toán" |

## 7. Việc còn lại / lưu ý

- [ ] `RefundService` hiện là `MockRefundServiceImpl` (chỉ log, không cộng ví, không đổi status) — khi làm refund thật phải idempotent (check `REFUNDED`, idempotency key) vì `expire` có thể bị gọi lại.
- [ ] `existsByPostIdAndStudentId/TutorId` đang chặn apply lại kể cả đơn `CANCELLED/REJECTED` — cân nhắc nới chỉ tính đơn "sống".
- [ ] Post `PENDING`/`CLOSED` participant xem được — nếu muốn chặt hơn (chỉ applicant của đơn ACCEPTED trở lên) thì siết `existsParticipantByPostIdAndUserId` thêm filter status.
- [ ] Đơn seed/legacy không có `selectedAt`: W1 không match (không biết mốc chọn), W2 vẫn dọn được qua `transactionDate`.
- [ ] Post `CLOSED`/`REMOVED` không bị reopen khi đơn hết hạn — chỉ `PENDING` mới về `PUBLISHED`.

## 8. Cách test nhanh

1. Student đăng bài `FIND_TUTOR` → tutor apply → student select.
2. Post chuyển `PENDING`; tutor thấy offer + countdown W1 (24h). Kiểm tra Redis: `ZRANGE application:expiry:payment 0 -1 WITHSCORES` có member = applicationId.
3. Tutor trả phí → `transactionStatus=SUCCESS`; đơn hiện countdown W2 (24h). Redis: item chuyển từ key `payment` sang `confirm`.
4. **Student KHÔNG confirm** (vì FIND_TUTOR thì tutor applicant confirm) — tutor bấm "Xác nhận" → `CONFIRMED`, post `CLOSED`, item bị `ZREM` khỏi cả 2 key.
5. Với `FIND_STUDENT`: student applicant mới là người confirm.
6. Để đơn quá hạn: chỉnh `payment-window-minutes`/`confirm-window-minutes` nhỏ, đợi scheduler poll (30s) → đơn `CANCELLED`, post về `PUBLISHED` (nếu đang PENDING), W2 có refund.
7. Test reconciler: `ZREM` tay một item khỏi Redis → đơn vẫn bị hủy trong vòng `reconcile.interval-ms`.
