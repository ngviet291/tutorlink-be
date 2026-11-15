# Bàn giao BE TutorLink: realtime thông báo và điều chỉnh API

Ngày: 14/11/2026. Phạm vi: các thay đổi phía BE mà FE đang chờ, theo thứ tự ưu tiên. Mục nào ghi "cần BE xác nhận" là chỗ FE chưa có đủ thông tin, không phải đã chốt.

## 1. Tóm tắt

| # | Việc | Ưu tiên | Trạng thái |
| --- | --- | --- | --- |
| 1 | Đẩy thông báo realtime (`NOTIFICATION_NEW`) | Cao | FE xong, chờ BE đẩy event |
| 2 | `ApplicationResponse` kèm `student`, `tutor`, `post` rút gọn | Cao | `student`/`tutor` đã sửa DTO; `post` chưa làm |
| 3 | API admin: lọc `status` và xem chi tiết mọi bài | Trung bình | Chi tiết đã có code; lọc `status` chưa có |
| 4 | Xác nhận quy tắc đọc bài theo trạng thái | Trung bình | Cần BE xác nhận |
| 5 | Endpoint `unread-count` cho thông báo | Thấp | Đề xuất |
| 6 | Thông báo kết quả duyệt chứng chỉ gia sư (thành công / thất bại) | Trung bình | Đã viết code, cần BE nối (mục 11) |

## 2. Realtime thông báo (ưu tiên cao)

### 2.1 Hiện trạng

FE đã kết nối STOMP qua SockJS, đã subscribe `/user/queue/events` và đã đăng ký xử lý event `NOTIFICATION_NEW` (làm mới danh sách, hiện toast). Chat đang chạy theo cùng đường này với event `MESSAGE_NEW`. Thông báo chưa hiện realtime vì FE chưa lắng nghe (đã sửa ở FE) và, cần BE xác nhận, vì BE có thể chưa đẩy event này.

### 2.2 Hợp đồng event

Khi tạo một `Notification` cho người dùng, BE gửi tới chính người nhận qua user destination:

- Destination FE đang subscribe: `/user/queue/events`. Phía BE, dùng `convertAndSendToUser(<principalName>, "/queue/events", payload)`.
- `principalName` phải trùng với tên Principal mà BE gán khi xác thực kết nối STOMP (FE gửi header `Authorization: Bearer <accessToken>` trong lúc CONNECT). Cần BE xác nhận đây là userId hay tên khác; nên dùng đúng cách đang dùng cho `MESSAGE_NEW`.
- Payload là phong bì JSON, đủ ba field `type`, `occurredAt`, `data`. FE bỏ qua event thiếu bất kỳ field nào và chỉ in cảnh báo trong console.

```json
{
  "type": "NOTIFICATION_NEW",
  "occurredAt": "2026-11-14T10:00:00Z",
  "data": {
    "id": "uuid",
    "title": "Bạn được chọn nhận lớp",
    "content": "...",
    "notificationType": "APPLICATION",
    "read": false,
    "createdAt": "2026-11-14T10:00:00Z"
  }
}
```

- `data` giữ đúng cấu trúc `NotificationResponse` của `GET /v1/notifications` để FE dùng chung kiểu.
- `notificationType` là một trong: `SYSTEM`, `APPLICATION`, `TRANSACTION`, `MESSAGE`, `REVIEW`, `POST`, `MATCHING`.
- Thời gian ở dạng ISO-8601 (UTC).

### 2.3 Điểm cần lưu ý

- Gửi event sau khi giao dịch DB commit (ví dụ `@TransactionalEventListener(phase = AFTER_COMMIT)`), để FE không refetch trước khi bản ghi tồn tại.
- Event không được replay khi mất kết nối. FE tự gọi lại `GET /v1/notifications` khi kết nối lại, BE không cần lưu hàng đợi.
- Người dùng chưa kết nối WebSocket thì không nhận event, nhưng vẫn thấy thông báo qua REST. Đây là hành vi mong muốn.
- Gửi cho người nhận thông báo, không gửi cho người thực hiện hành động.

