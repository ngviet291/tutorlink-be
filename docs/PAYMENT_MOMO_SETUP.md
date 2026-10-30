# MoMo Payment Gateway Setup Guide

## 1. Get MoMo Credentials

Register for a MoMo Business account and get your production credentials:
1. Visit: https://business.momo.vn
2. Register and verify your business
3. Get your credentials:
   - Partner Code
   - Access Key
   - Secret Key

## 2. Configure Environment Variables

### Option A: Using `.env` file (Recommended for Development)

The application now uses `spring-dotenv` library to automatically load from `.env` file.

Update the following variables in `.env`:

```env
# MoMo Production Configuration
MOMO_BASE_URL=https://payment.momo.vn
MOMO_PARTNER_CODE=<your-partner-code>
MOMO_ACCESS_KEY=<your-access-key>
MOMO_SECRET_KEY=<your-secret-key>

# Payment Callbacks
PAYMENT_IPN_BASE_URL=http://localhost:8080/api
PAYMENT_REDIRECT_URL=http://localhost:5173/wallet/top-up/result
```

### Option B: Using System Environment Variables (Production)

For production deployment, set these as system environment variables instead of using `.env` file.

## 3. Local Testing with IPN Callbacks

MoMo needs to call your IPN (Instant Payment Notification) endpoint to confirm payment status.

### Using ngrok:

```bash
# Install ngrok: https://ngrok.com/download
# Start your application first
mvnw spring-boot:run

# In another terminal, expose port 8080
ngrok http 8080
```

Update `.env` with ngrok URL:
```env
PAYMENT_IPN_BASE_URL=https://your-ngrok-url.ngrok.io/api
```

Restart the application after changing IPN URL.

## 4. Application Startup

The application can now start **without** real MoMo credentials. Payment features will work once you add real keys.

**Required for payment to work:**
- `MOMO_PARTNER_CODE`
- `MOMO_ACCESS_KEY`
- `MOMO_SECRET_KEY`

**Optional services** (can be empty):
- `CLOUDINARY_*` - for image uploads
- `MAIL_*` - for email notifications
- `GEMINI_API_KEY` - for AI features (disabled by default)
- `REDIS_PASSWORD` - if Redis requires auth

## 5. Testing Payment Flow

### Top-up Request:

```bash
POST /api/payment/top-up
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 50000
}
```

**Note:** Minimum amount is 10,000 VND

### Response:

```json
{
  "code": 1000,
  "message": "Payment initiated successfully",
  "result": {
    "payUrl": "https://payment.momo.vn/pay/...",
    "orderId": "...",
    "amount": 50000
  }
}
```

## 6. Production Deployment Checklist

- [ ] Use real MoMo production credentials
- [ ] Set `PAYMENT_IPN_BASE_URL` to your production domain
- [ ] Set `PAYMENT_REDIRECT_URL` to your frontend production URL
- [ ] Ensure HTTPS is enabled for IPN endpoint
- [ ] Whitelist your production IP with MoMo
- [ ] Test payment flow in MoMo sandbox first
- [ ] Monitor transaction logs for errors
- [ ] Set up proper logging and alerts

## 7. Security Notes

- **NEVER** commit `.env` file to git (already in `.gitignore`)
- Store credentials in secure environment variable management system
- Use different keys for development/staging/production
- Rotate keys periodically
- Monitor for suspicious payment activities

## 8. Troubleshooting

### Application won't start:
- Check if database is running
- Verify `.env` file exists in project root
- Check console logs for specific errors

### Payment returns error:
- Verify MoMo credentials are correct
- Check if IPN URL is accessible from internet
- Verify HMAC signature calculation
- Check MoMo dashboard for error details

### IPN callback not received:
- Verify ngrok is running (for local dev)
- Check if IPN URL is correctly set
- Verify firewall/security groups allow MoMo IPs
- Check application logs for callback errors

## 9. API Endpoints

### Payment Endpoints:
- `POST /api/payment/top-up` - Create top-up transaction
- `POST /api/payment/momo/callback` - MoMo IPN callback
- `GET /api/payment/momo/return` - Payment result redirect
- `GET /api/payment/transactions` - List transactions
- `GET /api/payment/transactions/{id}` - Get transaction details

### Wallet Endpoints:
- `GET /api/payment/wallet` - Get wallet balance
- `GET /api/payment/wallet/transactions` - Get wallet transaction history

## 10. Changes Summary

✅ **Removed:**
- ZaloPay payment gateway
- Mock payment gateway
- Sandbox/test URLs
- BANK_TRANSFER payment method

✅ **Added:**
- `spring-dotenv` for automatic `.env` loading
- Graceful startup without credentials (empty defaults)
- Production MoMo URL configuration
- Comprehensive documentation

✅ **Updated:**
- PaymentMethod enum (WALLET, MOMO only)
- TopUpRequest (no paymentMethod field, min 10,000 VND)
- All payment config organized in `.env`
- Removed duplicate configs from application.properties
- Google GenAI auto-configuration disabled by default
