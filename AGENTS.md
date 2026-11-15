# TutorLink backend — hướng dẫn cho agent

## Phạm vi và bảo mật

- Áp dụng cùng ../AGENTS.md. Chỉ sửa file được tác vụ cho phép; không tự chạy ứng dụng, build, test, cài package hoặc gọi API trong tác vụ chỉ đọc.
- Không mở/tìm nội dung .env, .env.*, application*.properties, application*.yml, application*.yaml, cấu hình chứa bí mật hoặc Bruno environments/.
- Không đọc .git, lịch sử commit, log, biến môi trường tiến trình, script nạp bí mật hoặc dữ liệu khởi tạo có thể chứa tài khoản. Bỏ qua target, build, .idea và thư mục phụ thuộc/kết quả sinh.
- Chỉ lấy tên cấu hình từ annotation, không lấy giá trị/default. Không ghi bí mật, tên người hoặc đường dẫn máy cá nhân.
- Đây là hướng dẫn hành vi cho agent. AGENTS.md không khóa quyền đọc; chặn đọc phải dùng cấu hình sandbox hoặc quyền file.

## Quy tắc Java và phân quyền bắt buộc

- Không dùng Java `record`; dùng class theo quy ước hiện có cho DTO và các kiểu dữ liệu khác.
- Bảo vệ thao tác dành cho ADMIN bằng `@PreAuthorize("hasAuthority('ADMIN')")` trên method/lớp phù hợp; không kiểm tra role bằng `auth -> auth.getAuthority().equals("ADMIN")` hoặc so sánh authority thủ công tương đương.
- Annotation kiểm tra authority không thay thế kiểm tra quyền sở hữu dữ liệu hay điều kiện nghiệp vụ trong service.
- Controller phụ trách hợp đồng HTTP và gọi service; service xử lý nghiệp vụ; repository truy cập dữ liệu; gateway/client phụ trách gọi dịch vụ ngoài. Không trộn các trách nhiệm này.
- Không nén nhiều câu lệnh, cả method hoặc nhánh xử lý trên một dòng. Viết rõ ràng, tách method theo trách nhiệm và tuân thủ clean code, SOLID.
- Không hardcode giá trị nghiệp vụ, phí, ngưỡng hoặc URL trong logic; dùng cấu hình, constant hoặc enum phù hợp theo quy tắc bảo mật ở `../AGENTS.md`.

## Công nghệ và cấu trúc

- Maven khai báo Java 21, Spring Boot 4.1.1; Web, Validation, JPA, PostgreSQL, Redis, Security OAuth2 Resource Server/JWT, WebSocket, Mail, MapStruct, Lombok, Cloudinary và Spring AI Google GenAI (pom.xml:5, :29, :48). Dependency không chứng minh tích hợp đã hoạt động.
- Mã chính nằm tại src/main/java/org/group3/tutorlink/. Các đường dẫn Java rút gọn dưới đây tính từ thư mục này.
- features gồm auth, user, post, application, payment, chat, review, subject, favorite, notification, statistics; có controller/service/repository/entity/DTO/mapper tùy feature.
- common chứa response/error, security REST/STOMP, WebSocket, upload media, utility và hạ tầng sự kiện.
- Controller nhận DTO và gọi service; service dùng repository/mapper, transaction và kiểm tra quyền (features/post/controller/PostController.java:23; features/post/service/impl/PostServiceImpl.java:54).

## Lệnh chạy — chưa chạy thử

- pom.xml:184 khai báo spring-boot-maven-plugin.
- Lệnh dự kiến mvn spring-boot:run được suy từ plugin, **cần xác minh**; pom không ghi nguyên văn câu lệnh shell chạy dự án. Chưa chạy thử.
- Không tìm thấy README BE trong lần kiểm kê trước; README hiện tại **cần xác minh**. Không có lệnh được trích từ README BE.
- Không khẳng định Maven wrapper, dependencies, CSDL, Redis hoặc cổng thanh toán đã sẵn sàng. Không mở cấu hình bị cấm để kiểm tra khả năng chạy.

## Quy ước thấy trong mã

- REST dùng prefix /v1; ghép mapping cấp class/method trước khi đối chiếu.
- ApiResponse có code/message/data, bỏ null; CursorResponse có content/hasNext/nextCursor (common/dto/response/ApiResponse.java:11; common/dto/response/CursorResponse.java:15). Một số thao tác trả void/204; không ép mọi response thành envelope.
- Request DTO dùng Bean Validation; response DTO và mapper tách hợp đồng API khỏi entity (features/post/dto/request/CreatePostRequest.java:31).
- Security gồm allowlist HTTP và @PreAuthorize; converter bỏ prefix SCOPE (common/security/rest/SecurityConfig.java:35, :97). Không suy quyền chỉ từ controller/comment.
- Service có transaction và kiểm tra nghiệp vụ. Payment cố ý không rollback khi thiếu số dư để giữ giao dịch FAILED (features/payment/service/impl/PaymentServiceImpl.java:219). Khi sửa cần giữ đúng ý nghĩa khóa ví và ghi giao dịch.
- Khi đổi trạng thái, đối chiếu enum, repository query, service, DTO và FE.

