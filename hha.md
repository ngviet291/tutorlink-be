# Báo cáo rà soát thông báo (`NotificationCreator`) — TutorLink

Phạm vi: BE (`tutorlink.zip`) và FE (`src.zip`) bản mới nhất. Báo cáo chỉ dựa trên việc đọc code, **chưa chạy hệ thống**. Số dòng là số dòng trong zip tại thời điểm đọc.

---

## 1. Tóm tắt

- BE mới có **3 service** gọi `NotificationCreator`: `ApplicationServiceImpl`, `PostServiceImpl`, `TutorCertificateReviewService`. Tổng cộng **13 điểm phát thông báo**.
- Luồng **đơn ứng tuyển** gần đủ. Luồng **ví / thanh toán / hoàn tiền**, **đánh giá / khiếu nại**, **admin duyệt gia sư thủ công**, **đơn hết hạn do scheduler** hiện **không có thông báo nào**.
- Có 2 lỗi nội dung/đối tượng nhận cần sửa ngay (mục 4).
- Hạ tầng thiếu một thứ quan trọng: thông báo **không có liên kết tới đối tượng** (bài, đơn, giao dịch), nên bấm vào thông báo ở FE chỉ đánh dấu đã đọc, không chuyển trang được (mục 5).

---

## 2. Hạ tầng hiện có

| Thành phần | Ghi chú |
|---|---|
| `NotificationCreator.createNotification(recipientId, type, title, content)` | Chỉ cho service gọi, `@Transactional(propagation = MANDATORY)`. Lưu DB rồi phát event `NotificationCreated`. |
| Realtime | `NotificationCreatedRealtimeHandler` đẩy `NOTIFICATION_NEW` tới đúng người dùng; `GlobalEventListener` chạy `AFTER_COMMIT` + `@Async`, nên chỉ đẩy khi giao dịch đã commit. |
| API đọc | `GET /v1/notifications`, `PATCH /{id}/read`, `PATCH /read-all`, `GET /unread-count`. |
| FE | `HttpNotificationGateway` đã nối API thật, `NotificationRealtimeBinding` tự làm mới danh sách và hiện toast khi có event. |
| `NotificationType` BE | `SYSTEM, APPLICATION, TRANSACTION, MESSAGE, REVIEW, POST, MATCHING`. |

Loại **chưa được dùng ở đâu cả** trong BE: `TRANSACTION`, `MESSAGE`, `REVIEW`, `POST`, `MATCHING`. Chỉ `SYSTEM` và `APPLICATION` có người gọi.

---

## 3. Các thông báo đã có

| # | Sự kiện | Nơi gọi | Người nhận | Type | Ghi chú |
|---|---|---|---|---|---|
| 1 | Có người ứng tuyển | `ApplicationServiceImpl.createApplication` (~142) | Chủ bài | APPLICATION | Đủ |
| 2 | Chủ bài chọn ứng viên | `selectApplication` (~265) | Ứng viên được chọn | APPLICATION | **Sai đối tượng khi bài FIND_STUDENT** (mục 4.1) |
| 3 | Các ứng viên khác bị loại khi có người được chọn | `selectApplication` (~269, vòng lặp) | Từng ứng viên bị loại | APPLICATION | Đủ |
| 4 | Chủ bài từ chối đơn | `rejectApplication` (~295) | Ứng viên | APPLICATION | Đủ |
| 5 | Xác nhận lớp học | `confirmApplication` (~336) | Chủ bài | APPLICATION | Đủ |
| 6 | Bỏ chọn ứng viên | `unselectApplication` (~373) | Ứng viên | APPLICATION | Đủ |
| 7 | Hoàn thành lớp | `finishApplication` (~406) | Bên còn lại | APPLICATION | Đủ |
| 8 | Ứng viên hủy đơn | `cancelApplication` (~464) | Chủ bài | APPLICATION | Đủ |
| 9 | Admin gỡ bài | `PostServiceImpl.removePostAdmin(postId, reason)` (~452) | Tác giả bài | SYSTEM | Đủ. Không báo cho ứng viên đang chờ (mục 4.5) |
| 10 | AI duyệt hồ sơ gia sư: được duyệt | `TutorCertificateReviewService.notifyResult` | Gia sư | SYSTEM | Đủ |
| 11 | AI: chứng chỉ được xác minh | cùng hàm | Gia sư | SYSTEM | Đủ |
| 12 | AI: hồ sơ / chứng chỉ không hợp lệ | cùng hàm | Gia sư | SYSTEM | Đủ |
| 13 | AI duyệt thất bại | `notifyReviewFailed` | Gia sư | SYSTEM | Chỉ báo gia sư, **admin không biết** hồ sơ đang kẹt (mục 4.6) |

