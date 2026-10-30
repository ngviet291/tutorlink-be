#!/bin/bash
# Script test MoMo API credentials

# Load .env variables
source .env

# Generate test data
REQUEST_ID=$(uuidgen)
ORDER_ID="TL$(uuidgen | tr -d '-')"
AMOUNT="50000"
ORDER_INFO="Test payment"
EXTRA_DATA=""
IPN_URL="http://localhost:8080/api/v1/payments/momo/ipn"
REDIRECT_URL="http://localhost:5173/wallet/top-up/result"
REQUEST_TYPE="payWithATM"

# Build raw signature (MUST be in alphabetical order)
RAW_SIGNATURE="accessKey=${MOMO_ACCESS_KEY}&amount=${AMOUNT}&extraData=${EXTRA_DATA}&ipnUrl=${IPN_URL}&orderId=${ORDER_ID}&orderInfo=${ORDER_INFO}&partnerCode=${MOMO_PARTNER_CODE}&redirectUrl=${REDIRECT_URL}&requestId=${REQUEST_ID}&requestType=${REQUEST_TYPE}"

# Generate HMAC SHA256 signature
SIGNATURE=$(echo -n "$RAW_SIGNATURE" | openssl dgst -sha256 -hmac "$MOMO_SECRET_KEY" | cut -d ' ' -f 2)

# Build JSON request
JSON=$(cat <<EOF
{
  "partnerCode": "${MOMO_PARTNER_CODE}",
  "requestId": "${REQUEST_ID}",
  "amount": ${AMOUNT},
  "orderId": "${ORDER_ID}",
  "orderInfo": "${ORDER_INFO}",
  "redirectUrl": "${REDIRECT_URL}",
  "ipnUrl": "${IPN_URL}",
  "requestType": "${REQUEST_TYPE}",
  "extraData": "${EXTRA_DATA}",
  "autoCapture": true,
  "lang": "vi",
  "signature": "${SIGNATURE}"
}
EOF
)

echo "============================================"
echo "🔵 Testing MoMo API Credentials"
echo "============================================"
echo ""
echo "Partner Code: ${MOMO_PARTNER_CODE}"
echo "Access Key: ${MOMO_ACCESS_KEY:0:8}..."
echo "Secret Key: ${MOMO_SECRET_KEY:0:8}..."
echo ""
echo "Request ID: ${REQUEST_ID}"
echo "Order ID: ${ORDER_ID}"
echo ""
echo "Raw Signature String:"
echo "${RAW_SIGNATURE}"
echo ""
echo "Signature: ${SIGNATURE}"
echo ""
echo "============================================"
echo "📤 Sending request to MoMo..."
echo "============================================"
echo ""

# Send request to MoMo
RESPONSE=$(curl -s -X POST "${MOMO_ENDPOINT_CREATE}" \
  -H "Content-Type: application/json" \
  -d "$JSON")

echo "Response:"
echo "$RESPONSE" | jq .

# Parse result code
RESULT_CODE=$(echo "$RESPONSE" | jq -r '.resultCode')
MESSAGE=$(echo "$RESPONSE" | jq -r '.message')

echo ""
echo "============================================"
if [ "$RESULT_CODE" = "0" ]; then
  echo "✅ SUCCESS! MoMo credentials are valid!"
  echo "Payment URL: $(echo "$RESPONSE" | jq -r '.payUrl')"
else
  echo "❌ FAILED! Result Code: ${RESULT_CODE}"
  echo "Message: ${MESSAGE}"
  echo ""
  echo "Common Error Codes:"
  echo "  9 - Transaction denied"
  echo "  10 - System error"
  echo "  11 - Wrong access key"
  echo "  12 - Wrong signature"
  echo "  13 - Wrong merchant configuration or account inactive"
  echo "  20 - Wrong format"
  echo "  21 - Amount is invalid"
fi
echo "============================================"