### 2.4 Tiêu chí nghiệm thu

1. Đăng nhập hai tài khoản A và B. A thực hiện hành động sinh thông báo cho B (ví dụ chọn gia sư).
2. Màn hình B hiện toast và số trên chuông tăng, không cần tải lại trang.
3. Tab Network của B, lọc WS, thấy một frame STOMP tới `/user/queue/events` có `type` là `NOTIFICATION_NEW`.
4. Ngắt mạng rồi nối lại: danh sách thông báo của B được làm mới.
5. A không nhận event dành cho B.

### 2.5 Khi không thấy event

- Không có frame WS: lỗi ở BE (chưa gửi, sai destination, hoặc sai `principalName`).
- Có frame nhưng FE không hiện: kiểm tra console có dòng `[realtime] bỏ qua event sai định dạng` hoặc `type` khác `NOTIFICATION_NEW`.

## 3. `ApplicationResponse` kèm thông tin liên quan (ưu tiên cao)

### 3.1 Vấn đề

`ApplicationResponse` trước đây chỉ có `postId`, `studentId`, `tutorId`. FE phải gọi thêm `GET /v1/users/{id}/public-profile` cho từng người và `GET /v1/posts/{id}` cho từng bài để hiển thị tên và tiêu đề. Hậu quả:

- Mỗi lần mở danh sách đơn, FE bắn hàng chục request.
- Khi chủ bài chọn gia sư, `selectApplication` đặt `post.status = CLOSED`. Endpoint công khai `GET /v1/posts/{id}` trả 404 cho bài `CLOSED`, nên gia sư không còn thấy các đơn `ACCEPTED`, `PAID`, `FINISHED` của mình (trang "Đơn đã ứng tuyển" và "Kết nối" trống đúng những đơn quan trọng nhất).

### 3.2 Đã làm

- Thêm `UserSummaryResponse` (`id`, `fullName`, `avatarUrl`).
- `ApplicationResponse` đổi `studentId`, `tutorId` thành `student`, `tutor` kiểu `UserSummaryResponse`.
- Mapper `toApplicationResponse` bỏ hai dòng `@Mapping` của `studentId`, `tutorId`.

Cần BE kiểm tra:

- Tên field của entity user có khớp `id`, `fullName`, `avatarUrl` không, nếu khác thì thêm `@Mapping`.
- Mọi chỗ khác dùng `getStudentId()` hoặc `getTutorId()` của `ApplicationResponse` (ví dụ `ApplicationRankingService`, thông báo) phải đổi sang `getStudent().getId()`.
- `student` và `tutor` đang `LAZY` thì cần `join fetch` hoặc `@EntityGraph` ở các query `mine`, danh sách admin và `rankApplications` để tránh N+1.

### 3.3 Còn phải làm: thêm `post` rút gọn

Thêm `PostSummaryResponse` và field `post` vào `ApplicationResponse` (giữ `postId`). Các field FE đang đọc: `id`, `title`, `status`, `type`, `minBudget`, `maxBudget`, `budgetUnit`, `authorId`. Tên và kiểu thật của các field phụ thuộc entity `Post`, FE chưa thấy class này nên chưa viết hộ.

```java
public class PostSummaryResponse {
    private UUID id;
    private String title;
    private PostStatus status;
    private PostType type;
    private BigDecimal minBudget;
    private BigDecimal maxBudget;
    private BudgetUnit budgetUnit;
    private UUID authorId;
}
```

Sau khi BE xong, FE bỏ hẳn các lệnh gọi `GET /v1/posts/{id}` khi ghép đơn. Trang thanh toán phí (`PayFeeModal`) và danh sách ứng viên có thể cần thêm field, FE sẽ báo nếu thiếu.

### 3.4 Tiêu chí nghiệm thu

- `GET /v1/applications/mine`, `GET /v1/applications/{id}`, `GET /v1/applications/post/{postId}/ranked` đều trả `student`, `tutor`, `post`.
- Tài khoản gia sư vẫn thấy đơn thuộc bài `CLOSED` trong `GET /v1/applications/mine`.
- Không phát sinh N+1 query khi lấy 100 đơn (kiểm tra log SQL).