---

## 4. Chỗ còn thiếu hoặc sai — xếp theo mức ưu tiên

Quy ước: **P0** = ảnh hưởng tiền / quyết định nghiệp vụ, nên làm trước. **P1** = nên có. **P2** = tùy chọn.

### 4.1 [P0 — lỗi] Nhắc thanh toán phí gửi nhầm người khi bài FIND_STUDENT

- Vị trí: `ApplicationServiceImpl.selectApplication` (~265).
- Hiện tại: thông báo "Bạn được chọn cho bài ... Vui lòng thanh toán phí" luôn gửi cho `applicantOf(application)`.
- Vấn đề: phí hoa hồng **luôn do gia sư trả** (`PaymentServiceImpl.resolveTutorId`: lấy `application.getTutor()`, nếu null thì lấy tác giả bài). Ở bài FIND_STUDENT, người ứng tuyển là **học viên**, còn gia sư là **chủ bài**. Học viên nhận câu "vui lòng thanh toán phí" là sai, gia sư (người cần trả) lại không được nhắc.
- Đề xuất:
    - FIND_TUTOR: giữ nguyên (gia sư là ứng viên, được nhắc trả phí).
    - FIND_STUDENT: học viên nhận "Bạn được chọn cho bài ..., đang chờ gia sư xác nhận thanh toán"; gia sư (chủ bài) nhận "Bạn đã chọn học viên ..., vui lòng thanh toán phí trong thời hạn quy định".

### 4.2 [P0 — thiếu] Đơn hết hạn do scheduler không báo ai

- Vị trí: `ApplicationExpiryScheduler.expireApplication` và nhánh `refundService.refundCommission`.
- Hiện tại: đơn bị chuyển `CANCELLED`, bài quay về `PUBLISHED`, các đơn bị loại được khôi phục `PENDING`, nhưng **không ai được thông báo**.
- Cần thêm:
    - Báo **ứng viên được chọn**: "Đơn của bạn vào bài ... đã hết hạn" (nêu rõ lý do: chưa thanh toán hay chưa xác nhận).
    - Báo **chủ bài**: "Bài ... đã mở lại vì ứng viên không thanh toán/xác nhận đúng hạn".
    - Báo **các ứng viên được khôi phục về PENDING**: "Bài ... mở lại, đơn của bạn đang chờ xét duyệt" (tùy chọn, P1).
    - Khi hoàn phí (`refundCommission`, hiện là mock): báo gia sư `TRANSACTION` "Phí ... đã được hoàn".
- Lưu ý kỹ thuật: `expireApplications()` đã `@Transactional` nên gọi `NotificationCreator` được. Có thể tái dùng helper `notifyUser` của `ApplicationServiceImpl` (hiện là `private`) bằng cách đưa ra một class dùng chung.

### 4.3 [P0 — thiếu] Ví và thanh toán (`PaymentServiceImpl`) không có thông báo nào

