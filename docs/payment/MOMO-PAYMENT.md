# Nạp tiền vào ví qua MoMo

## Luồng xử lý

1. Gia sư đã đăng nhập gọi `POST /v1/payments/wallet/top-up` với số tiền `amount`.
2. Backend tạo đơn hàng `captureWallet` và trả về `paymentLink`, `qrCodeUrl` và `deeplink` nếu MoMo cung cấp.
3. Client mở `paymentLink`. Việc chuyển hướng trình duyệt không làm tăng số dư ví.
4. MoMo gửi `POST /v1/payments/momo/ipn`. Backend xác minh HMAC, đối tác, đơn hàng và số tiền; khóa giao dịch và ví; chỉ cộng tiền khi giao dịch nạp tiền đang chờ xử lý được xác nhận thành công.
5. Client có thể thăm dò trạng thái bằng `GET /v1/payments/wallet/top-up/{id}`; chỉ chủ sở hữu giao dịch được phép thực hiện. Backend truy vấn MoMo nếu giao dịch vẫn đang chờ xử lý.
6. Thông báo IPN hợp lệ đã được xử lý trả về HTTP 204. Đơn hàng không xác định sẽ trả về lỗi để tránh xác nhận nhầm một callback đến sớm khi chưa tìm thấy đơn hàng.

`payment.ipn-base-url` phải bao gồm đường dẫn ngữ cảnh (context path) của API khi triển khai. Dịch vụ nối thêm `/v1/payments/momo/ipn`. URL IPN công khai phải truy cập được từ MoMo; backend chỉ chạy trong mạng cục bộ không thể nhận callback từ môi trường production.

## Tên cấu hình

Thiết lập riêng bên ngoài hệ thống quản lý mã nguồn:

- `payment.momo.partner-code`
- `payment.momo.access-key`
- `payment.momo.secret-key`
- `payment.momo.endpoint-create`
- `payment.momo.endpoint-query`
- `payment.ipn-base-url`
- `payment.redirect-url`

Sử dụng thông tin xác thực và endpoint tương ứng với môi trường của đối tác thanh toán. Môi trường production yêu cầu tích hợp MoMo dành cho đối tác đã được kích hoạt; mã nguồn này không kích hoạt tài khoản. Gateway yêu cầu endpoint và URL chuyển hướng/callback sử dụng HTTPS. Không đưa khóa của đối tác vào biến phía frontend.

Profile `dev-mock` chọn gateway giả lập và loại trừ gateway MoMo thật. Không bật profile này khi thực hiện thanh toán thật.

## Trạng thái kiểm chứng

Phần triển khai mới được rà soát tĩnh. Chưa chạy ứng dụng, build, kiểm thử hoặc gửi yêu cầu tới API thanh toán. Trước khi đưa vào production, cần kiểm tra trong môi trường kiểm thử được phép: tạo giao dịch thành công hoặc bị từ chối; chữ ký không hợp lệ; sai đối tác, đơn hàng hoặc số tiền; IPN lặp lại và IPN đồng thời; tranh chấp giữa thăm dò trạng thái và IPN; hết thời gian chờ và thử lại; kiểm tra quyền sở hữu giao dịch. Chưa xác nhận có giao dịch tiền thật thành công.

Tài liệu tham khảo chính thức (Momo đang bảo trì thiết bị nên tất cả các web dưới đề lỗi):

- [API thanh toán ví một lần](https://developers.momo.vn/v3/docs/payment/api/wallet/onetime/)
- [Thông báo kết quả thanh toán qua IPN](https://developers.momo.vn/v3/docs/payment/api/result-handling/notification/)
- [API truy vấn trạng thái giao dịch](https://developers.momo.vn/v3/docs/payment/api/payment-api/query/)

## Ánh xạ biến môi trường

`MOMO_BASE_URL`, `MOMO_ENDPOINT_CREATE`, `MOMO_ENDPOINT_QUERY`, `MOMO_PARTNER_CODE`, `MOMO_ACCESS_KEY` và `MOMO_SECRET_KEY` ghi đè các thuộc tính `payment.momo.*` tương ứng. Cần cấu hình các endpoint phù hợp; chỉ thay đổi URL cơ sở không ghi đè các endpoint đã được khai báo cụ thể.

`MOMO_IPN_BASE_URL` và `MOMO_REDIRECT_URL` ghi đè `payment.ipn-base-url` và `payment.redirect-url`. Nếu thông tin xác thực của đối tác để trống, hệ thống trả về `PAYMENT_503` trước khi gửi yêu cầu tới nhà cung cấp.

Các ánh xạ này không tự tìm những file `.env` bên ngoài ở vị trí bất kỳ. Quá trình thiết lập môi trường phải nạp các biến vào nguồn thuộc tính của Spring. Không chia sẻ hoặc commit giá trị thông tin xác thực.