## 4. API admin về bài đăng (ưu tiên trung bình)

### 4.1 Lọc theo trạng thái

`getAllPostsAdmin` đang cố định cả bốn trạng thái `PUBLISHED`, `CLOSED`, `REMOVED`, `DRAFT`, nên ô lọc trạng thái trên trang admin chưa có tác dụng. Đề xuất:

- Thêm tham số `PostStatus status` (không bắt buộc) vào controller `GET /v1/posts/admin` và service.
- Khi có `status`, truyền vào `PostSpecification.hasStatus(status)`. Khi không có thì giữ hành vi hiện tại (cả bốn trạng thái).

FE gửi `status` rỗng thì bỏ khỏi query, BE không cần xử lý chuỗi rỗng.

### 4.2 Chi tiết mọi bài cho admin

Đã viết: `GET /v1/posts/{postId}/admin`, chỉ `ADMIN`, không lọc theo trạng thái nên xem được `DRAFT`, `CLOSED`, `REMOVED`. FE đã nối vào trang chi tiết. Cần BE xác nhận:

- Tên exception not-found đang dùng (bản mẫu để `ResourceNotFoundException`).
- Response bọc `ApiResponse<T>` đúng như các controller khác.

### 4.3 Phân trang

Danh sách admin dùng cursor (`cursor`, `limit`, `hasNext`, `nextCursor`), FE tải tiếp khi cuộn. Không cần đổi.

## 5. Quy tắc đọc bài theo trạng thái (cần BE xác nhận)

FE đang suy ra từ log chứ chưa có tài liệu. Xin BE điền đúng quy tắc:

| Endpoint | Cho đọc những trạng thái nào | Ai được đọc |
| --- | --- | --- |
| `GET /v1/posts/{id}` | Quan sát được: `PUBLISHED`; trả 404 với `CLOSED` và `DRAFT` | Cần BE xác nhận |
| `GET /v1/posts/{id}/me` | Cần BE xác nhận | Cần BE xác nhận (chỉ tác giả, hay cả người có đơn?) |
| `GET /v1/posts/{id}/admin` | Mọi trạng thái | `ADMIN` |

Sau khi chọn gia sư, bài chuyển `CLOSED` và các đơn còn lại của bài chuyển `REJECTED` (đúng theo `selectApplication`). Gọi `select` lần hai trả 404 "Application not found" vì `findSelectableApplication` chỉ tìm đơn `PENDING` của bài `PUBLISHED`. Đây là hành vi đúng, FE không cần BE đổi.

## 6. Xếp hạng ứng viên

`GET /v1/applications/post/{postId}/ranked` đã khớp với FE: `ranked: true` thì `top` đủ 5 người, `others` là phần còn lại, `breakdown` gồm `experience`, `price`, `rating`, `trust`, `warnings` là mảng `MatchWarning`.

Cần BE xác nhận: khi `ranked = false` (từ 5 ứng viên trở xuống), `top` và `others` chứa gì (rỗng, hay chứa toàn bộ). FE hiện ẩn khung xếp hạng khi `ranked` là `false`, nên không phụ thuộc câu trả lời, nhưng cần biết để làm tiếp nếu muốn hiển thị danh sách trong trường hợp đó.

`proposedFee` có thể là `null` (đã thấy trong dữ liệu thật). FE đã coi là tùy chọn.

## 7. Endpoint số thông báo chưa đọc (ưu tiên thấp)

Chuông thông báo đang đếm chưa đọc từ 10 mục đầu của danh sách, nên người có hơn 10 thông báo chưa đọc sẽ thấy số thấp hơn thực tế. Đề xuất `GET /v1/notifications/unread-count` trả số nguyên, giống `GET /v1/chat/unread-count` đang có. Khi có, nên đẩy kèm trong event `NOTIFICATION_NEW` hoặc để FE tăng cục bộ rồi đồng bộ khi kết nối lại.

## 8. Danh sách việc cho BE