| Sự kiện | Nơi chèn | Người nhận | Type | Nội dung gợi ý |
|---|---|---|---|---|
| Nạp ví thành công | `settleTopUp`, nhánh `SUCCESS` hợp lệ | Gia sư | TRANSACTION | "Nạp ví thành công +{amount}đ" |
| Nạp ví thất bại | `settleTopUp`, nhánh `FAILED` | Gia sư | TRANSACTION | "Nạp ví không thành công" |
| Nạp ví lệch số tiền | `settleTopUp`, nhánh amount mismatch | Gia sư (+ admin, mục 4.6) | TRANSACTION | "Giao dịch cần đối soát, vui lòng liên hệ hỗ trợ" |
| Thanh toán phí thành công | `payCommission` sau khi lưu SUCCESS | Gia sư | TRANSACTION | "Đã thanh toán phí {amount}đ cho bài ..." |
| Thanh toán phí thành công | cùng chỗ | **Học viên của đơn** | APPLICATION | "Gia sư đã thanh toán, bạn có thể nhắn tin ngay" (kèm kênh chat vừa mở) |
| Hoàn phí bởi admin | `refundTransaction` | Gia sư | TRANSACTION | "Phí ... đã được hoàn vào ví" |

- Lưu ý: `settleTopUp` chạy trong `handleGatewayCallback` và `syncTopUpStatus` đều `@Transactional`, gọi được `NotificationCreator`. Giao dịch nạp ví có thể được chốt từ **hai đường** (IPN và `syncTopUpStatus`); `settleTopUp` đã chặn chốt lặp bằng điều kiện `PENDING`, nên đặt thông báo **bên trong** hàm này là an toàn, không bị gửi đôi.
- `payCommission` thất bại do thiếu số dư dùng `noRollbackFor`, nên thông báo "số dư không đủ" cũng lưu được nếu muốn (P2).

### 4.4 [P1 — thiếu] Đánh giá và khiếu nại (`ReviewServiceImpl`, `ReviewComplaintServiceImpl`)

| Sự kiện | Nơi chèn | Người nhận | Type |
|---|---|---|---|
| Học viên đánh giá gia sư | `createReview` | Gia sư | REVIEW |
| Gia sư gửi khiếu nại đánh giá | `ReviewComplaintServiceImpl.create` | Tất cả admin | SYSTEM |
| Admin xử lý khiếu nại (APPROVED/REJECTED) | `resolve` | Gia sư gửi khiếu nại | REVIEW |

- Cần thêm cách lấy danh sách admin (ví dụ `userRepository` theo role ADMIN); hiện chưa có chỗ nào gửi thông báo tới admin.

### 4.5 [P1 — thiếu] Bài đăng

- `closePost` và `removePost` (do **tác giả**) không báo **các ứng viên đang chờ** (PENDING). Ứng viên không biết bài đã đóng.
- `removePostAdmin(postId, reason)` chỉ báo tác giả; ứng viên đang chờ cũng không biết.
- Bài **không có scheduler hết hạn theo `deadline`** (chỉ có `ApplicationExpiryScheduler`). Nếu sau này thêm job đóng bài quá hạn thì phải báo tác giả.
- Lưu ý: ở `PostServiceImpl` bản `removePostAdmin(UUID)` (không có lý do) tìm bài bằng id của admin nên chỉ gỡ được bài của chính admin; bản có `reason` đúng. Nếu controller admin còn gọi bản không có lý do thì không gỡ được bài người khác và cũng **không phát thông báo**.

### 4.6 [P1 — thiếu] Admin duyệt gia sư thủ công và thông báo cho admin

- `UserServiceImpl.approveTutor(userId, request)` (admin duyệt/từ chối) **không có thông báo**; chỉ luồng AI mới báo gia sư. Cần báo gia sư kết quả, kèm `reason` khi bị từ chối (`SYSTEM`).
- `notifyReviewFailed` chỉ báo gia sư. Admin nên nhận "Hồ sơ gia sư {tên} cần duyệt thủ công vì AI lỗi".
- `TutorCertificateService.uploadCertificates` khi gia sư bị REJECTED nộp lại (`resubmitIfRejected`) nên báo gia sư "Đã nhận hồ sơ, đang xét duyệt" (P2) và (tùy chọn) báo admin.

### 4.7 [P1/P2 — thiếu] Tài khoản và bảo mật

