# Cách tính điểm xếp hạng gia sư

Chỉ chấm điểm khi bài đăng có **hơn 5 ứng viên** đang chờ duyệt. Từ 5 người trở xuống thì hiển thị hết theo thời gian ứng tuyển.

## Công thức

```
điểm = (w_giá × giá + w_đánh_giá × đánh_giá + w_kinh_nghiệm × kinh_nghiệm + w_nội_dung × nội_dung)
       × các hệ số phạt
```

Mỗi tiêu chí nằm trong khoảng 0 đến 1.

## Tiêu chí

| Tiêu chí | Cách tính |
|---|---|
| **Giá** | Trong ngân sách: 1.0. Thấp hơn mức tối thiểu: 0.8. Vượt ngân sách: giảm dần, `1 - (phí - tối đa) / tối đa`. Thiếu ngân sách hoặc phí: 0.5. Phí lấy từ mức gia sư đề xuất, không có thì lấy giá theo giờ trong hồ sơ rồi quy về đơn vị ngân sách của bài đăng (giờ, buổi hoặc tháng) |
| **Đánh giá** | `(n × điểm_TB + 5 × 4.0) / (n + 5)` rồi chia 5, với `n` là số lượt review. Gia sư ít review bị kéo về mức trung bình, gia sư chưa có review nhận 0.8 |
| **Kinh nghiệm** | `số năm / 10`, tối đa 1.0 |
| **Nội dung (AI)** | Gemini chấm 0 đến 100 độ phù hợp giữa bài đăng với hồ sơ và lời nhắn của gia sư, chia 100 |

## Trọng số

| Tiêu chí | Có điểm AI | AI lỗi |
|---|---|---|
| Giá | 0.30 | 0.40 |
| Đánh giá | 0.25 | 0.35 |
| Kinh nghiệm | 0.15 | 0.25 |
| Nội dung (AI) | 0.30 | 0 |

## Hệ số phạt

Không loại gia sư, chỉ nhân điểm xuống. Nhiều điều kiện sai thì nhân dồn.

| Không khớp | Hệ số |
|---|---|
| Môn học | × 0.3 |
| Cấp học | × 0.5 |
| Hình thức dạy (online và offline không giao nhau, `BOTH` khớp mọi giá trị) | × 0.5 |
| Khu vực (chỉ xét khi bài đăng học trực tiếp) | × 0.6 |
| Lịch rảnh không đủ số buổi mỗi tuần | × 0.6 |

Gia sư chưa điền dữ liệu (cấp dạy, hình thức, khu vực, lịch rảnh) thì không bị phạt.

## Thứ tự

Điểm giảm dần. Bằng điểm thì ai ứng tuyển sớm hơn xếp trước. 5 người đầu là top, những người còn lại vẫn xem được.

## Ví dụ

Bài đăng có ngân sách 150.000 đến 250.000 mỗi giờ, có điểm AI.

| | Gia sư A | Gia sư B |
|---|---|---|
| Giá | 1.0 | 0.8 |
| Đánh giá | 0.928 | 0.8 |
| Kinh nghiệm | 0.5 | 1.0 |
| Nội dung (AI) | 0.85 | 0.90 |
| Điểm cơ bản | 0.862 | 0.860 |
| Phạt | không | chưa dạy cấp này, × 0.5 |
| **Điểm cuối** | **0.862** | **0.430** |