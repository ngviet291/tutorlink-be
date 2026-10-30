# Hướng Dẫn Test Payment MoMo

## 📋 Checklist Trước Khi Test

### 1. Kiểm tra file .env đã đầy đủ:
```env
✅ Database config (DB_HOST, DB_USER, DB_PASS...)
✅ JWT config (JWT_SECRET, JWT_EXPIRATION...)
✅ Redis config (REDIS_HOST, REDIS_PORT...)
✅ MoMo credentials (MOMO_PARTNER_CODE, MOMO_ACCESS_KEY, MOMO_SECRET_KEY)
✅ Payment URLs (PAYMENT_IPN_BASE_URL, PAYMENT_REDIRECT_URL)
```

### 2. Services đang chạy:
```bash
# PostgreSQL
docker ps | grep postgres

# Redis
docker ps | grep redis

# Hoặc dùng Docker Compose
docker-compose up -d
```

### 3. Restart ứng dụng để load .env mới:
```bash
# Stop ứng dụng cũ (Ctrl+C)
# Start lại
./mvnw spring-boot:run
```

---

## 🧪 Test Flow Payment

### Bước 1: Đăng nhập và lấy JWT Token

```bash
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "tutor@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "token": "eyJhbGc...",
    "refreshToken": "..."
  }
}
```

### Bước 2: Kiểm tra số dư ví hiện tại

```bash
GET http://localhost:8080/api/v1/payments/wallet
Authorization: Bearer <your-token>
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "balance": 0,
    "lastUpdated": "2024-10-30T13:00:00Z"
  }
}
```

### Bước 3: Tạo yêu cầu nạp tiền

```bash
POST http://localhost:8080/api/v1/payments/wallet/top-up
Authorization: Bearer <your-token>
Content-Type: application/json

{
  "amount": 50000
}
```

**Response:**
```json
{
  "code": 1000,
  "message": "Payment initiated successfully",
  "result": {
    "id": "01a18cfb-81fd-756b-8ca0-6dfa2e6ba9f1",
    "type": "TOP_UP",
    "amount": 50000,
    "status": "PENDING",
    "paymentMethod": "MOMO",
    "paymentLink": "https://payment.momo.vn/pay/...",
    "providerOrderId": "TL9a9406d470bf4209b62412f96dace37b",
    "transactionDate": "2024-10-30T13:24:31Z"
  }
}
```

### Bước 4: Mở link thanh toán

Copy `paymentLink` từ response và mở trong browser:
```
https://payment.momo.vn/pay/...
```

**Lưu ý:** Nếu gặp lỗi MoMo "Cấu hình doanh nghiệp không chính xác":
- Credentials không hợp lệ
- Tài khoản MoMo test đã hết hạn
- Cần lấy credentials mới từ https://business.momo.vn

### Bước 5: Thanh toán trên MoMo

1. Chọn phương thức thanh toán (ATM/Visa/MoMo Wallet)
2. Nhập thông tin thẻ
3. Xác nhận thanh toán

### Bước 6: Callback từ MoMo (Tự động)

MoMo sẽ gọi IPN callback:
```
POST http://localhost:8080/api/v1/payments/momo/ipn
```

**Backend sẽ:**
- Verify chữ ký từ MoMo
- Cập nhật trạng thái giao dịch
- Cộng tiền vào ví user

### Bước 7: Kiểm tra kết quả

```bash
GET http://localhost:8080/api/v1/payments/wallet
Authorization: Bearer <your-token>
```

**Nếu thành công:**
```json
{
  "code": 1000,
  "result": {
    "balance": 50000,  // ✅ Đã cộng tiền!
    "lastUpdated": "2024-10-30T13:25:00Z"
  }
}
```

### Bước 8: Xem lịch sử giao dịch

```bash
GET http://localhost:8080/api/v1/payments/transactions
Authorization: Bearer <your-token>
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "data": [
      {
        "id": "01a18cfb-81fd-756b-8ca0-6dfa2e6ba9f1",
        "type": "TOP_UP",
        "amount": 50000,
        "status": "COMPLETED",  // ✅ Đã hoàn thành
        "paymentMethod": "MOMO",
        "providerTransactionId": "3845629437",
        "transactionDate": "2024-10-30T13:24:31Z"
      }
    ],
    "page": 0,
    "size": 10,
    "total": 1
  }
}
```

---

## 🔧 Troubleshooting

### Vấn đề 1: "Gateway unavailable or rejected"

**Nguyên nhân:**
- MoMo credentials không hợp lệ
- Network không kết nối được đến MoMo

**Giải pháp:**
```bash
# Kiểm tra credentials trong .env
cat .env | grep MOMO

# Test kết nối MoMo
curl https://payment.momo.vn
```

### Vấn đề 2: Callback không nhận được (Local dev)

**Nguyên nhân:**
- MoMo không thể gọi đến `http://localhost:8080`

**Giải pháp:** Dùng ngrok để expose local server:
```bash
# Cài ngrok: https://ngrok.com/download
ngrok http 8080

# Cập nhật .env với URL từ ngrok:
PAYMENT_IPN_BASE_URL=https://abc123.ngrok.io/api

# Restart ứng dụng
```

### Vấn đề 3: Ví không cộng tiền

**Nguyên nhân:**
- Callback chưa được xử lý
- Giao dịch vẫn ở trạng thái PENDING

**Giải pháp:** Sync thủ công:
```bash
POST http://localhost:8080/api/v1/payments/transactions/{transactionId}/sync
Authorization: Bearer <your-token>
```

### Vấn đề 4: Invalid signature

**Nguyên nhân:**
- Secret key không đúng
- Raw signature string không đúng thứ tự

**Kiểm tra logs:**
```bash
tail -f logs/application.log | grep MoMo
```

---

## 📝 Test với Postman

1. Import collection từ file `postman_collection.json` (nếu có)
2. Tạo environment với biến:
   - `BASE_URL`: http://localhost:8080/api/v1
   - `TOKEN`: Bearer token từ login

---

## 🎯 Expected Results

| Bước | Endpoint | Status | Kết quả |
|------|----------|--------|---------|
| Login | `/auth/login` | 200 | Có token |
| Get Wallet | `/payments/wallet` | 200 | balance = 0 |
| Top Up | `/payments/wallet/top-up` | 200 | paymentLink có URL |
| After Payment | `/payments/wallet` | 200 | balance = 50000 |
| Transactions | `/payments/transactions` | 200 | Có 1 giao dịch COMPLETED |

---

## ⚠️ Lưu Ý Quan Trọng

1. **Minimum amount:** 10,000 VND
2. **Test credentials:** Chỉ dùng cho môi trường test
3. **Production:** Phải đổi sang credentials production của MoMo
4. **Security:** KHÔNG commit file .env vào git!

---

## 📞 Support

Nếu vẫn gặp lỗi:
1. Check application logs
2. Check MoMo dashboard: https://business.momo.vn
3. Đọc doc MoMo: https://developers.momo.vn