| Sự kiện | Nơi | Người nhận | Mức |
|---|---|---|---|
| Đổi mật khẩu thành công | `ChangePasswordService` | Chính người dùng (SYSTEM) | P1 |
| Đặt lại mật khẩu thành công | `AuthenticationService` (luồng reset) | Chính người dùng | P2 |
| Admin khóa tài khoản | `UserServiceImpl.updateUserStatus` | — | Không có ích khi bị khóa vì không đăng nhập được; nên gửi email |
| Admin mở khóa tài khoản | `updateUserStatus` | Người dùng (SYSTEM) | P2 |
| Đăng ký xong (chào mừng / chờ duyệt) | `registerStudent`, `registerTutor` | Người dùng | P2 |

### 4.8 [P2 — cân nhắc] Các loại chưa dùng

- `MESSAGE`: **không nên** tạo thông báo DB cho mỗi tin nhắn (sẽ spam, bảng phình rất nhanh). Chat đã có realtime `MessageSent` và `unread-count` riêng. Chỉ nên thông báo khi **mở kênh chat lần đầu** (xem 4.3, thanh toán phí).
- `MATCHING`: có `TutorMatchingAiService` và `AreaMatcher`, nhưng chưa dùng cho thông báo. Có thể báo gia sư phù hợp khi có bài FIND_TUTOR mới được đăng. Cần tính tần suất để không spam; nên làm sau cùng.
- `POST`: dùng cho các thông báo bài đăng ở 4.5 nếu muốn tách khỏi `SYSTEM`.
- Yêu thích: **không nên** báo gia sư khi bị thêm vào yêu thích (riêng tư, dễ spam).

---

## 5. Vấn đề hạ tầng và FE

### 5.1 Thông báo không có liên kết tới đối tượng (quan trọng)

- `Notification` / `NotificationResponse` chỉ có `id, title, content, notificationType, isRead, createdAt`.
- `NotificationList.tsx` khi bấm chỉ gọi `markOne` và `onNavigate` (đóng popup). Không có chuyển trang.
- Đề xuất: thêm vào entity, DTO và `createNotification` hai trường `referenceType` (enum: `POST`, `APPLICATION`, `TRANSACTION`, `REVIEW`, `COMPLAINT`, `TUTOR_PROFILE`…) và `referenceId` (UUID, nullable). FE ánh xạ sang route đã có:

| referenceType | Route FE |
|---|---|
| POST | `ROUTES.POST_DETAIL(id)` |
| APPLICATION | `ROUTES.MY_APPLICATIONS` (hoặc `POST_DETAIL` của bài tương ứng) |
| TRANSACTION | `ROUTES.WALLET` |
| TUTOR_PROFILE | `ROUTES.TUTOR_PROFILE` |
| REVIEW | `ROUTES.TUTOR_PROFILE` |
| COMPLAINT | `ROUTES.ADMIN_COMPLAINTS` |
| CHAT | `ROUTES.CHAT(channelId)` |

- Để tương thích dữ liệu cũ, các cột này để nullable, FE chỉ chuyển trang khi có giá trị. Cần cập nhật cả `InitData` (đang seed thông báo không có tham chiếu).

### 5.2 Lưu ý khi gọi `NotificationCreator`

- Phương thức yêu cầu giao dịch đang mở (`MANDATORY`). **Không gọi** từ method `@Transactional(readOnly = true)` (ghi sẽ không được lưu đúng) và không gọi từ code không có giao dịch (sẽ ném `IllegalTransactionStateException`). `TutorCertificateReviewService` đã bọc bằng `tx.executeWithoutResult` đúng cách.
- Nên gom việc tạo nội dung thông báo vào **một class riêng** (ví dụ `NotificationTemplates`) thay vì rải hằng số chuỗi trong từng service như hiện nay; đồng thời đưa helper `notifyUser(User, …)` (đang `private` trong `ApplicationServiceImpl`) ra dùng chung, kiểm tra `recipient != null`.
- Hiện chưa có chính sách **dọn / giới hạn** thông báo cũ (không có job xóa). Nên thêm job xóa thông báo đã đọc quá N ngày khi số lượng tăng.
- Không có chống trùng: nếu một hành động bị gọi lại (ví dụ `finishApplication` cho phép gọi khi đã `FINISHED`) thì thông báo bị gửi lặp. Nên chặn ở `finishApplication` khi trạng thái đã `FINISHED`.