## Các luồng và điểm cần cẩn thận

### Auth

- Login kiểm tra email/password và BANNED; register student tạo ACTIVE, tutor tạo ACTIVE và verification PENDING (features/auth/service/AuthenticationService.java:67, :113).
- Register tutor nhận @RequestBody; FE gửi multipart; DTO BE không có certificates (features/auth/controller/AuthenticationController.java:51; features/auth/dto/request/RegisterTutorRequest.java:20).
- Introspect không nằm trong allowlist public nhưng FE dùng public client (common/security/rest/SecurityConfig.java:35; FE src/features/auth/api/http.auth.gateway.ts:25).
- Generate refresh token lưu user ID, refresh tìm bằng email; nhánh đăng ký lưu đè email (features/auth/service/JwtService.java:81; features/auth/service/AuthenticationService.java:132, :220). Lệch định danh thấy trong mã; tác động runtime **cần xác minh**.
- Forgot-password thay mật khẩu rồi gửi email; reset-password nhận token/newPassword và thu hồi token (features/auth/service/AuthenticationService.java:253). Không mô tả thành luồng chỉ gửi link.

### Post và application

- Post kiểm tra loại người đăng và subject tồn tại (features/post/service/impl/PostServiceImpl.java:54). GET posts không thuộc allowlist public (common/security/rest/SecurityConfig.java:49).
- Tạo đơn PENDING; select chuyển ACCEPTED, reject các pending khác và đóng bài. Chủ bài confirm sang CONFIRMED; người tham gia finish sang FINISHED; applicant cancel PENDING/ACCEPTED (features/application/service/impl/ApplicationServiceImpl.java:52, :187, :254, :291, :323).
- Response application có studentId/tutorId, không phải object student/tutor FE đang đọc; không có proposedFee trong response (features/application/dto/response/ApplicationResponse.java:26; FE src/features/application/service/application.service.ts:123).
- Ranked, fee-quote, tutor-profile, change-password, review-exists và một số route admin chưa khớp mapping trong lần phân tích trước. Kiểm tra lại controller hiện tại; không thêm API chỉ dựa trên comment FE.

### Payment

- TopUpRequest chỉ có amount; history nhận cursor/limit, không có filter trạng thái của FE (features/payment/dto/request/TopUpRequest.java:20; features/payment/controller/PaymentController.java:70).
- Callback giao gateway xác minh; settlement xử lý PENDING, kiểm tra số tiền và cộng ví khi thành công (features/payment/service/impl/PaymentServiceImpl.java:141, :179). Tính đúng khi chạy đồng thời **cần xác minh**, không kết luận từ comment idempotent.
- Gia sư trả commission khi đơn ACCEPTED hoặc FINISHED, không nhận CONFIRMED. Pay không chuyển trạng thái đơn; confirm không kiểm tra payment (features/payment/service/impl/PaymentServiceImpl.java:223; features/application/service/impl/ApplicationServiceImpl.java:254).
- Commission lấy maxBudget, thiếu thì minBudget, nhân commissionRate; hàm này không đọc MatchingFeeConfig dù có API quản lý cấu hình phí (features/payment/service/impl/PaymentServiceImpl.java:405, :469).

### Chat và review

- Chat send kiểm tra cặp Tutor–Student và yêu cầu channel có sẵn; comment yêu cầu thanh toán chưa được thực thi trong đoạn send. Có createChannel nhưng lần tìm tham chiếu trong mã chính chưa thấy caller; luồng tạo channel **cần xác minh** (features/chat/service/ChatService.java:48, :95, :109).
- Có cấu hình STOMP /ws nhưng push message/read còn TODO; không khẳng định realtime chạy xuyên suốt (common/ws/WebSocketConfiguration.java:21; features/chat/service/ChatService.java:91, :169).
- Review yêu cầu STUDENT, application FINISHED và chưa có review cho application (features/review/service/impl/ReviewServiceImpl.java:42).
- Backend cho addresses/chatbot/tracking của FE chưa thấy trong lần kiểm kê trước; dịch vụ ngoài **cần xác minh**. Dependency Spring AI không thay cho implementation.

Các đường dẫn FE rút gọn tính từ ../tutorlink-fe/.

## Tên cấu hình

Chỉ tên, không giá trị:
- jwt.secret-key, jwt.expiration, jwt.refreshable-duration, spring.application.name (features/auth/service/JwtService.java:28; features/auth/service/RefreshTokenService.java:17).
- app.cors.allowed-origins (common/security/rest/SecurityConfig.java:107).
- payment.pagination.default-limit, payment.pagination.max-limit, payment.commission-rate (features/payment/service/impl/PaymentServiceImpl.java:67).
- payment.momo.base-url, payment.momo.endpoint-create, payment.momo.endpoint-query, payment.momo.partner-code, payment.momo.access-key, payment.momo.secret-key, payment.ipn-base-url, payment.redirect-url (features/payment/gateway/momo/MomoGateway.java:44).
- cloudinary.cloud-name, cloudinary.api-key, cloudinary.api-secret (common/config/CloudinaryConfig.java:12).
