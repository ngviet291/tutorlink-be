# TutorLink - Tài liệu bàn giao: module review & review complaint

Phạm vi: module `review` (BE) + các màn hình review/complaint (FE).

## 1. Tổng quan

- **Review**: học viên đánh giá gia sư sau khi đơn `FINISHED`. Một application tối đa một review (`Review.application` OneToOne).
- **ReviewComplaint**: gia sư khiếu nại một review không hợp lệ. Admin xử lý (`PENDING → APPROVED/REJECTED`). Review **không bị xóa** khi complaint được chấp nhận — chỉ đổi trạng thái complaint.
- Ai tạo complaint: **tutor** (chủ review bị khiếu nại). Ai resolve: **admin**.

## 2. API (BE)

Base: `/v1/reviews`, `/v1/review-complaints`

| Method | Path | Mô tả |
| --- | --- | --- |
| POST | `/v1/reviews` | Tạo review (student, đơn FINISHED, chưa review) |
| GET | `/v1/reviews?tutorId=&cursor=&limit=` | List review của tutor (cursor pagination) |
| GET | `/v1/reviews/{reviewId}` | Chi tiết 1 review |
| GET | `/v1/reviews/exists?applicationId=` | Check application đã có review chưa |
| POST | `/v1/reviews/{reviewId}/complaints` | Tutor tạo complaint `{ reason }` |
| GET | `/v1/review-complaints?status=` | Admin list complaint (filter status) |
| PATCH | `/v1/review-complaints/{complaintId}/resolve` | Admin resolve `{ status: APPROVED/REJECTED }` |

### 2.1. `ReviewResponse` fields

`id, applicationId, studentId, studentName, tutorId, tutorName, rating, comment, createdAt, complaintStatus`

- `complaintStatus`: `NONE | PENDING | APPROVED | REJECTED` (map từ complaint mới nhất của review; không có complaint → `NONE`).

### 2.2. `ReviewComplaintResponse` fields

`id, reason, status, reviewId, applicationId, tutorId, adminId, createdAt`

- `applicationId` — lấy từ `review.application.id` (thêm 17/11/2026, phục vụ admin modal).
- `createdAt` — thời điểm gửi khiếu nại (thêm 17/11/2026).

## 3. Files BE chính

| File | Vai trò |
| --- | --- |
| `review/entity/Review.java` | Review ↔ Application (OneToOne), Student, Tutor |
| `review/entity/ReviewComplaint.java` | Complaint ↔ Review, Tutor (người khiếu nại), Admin (người xử lý) |
| `review/service/impl/ReviewServiceImpl.java` | Tạo review, list, exists; set `tutor` = post author nếu author là Tutor (FIND_STUDENT), ngược lại `application.tutor` |
| `review/service/impl/ReviewComplaintServiceImpl.java` | Tạo complaint (chỉ tutor, 1 complaint/review), resolve, `toResponse` map `applicationId`/`createdAt` |
| `review/mapper/ReviewMapper.java` | Map entity → `ReviewResponse` (kèm `studentName`/`tutorName`, `complaintStatus`) |
| `review/controller/ReviewController.java` | Endpoints `/v1/reviews` |
| `review/controller/ReviewComplaintController.java` | Endpoints `/v1/review-complaints` (admin) |

## 4. Files FE chính

| File | Vai trò |
| --- | --- |
| `features/review/service/review.service.ts` | `getReviewsByTutor`, `getReviewById`, `submitReview`, `submitComplaint`, `existsByApplicationId`; map `complaintStatus` BE→FE (`PENDING→OPEN`, `APPROVED/REJECTED→RESOLVED`) |
| `features/review/hooks/useReviews.ts` | `useTutorReviews`, `useHasReviewed`, mutations |
| `features/review/components/ReviewList.tsx` | List review trên trang tutor; tutor owner thấy nút khiếu nại |
| `features/review/components/ReviewDetailModal.tsx` | Modal xem lại đánh giá của 1 đơn FINISHED (từ MyApplications); tutor có thể mở ComplaintModal |
| `features/review/components/ComplaintModal.tsx` | Form gửi lý do khiếu nại |
| `pages/admin/AdminComplaintsPage.tsx` | Admin list + modal **chi tiết khiếu nại**: 4 section (Khiếu nại / Đánh giá / Đơn ứng tuyển / Tiến trình timeline) + nút Chấp nhận/Từ chối |
| `features/admin/service/admin.complaints.service.ts` | `ComplaintDto` (+`applicationId`, `createdAt`), `getComplaints`, `resolveComplaint` |

## 5. Thay đổi 17/11/2026

### BE

- `ReviewComplaintResponse` + `applicationId`, `createdAt`.
- `ReviewComplaintServiceImpl.toResponse` map 2 field trên.
- `ApplicationResponse` + `paidAt` (phục vụ timeline "Thanh toán phí" trong admin modal — xem `docs/application/handover.md` mục 6.1).

### FE

- `review.service.ts`:
  - `getReviewsByTutor` map `applicationId` + `complaintStatus` thật (trước hardcode `NONE` → `ReviewDetailModal` không find được review → "Không tìm thấy đánh giá").
  - Dùng `studentName` BE trả sẵn thay vì fetch `public-profile` từng review (1 request lỗi làm hỏng cả list).
  - Thêm `getReviewById(reviewId)` → `GET /v1/reviews/{id}` (admin modal).
  - Restore `toComplaintStatus` (bị mất khi git restore).
- `AdminComplaintsPage.tsx`: modal "Chi tiết khiếu nại" đầy đủ — fetch song song `applicationService.getApplicationById` + `reviewService.getReviewById`, hiển thị timeline: Ứng tuyển → Được chọn → Thanh toán phí → Đánh giá → Khiếu nại.
- Types: `ComplaintDto` + `applicationId`/`createdAt`; `ReviewComplaintResponse` (dto) + `createdAt`; `ApplicationResponse` + `selectedAt`/`paidAt`.

## 6. Lưu ý / bẫy đã gặp

- **`ReviewDetailModal` tìm review bằng `applicationId`** — `getReviewsByTutor` bắt buộc phải map `applicationId` từ DTO, thiếu là modal luôn "Không tìm thấy đánh giá".
- **`complaintStatus` phải map enum** — FE dùng `NONE/OPEN/RESOLVED`, BE dùng `NONE/PENDING/APPROVED/REJECTED`; hardcode `NONE` làm nút "Khiếu nại" hiện lại dù đã gửi.
- **`existsByApplicationId`** check review tồn tại — dùng để ẩn/hiện nút "Đánh giá" vs "Xem đánh giá" trên đơn FINISHED.
- Complaint chỉ tutor tạo được; student không khiếu nại review của chính mình.
