# Simple MoMo Test Script
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Testing MoMo API Credentials" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# Read .env file
Get-Content .env | ForEach-Object {
    if ($_ -match '^MOMO_PARTNER_CODE=(.+)$') { $script:PARTNER_CODE = $matches[1] }
    if ($_ -match '^MOMO_ACCESS_KEY=(.+)$') { $script:ACCESS_KEY = $matches[1] }
    if ($_ -match '^MOMO_SECRET_KEY=(.+)$') { $script:SECRET_KEY = $matches[1] }
    if ($_ -match '^MOMO_ENDPOINT_CREATE=(.+)$') { $script:ENDPOINT = $matches[1] }
}

Write-Host "`nCredentials loaded:"
Write-Host "Partner Code: $PARTNER_CODE"
Write-Host "Access Key: $($ACCESS_KEY.Substring(0, [Math]::Min(10, $ACCESS_KEY.Length)))..."
Write-Host "Secret Key: $($SECRET_KEY.Substring(0, [Math]::Min(10, $SECRET_KEY.Length)))..."
Write-Host "Endpoint: $ENDPOINT"

# Generate test data
$requestId = [guid]::NewGuid().ToString()
$orderId = "TL" + [guid]::NewGuid().ToString().Replace("-", "")
$amount = 50000

Write-Host "`nTest Transaction:"
Write-Host "Request ID: $requestId"
Write-Host "Order ID: $orderId"
Write-Host "Amount: $amount VND"

# Build signature string manually (avoid & character issues)
$parts = @(
    "accessKey=$ACCESS_KEY",
    "amount=$amount",
    "extraData=",
    "ipnUrl=http://localhost:8080/api/v1/payments/momo/ipn",
    "orderId=$orderId",
    "orderInfo=Test payment",
    "partnerCode=$PARTNER_CODE",
    "redirectUrl=http://localhost:5173/wallet/top-up/result",
    "requestId=$requestId",
    "requestType=payWithATM"
)
$rawSignature = $parts -join '&'

Write-Host "`nRaw Signature:" -ForegroundColor Yellow
Write-Host $rawSignature

# Calculate HMAC-SHA256
$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [Text.Encoding]::UTF8.GetBytes($SECRET_KEY)
$hash = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($rawSignature))
$signature = [BitConverter]::ToString($hash).Replace("-", "").ToLower()

Write-Host "`nSignature: $signature" -ForegroundColor Green

# Build request body
$body = @{
    partnerCode = $PARTNER_CODE
    requestId = $requestId
    amount = $amount
    orderId = $orderId
    orderInfo = "Test payment"
    redirectUrl = "http://localhost:5173/wallet/top-up/result"
    ipnUrl = "http://localhost:8080/api/v1/payments/momo/ipn"
    requestType = "payWithATM"
    extraData = ""
    autoCapture = $true
    lang = "vi"
    signature = $signature
} | ConvertTo-Json -Compress

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "Sending request to MoMo..." -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

try {
    $headers = @{
        "Content-Type" = "application/json"
        "User-Agent" = "TutorLink/1.0"
    }
    
    $response = Invoke-RestMethod -Uri $ENDPOINT -Method Post -Body $body -Headers $headers
    
    Write-Host "`nResponse:" -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 5
    
    Write-Host "`n============================================" -ForegroundColor Cyan
    if ($response.resultCode -eq 0) {
        Write-Host "SUCCESS! Credentials are valid!" -ForegroundColor Green
        Write-Host "Payment URL: $($response.payUrl)" -ForegroundColor Green
    } else {
        Write-Host "FAILED! Result Code: $($response.resultCode)" -ForegroundColor Red
        Write-Host "Message: $($response.message)" -ForegroundColor Red
        Write-Host "`nError Code Meanings:" -ForegroundColor Yellow
        Write-Host "  9  - Transaction denied"
        Write-Host "  10 - System error"
        Write-Host "  11 - Wrong access key"
        Write-Host "  12 - Wrong signature"
        Write-Host "  13 - Merchant config wrong or account inactive (YOUR ERROR!)"
        Write-Host "  20 - Wrong format"
        Write-Host "  21 - Invalid amount"
        
        if ($response.resultCode -eq 13) {
            Write-Host "`nSuggestions for Error 13:" -ForegroundColor Cyan
            Write-Host "1. Check MoMo Business Portal if account is active"
            Write-Host "2. Verify you're using Production credentials (not test)"
            Write-Host "3. Contact MoMo support: api.support@momo.vn"
            Write-Host "4. Check if IP needs to be whitelisted"
        }
    }
    Write-Host "============================================" -ForegroundColor Cyan
    
} catch {
    Write-Host "`nERROR: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        Write-Host "Response Body: $errorBody" -ForegroundColor Yellow
    }
}
