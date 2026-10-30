# PowerShell script to test MoMo API credentials

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "🔵 Testing MoMo API Credentials" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Load .env file
$envFile = Get-Content .env
$env:MOMO_PARTNER_CODE = ($envFile | Where-Object { $_ -match "^MOMO_PARTNER_CODE=" }) -replace "MOMO_PARTNER_CODE=", ""
$env:MOMO_ACCESS_KEY = ($envFile | Where-Object { $_ -match "^MOMO_ACCESS_KEY=" }) -replace "MOMO_ACCESS_KEY=", ""
$env:MOMO_SECRET_KEY = ($envFile | Where-Object { $_ -match "^MOMO_SECRET_KEY=" }) -replace "MOMO_SECRET_KEY=", ""
$env:MOMO_ENDPOINT_CREATE = ($envFile | Where-Object { $_ -match "^MOMO_ENDPOINT_CREATE=" }) -replace "MOMO_ENDPOINT_CREATE=", ""

# Generate test data
$requestId = [guid]::NewGuid().ToString()
$orderId = "TL" + [guid]::NewGuid().ToString().Replace("-", "")
$amount = 50000
$orderInfo = "Test payment"
$extraData = ""
$ipnUrl = "http://localhost:8080/api/v1/payments/momo/ipn"
$redirectUrl = "http://localhost:5173/wallet/top-up/result"
$requestType = "payWithATM"

Write-Host "Partner Code: $($env:MOMO_PARTNER_CODE)"
Write-Host "Access Key: $($env:MOMO_ACCESS_KEY.Substring(0, 8))..."
Write-Host "Secret Key: $($env:MOMO_SECRET_KEY.Substring(0, 8))..."
Write-Host ""
Write-Host "Request ID: $requestId"
Write-Host "Order ID: $orderId"
Write-Host ""

# Build raw signature (MUST be in alphabetical order)
$rawSignature = "accessKey=$($env:MOMO_ACCESS_KEY)" + "&amount=$amount" + "&extraData=$extraData" + "&ipnUrl=$ipnUrl" + "&orderId=$orderId" + "&orderInfo=$orderInfo" + "&partnerCode=$($env:MOMO_PARTNER_CODE)" + "&redirectUrl=$redirectUrl" + "&requestId=$requestId" + "&requestType=$requestType"

Write-Host "Raw Signature String:" -ForegroundColor Yellow
Write-Host $rawSignature
Write-Host ""

# Generate HMAC SHA256 signature
$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [Text.Encoding]::UTF8.GetBytes($env:MOMO_SECRET_KEY)
$signatureBytes = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($rawSignature))
$signature = [BitConverter]::ToString($signatureBytes).Replace("-", "").ToLower()

Write-Host "Signature: $signature" -ForegroundColor Green
Write-Host ""

# Build JSON request
$body = @{
    partnerCode = $env:MOMO_PARTNER_CODE
    requestId = $requestId
    amount = $amount
    orderId = $orderId
    orderInfo = $orderInfo
    redirectUrl = $redirectUrl
    ipnUrl = $ipnUrl
    requestType = $requestType
    extraData = $extraData
    autoCapture = $true
    lang = "vi"
    signature = $signature
} | ConvertTo-Json

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "📤 Sending request to MoMo..." -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Send request to MoMo
try {
    $response = Invoke-RestMethod -Uri $env:MOMO_ENDPOINT_CREATE -Method Post -Body $body -ContentType "application/json"
    
    Write-Host "Response:" -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 10
    Write-Host ""
    
    Write-Host "============================================" -ForegroundColor Cyan
    if ($response.resultCode -eq 0) {
        Write-Host "✅ SUCCESS! MoMo credentials are valid!" -ForegroundColor Green
        Write-Host "Payment URL: $($response.payUrl)" -ForegroundColor Green
    } else {
        Write-Host "❌ FAILED! Result Code: $($response.resultCode)" -ForegroundColor Red
        Write-Host "Message: $($response.message)" -ForegroundColor Red
        Write-Host ""
        Write-Host "Common Error Codes:" -ForegroundColor Yellow
        Write-Host "  9 - Transaction denied"
        Write-Host "  10 - System error"
        Write-Host "  11 - Wrong access key"
        Write-Host "  12 - Wrong signature"
        Write-Host "  13 - Wrong merchant configuration or account inactive"
        Write-Host "  20 - Wrong format"
        Write-Host "  21 - Amount is invalid"
    }
    Write-Host "============================================" -ForegroundColor Cyan
    
} catch {
    Write-Host "❌ ERROR: $_" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}