- [ ] Đẩy `NOTIFICATION_NEW` tới `/user/queue/events` đúng phong bì ở mục 2.2
- [ ] Xác nhận `principalName` dùng cho user destination
- [ ] Rà soát mọi chỗ dùng `getStudentId()`, `getTutorId()` sau khi đổi DTO
- [ ] `join fetch` hoặc `@EntityGraph` cho `student`, `tutor` trong các query đơn
- [ ] Thêm `PostSummaryResponse` và field `post` vào `ApplicationResponse`
- [ ] Thêm tham số `status` cho `GET /v1/posts/admin`
- [ ] Xác nhận tên exception và `ApiResponse` của `GET /v1/posts/{id}/admin`
- [ ] Điền bảng quy tắc đọc bài ở mục 5
- [ ] Cho biết `top`/`others` khi `ranked = false`
- [ ] (Tùy chọn) `GET /v1/notifications/unread-count`
- [ ] Ghi đè `TutorCertificateReviewService` để gửi thông báo kết quả duyệt chứng chỉ (mục 11)
- [ ] Kiểm tra `NotificationCreatedListener` đẩy được event của thông báo chứng chỉ (mục 11.4)

## 9. Thông tin FE cần BE gửi lại

- Entity `Post` và `PostResponse` (để viết `PostSummaryResponse`).
- Entity user của `Application.student`, `Application.tutor` (để chắc tên field).
- Nơi tạo `Notification` và nơi gửi event của chat (`MESSAGE_NEW`), để đối chiếu cách gửi.

## 10. Cách gọi

Đẩy notification Inject

```java
private final NotificationCreator notificationCreator;
```

Gọi cuối chức năng cần thông báo trước return

```java
public enum NotificationType {
    SYSTEM,
    APPLICATION,
    TRANSACTION,
    MESSAGE,
    REVIEW,
    POST,
    MATCHING
}
// nếu thiếu thì thêm vào
```

Ví dụ một nơi gọi hiện có là `removePostAdmin` (gỡ bài vi phạm), giữ nguyên:

```java
notificationCreator.createNotification(
        post.getAuthor().getId(),          // người nhận, cũng là principalName của user destination
        NotificationType.SYSTEM,
        ADMIN_REMOVE_POST,                 // Title
        String.format("%s %s", ADMIN_REMOVE_POST_REASON, reason) // Content
);
```

Lưu ý: `createNotification` có `@Transactional(propagation = Propagation.MANDATORY)`, nên chỉ gọi được khi đang trong một transaction (method `@Transactional` hoặc `TransactionTemplate`). Gọi ngoài transaction sẽ ném `IllegalTransactionStateException`.

## 11. Thông báo kết quả duyệt chứng chỉ gia sư (ưu tiên trung bình)

### 11.1 Vấn đề

Gia sư tải chứng chỉ qua `POST /v1/users/me/certificates` (hoặc lúc đăng ký). AI duyệt bất đồng bộ trong `TutorCertificateReviewService` sau khi commit. Trước đây gia sư chỉ thấy nhãn "Đang duyệt" rồi chứng chỉ tự biến mất (nếu bị loại) hoặc chuyển "Đã xác thực", không có thông báo nào, và hồ sơ bị từ chối cũng không ai báo.

### 11.2 Hành vi mới

Sau mỗi lần duyệt, `TutorCertificateReviewService` gọi `NotificationCreator.createNotification` (cùng cách với `PostServiceImpl#removePostAdmin`), người nhận là chính gia sư, loại `SYSTEM`.

