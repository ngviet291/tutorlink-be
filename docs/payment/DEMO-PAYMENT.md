# Thanh toán TutorLink: demo phát triển và QR MoMo

Đây là tiền giả lập nhưng số dư và lịch sử được lưu vào CSDL đang kết nối.
Chỉ chạy với CSDL phát triển, không dùng dữ liệu thật hoặc bật cờ demo trên production.

## Bật demo

1. Dừng backend đang chạy.
2. Trong IntelliJ, mở Run > Edit Configurations của Spring Boot.
3. Giữ nguyên cấu hình/profile đang đăng nhập được, thêm `--payment.demo.enabled=true`
   vào Program arguments rồi chạy lại. Cờ này chỉ chọn gateway và endpoint demo.
4. Mở frontend, đăng nhập tài khoản gia sư phát triển, vào ví và tạo lần nạp mới.
5. Hộp thanh toán hiện nhãn DEMO. Bấm **Xác nhận thanh toán demo thành công**.
6. Kiểm tra trạng thái SUCCESS, số dư tăng đúng số tiền và lịch sử giao dịch.

Không cần MoMo Test, QR, tunnel IPN hay thay bộ khóa MoMo cho luồng này.
Không dùng lại đơn MoMo thật hoặc đơn demo đã tạo trước khi khởi động lại backend.
Đơn demo nằm trong bộ nhớ của gateway; khởi động lại sẽ mất trạng thái cổng giả lập.

## Phạm vi và kiểm thử

- `payment.demo.enabled=true` bật gateway và endpoint POST `/v1/payments/demo/top-up/{id}/confirm`.
- Không đặt cờ hoặc đặt `false` sẽ dùng gateway MoMo thật, không đăng ký endpoint demo.
- Endpoint yêu cầu đăng nhập với quyền TUTOR; service kiểm tra chủ sở hữu trước khi xác nhận.
- Polling không tự xác nhận; IPN công khai bị từ chối trong chế độ demo.
- Việc cộng ví dùng lại khóa giao dịch và kiểm tra PENDING trong service thanh toán.
- Bấm lặp lại không tạo giao dịch mới; cần kiểm thử tích hợp để xác minh số dư khi gọi đồng thời.
- Khi có Maven, chạy unit test không khởi động Spring: `mvn -Dtest=MockPaymentGatewayTest,DemoPaymentControllerTest test`.

## Trở lại MoMo

Bỏ `--payment.demo.enabled=true` khỏi Program arguments, khởi động lại backend và tạo giao dịch mới.
Gateway MoMo thật giữ nguyên. Không sửa `.env` trong thay đổi này.

Có thể đặt rõ `--payment.demo.enabled=false` để chọn MoMo. Chỉ dùng giá trị
`true` hoặc `false`; giá trị khác không khớp điều kiện đăng ký gateway.
Không cần đổi profile đăng nhập/CSDL để chuyển chế độ thanh toán.
Việc MoMo đang bảo trì hay đã phục hồi **cần xác minh** với nhà cung cấp;
không suy ra tình trạng bảo trì chỉ từ lỗi tải trang.

## Cấu trúc mã và luồng xử lý

Các đường dẫn dưới đây tính từ gốc workspace.

| Thành phần | Trách nhiệm | Bằng chứng |
| --- | --- | --- |
| `MockPaymentGateway` | Tạo đơn PENDING trong bộ nhớ, không tạo QR/link giả; truy vấn chỉ đọc trạng thái | `tutorlink-be/src/main/java/org/group3/tutorlink/features/payment/gateway/mock/MockPaymentGateway.java:41`, `:65` |
| `DemoPaymentController` | Chỉ tồn tại khi bật cờ demo; yêu cầu TUTOR và gọi service kiểm tra chủ giao dịch trước khi xác nhận | `tutorlink-be/src/main/java/org/group3/tutorlink/features/payment/controller/DemoPaymentController.java:20`, `:28`, `:31` |
| `PaymentServiceImpl` | Khóa giao dịch, kiểm tra chủ sở hữu, đối chiếu kết quả và cập nhật ví | `tutorlink-be/src/main/java/org/group3/tutorlink/features/payment/service/impl/PaymentServiceImpl.java:157`, `:180` |
| `MomoGateway` | Chọn mặc định khi tắt demo; giữ `captureWallet`, chữ ký, IPN và truy vấn MoMo | `tutorlink-be/src/main/java/org/group3/tutorlink/features/payment/gateway/momo/MomoGateway.java:34`, `:38`, `:138`, `:172` |
| `TopUpModal` | Nhận diện mã đơn MOCK, hiển thị nhãn/nút demo hoặc mở payUrl MoMo; hỏi trạng thái mỗi 3 giây | `tutorlink-fe/src/features/wallet/components/TopUpModal.tsx:39`, `:58`, `:98` |