### 5.3 Lệch kiểu / mã thừa ở FE

- `NotificationType` ở FE (`common.types.ts`) có thêm `PAYMENT` và `CHAT` mà BE không có; BE có `TRANSACTION` và `MESSAGE`. Nên thống nhất theo BE và bỏ hai loại thừa (cả `icons` trong `NotificationList.tsx` và `NOTIFICATION_TYPE_LABEL` trong `enums.ts`).
- `features/notification/hooks/useNotificationRealtime.ts` trùng chức năng với `NotificationRealtimeBinding.tsx` và còn comment "ĐỔI"; nên xóa nếu không còn dùng (không tìm thấy nơi nào import).
- `env.ts` còn cờ `notify: true` nhưng `container.ts` luôn dùng `HttpNotificationGateway` (API thật). Cờ này không còn tác dụng, nên bỏ hoặc cập nhật để khỏi gây hiểu nhầm.
- Một số service mock FE (`admin.service.ts`, `tutor.service.ts`) vẫn tự ghi mảng `notifications` mock khi duyệt gia sư / xóa bài. Khi chạy API thật các đoạn này không dùng, nhưng nên xóa để tránh nhầm với thông báo thật.

---

## 6. Thứ tự làm đề xuất

1. **Sửa 4.1** (nhắc phí đúng người) — nhỏ, sửa tại chỗ.
2. **Thêm 4.2** (scheduler hết hạn) và **4.3** (ví / phí / hoàn phí) — ảnh hưởng tiền và trạng thái đơn.
3. **Thêm `referenceType` / `referenceId`** (5.1) rồi gắn vào mọi lần gọi mới và cũ, FE bấm là chuyển trang.
4. **Thêm 4.6** (admin duyệt thủ công + báo admin khi AI lỗi) và **4.4** (đánh giá / khiếu nại).
5. **Thêm 4.5** (báo ứng viên khi bài đóng / gỡ).
6. Làm 4.7, 4.8 và dọn các mục ở 5.2, 5.3.

---

## 7. Ví dụ mã tham khảo (chưa chạy thử)

Chuẩn bị chung (class mới, dùng trong mọi service):

```java
@Component
@RequiredArgsConstructor
public class NotificationHelper {
    private final NotificationCreator creator;

    public void notify(User recipient, NotificationType type, String title, String content) {
        if (recipient != null) {
            creator.createNotification(recipient.getId(), type, title, content);
        }
    }
}
```

Nạp ví thành công, đặt trong `PaymentServiceImpl.settleTopUp`, ngay sau `transaction.setStatus(SUCCESS)`:

```java
notificationHelper.notify(
        transaction.getTutor(),
        NotificationType.TRANSACTION,
        "Nạp ví thành công",
        String.format("Ví của bạn đã được cộng %,.0fđ.", transaction.getAmount()));
```

Sửa 4.1 trong `selectApplication`:

```java
if (post.getType() == PostType.FIND_TUTOR) {
    // ứng viên là gia sư → nhắc trả phí
    notifyUser(applicant, APPLICATION, NOTI_APPLICATION_SELECTED_TITLE,
        String.format("Bạn được chọn cho bài \"%s\". Vui lòng thanh toán phí trong thời hạn quy định.", post.getTitle()));
} else {
    // FIND_STUDENT: ứng viên là học viên, gia sư (chủ bài) mới là người trả phí
    notifyUser(applicant, APPLICATION, NOTI_APPLICATION_SELECTED_TITLE,
        String.format("Bạn được chọn cho bài \"%s\". Vui lòng chờ gia sư thanh toán phí.", post.getTitle()));
    notifyUser(post.getAuthor(), APPLICATION, NOTI_APPLICATION_SELECTED_TITLE,
        String.format("Bạn đã chọn %s cho bài \"%s\". Vui lòng thanh toán phí trong thời hạn quy định.",
                applicantName(saved), post.getTitle()));
}
```