| Tình huống | Tiêu đề | Nội dung |
| --- | --- | --- |
| Hồ sơ `PENDING` và có ít nhất 1 chứng chỉ hợp lệ (hồ sơ chuyển `APPROVED`) | Hồ sơ gia sư đã được duyệt | Chứng chỉ của bạn đã được xác thực, bạn có thể bắt đầu đăng ký nhận lớp. |
| Hồ sơ `PENDING` và không chứng chỉ nào hợp lệ (hồ sơ chuyển `REJECTED`) | Hồ sơ gia sư bị từ chối | `"<tên chứng chỉ>": <lý do>` của từng chứng chỉ, nối bằng `; ` |
| Hồ sơ đã `APPROVED`, tải thêm và có chứng chỉ hợp lệ | Chứng chỉ đã được xác thực | N chứng chỉ của bạn đã được xác thực. |
| Có chứng chỉ không hợp lệ (bị xóa khỏi danh sách) nhưng hồ sơ không bị từ chối | Chứng chỉ không hợp lệ | `"<tên chứng chỉ>": <lý do>` |
| AI gặp lỗi kỹ thuật (hết quota, mạng...) | Chưa kiểm tra được chứng chỉ | Chứng chỉ vẫn chờ duyệt, sẽ kiểm tra lại ở lần tải lên tiếp theo |

Một lần duyệt có thể sinh 2 thông báo (ví dụ hồ sơ được duyệt nhưng có 1 ảnh bị loại).

### 11.3 Cách gọi

```java
// TutorCertificateReviewService
private final NotificationCreator notificationCreator;

// applyResult chạy trong tx.executeWithoutResult(...) nên thỏa Propagation.MANDATORY
notificationCreator.createNotification(
        tutorId,                       // người nhận
        NotificationType.SYSTEM,
        TITLE_PROFILE_APPROVED,        // Title
        CONTENT_PROFILE_APPROVED       // Content
);
```

- Thông báo được tạo trong cùng transaction với việc ghi kết quả duyệt: cùng thành công hoặc cùng hủy, không bao giờ có thông báo "đã duyệt" mà DB chưa đổi.
- Tên chứng chỉ lấy trước khi xóa các chứng chỉ không hợp lệ. Tên ban đầu lấy từ tên file lúc tải lên (`TutorCertificateService`), AI đọc được tên thật thì ghi đè, không đọc được thì giữ tên file.
- Nội dung thông báo cắt ở 500 ký tự (`MAX_CONTENT_LENGTH`) để không vượt độ dài cột. BE kiểm tra lại giới hạn thực tế của cột `content`.
- Mọi chuỗi tiêu đề và nội dung là hằng số ở đầu class, giống `ADMIN_REMOVE_POST` trong `PostServiceImpl`.

### 11.4 Realtime

`NotificationCreator` đã publish `NotificationCreated`. Khi mục 2 xong (listener đẩy `NOTIFICATION_NEW`), thông báo chứng chỉ tự hiện toast và tăng số trên chuông, không cần code thêm. Cần BE xác nhận listener không giới hạn theo `notificationType`.

Phía FE không cần đổi: danh sách chứng chỉ trên trang hồ sơ tự làm mới mỗi 5 giây khi còn chứng chỉ "Đang duyệt", và dừng khi tất cả đã có kết quả.

### 11.5 Tiêu chí nghiệm thu

1. Gia sư mới đăng ký, tải ảnh chứng chỉ hợp lệ: nhận thông báo "Hồ sơ gia sư đã được duyệt", `verificationStatus` thành `APPROVED`.
2. Tải ảnh không phải chứng chỉ: nhận thông báo "Hồ sơ gia sư bị từ chối" kèm tên file và lý do, `verificationNote` trùng nội dung, ảnh biến mất khỏi danh sách.
3. Gia sư `APPROVED` tải thêm 1 ảnh hợp lệ: nhận "Chứng chỉ đã được xác thực", hồ sơ vẫn `APPROVED`.
4. Gia sư `APPROVED` tải thêm 1 ảnh không hợp lệ: nhận "Chứng chỉ không hợp lệ", hồ sơ vẫn `APPROVED`, không bị hạ trạng thái.
5. Tắt khóa API của AI (hoặc hết quota) rồi tải ảnh: nhận "Chưa kiểm tra được chứng chỉ", chứng chỉ vẫn "Đang duyệt".
6. Mỗi trường hợp trên thấy đúng một thông báo (hoặc hai nếu vừa có ảnh hợp lệ vừa có ảnh không hợp lệ) trong `GET /v1/notifications`.