### Demo

1. FE tạo giao dịch qua API nạp ví hiện có. Gateway tạo providerOrderId bắt đầu bằng `MOCK`.
2. Đơn PENDING được lưu trong CSDL; trạng thái cổng demo nằm trong bộ nhớ JVM.
3. FE gọi POST `/v1/payments/demo/top-up/{id}/confirm` bằng HTTP client có đăng nhập.
   Request không nhận số tiền hoặc kết quả thành công từ trình duyệt.
4. Controller gọi `syncTopUpStatus` để kiểm tra quyền và loại giao dịch.
5. Nếu còn PENDING, gateway đổi đơn thành SUCCESS rồi controller đồng bộ lại qua service.
6. Service đối chiếu số tiền, mã đơn, phương thức và trạng thái trước khi cộng ví.
   FE cập nhật cache trạng thái và làm mới dữ liệu ví.

Tiền tố MOCK chỉ phục vụ giao diện, không thay thế kiểm tra quyền backend.
Callback IPN công khai bị gateway demo từ chối
(`tutorlink-be/src/main/java/org/group3/tutorlink/features/payment/gateway/mock/MockPaymentGateway.java:49`).

### QR MoMo

Backend tiếp tục tạo yêu cầu `captureWallet` và trả `payUrl`.
FE mở trang của MoMo để người dùng quét QR. Backend xử lý kết quả qua IPN
có kiểm tra chữ ký hoặc truy vấn trạng thái đơn. Không có nút xác nhận demo ở luồng này.
Không thay mã QR/deeplink, thuật toán ký hay nghiệp vụ cộng ví khi dọn mã demo.

## Giới hạn vận hành

- Demo vẫn dùng enum MOMO để tương thích hợp đồng API; phân biệt đơn bằng providerOrderId MOCK.
- Không dùng demo trên CSDL thật. Cờ demo không tự tạo CSDL hoặc tài khoản riêng.
- Gateway demo chỉ phù hợp một tiến trình phát triển, không dùng cho nhiều instance.
- Đơn không có trong bộ nhớ trả FAILED khi truy vấn; đơn PENDING cũ có thể bị chốt FAILED
  sau restart hoặc khi đổi gateway. Luôn tạo đơn mới sau khi đổi chế độ.
- Bộ nhớ giữ đơn đến khi tiến trình dừng; chưa có cơ chế dọn đơn theo thời gian.
- Xác nhận gateway và cập nhật CSDL là hai bước riêng. Nếu cập nhật CSDL lỗi sau khi
  gateway thành công, lần polling tiếp theo có thể đồng bộ lại khi tiến trình vẫn chạy.
- Cơ chế khóa/PENDING có trong service; khả năng chống cộng lặp khi gọi đồng thời
  trên CSDL thực tế **cần xác minh** bằng kiểm thử tích hợp.

## Kiểm tra trước khi demo hoặc bật lại MoMo

1. Demo: tạo đơn, chờ vài lần polling; trạng thái vẫn phải PENDING.
2. Bấm xác nhận; kiểm tra SUCCESS và ví tăng đúng một lần.
3. Gọi xác nhận lại cùng đơn; kiểm tra số dư không tăng thêm.
4. Dùng tài khoản gia sư khác xác nhận đơn; backend phải từ chối.
5. Tắt cờ demo và restart: endpoint demo không còn được đăng ký; tạo đơn mới có payUrl MoMo.
6. Kiểm thử MoMo sandbox và callback thực tế khi dịch vụ sẵn sàng.

Unit test hiện có kiểm tra polling không tự thành công, callback bị từ chối,
đơn không tồn tại, xác nhận lặp và controller không gọi gateway khi service từ chối quyền.
Đây không phải kiểm thử Spring Security, lựa chọn bean hoặc giao dịch CSDL xuyên suốt.

Lần kiểm tra dọn mã: TypeScript không báo lỗi ở phần wallet, nhưng toàn bộ kiểm tra
chưa đạt do import không dùng ở `ApplicantList.tsx:10` và `:11`, cùng ánh xạ thiếu
PAYMENT/CHAT ở `NotificationList.tsx:8`. Không sửa các file ngoài phạm vi thanh toán.
Backend chưa chạy unit test vì môi trường công cụ chưa có lệnh Maven (`mvn`).
