# ============================================================
# TEST SCRIPT: Admin Aggregate Sync Endpoint
# ============================================================
$BASE = "http://localhost:8080"
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  TESTING: POST /api/admin/inventory/aggregate/sync" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

function Test($Method, $Url, $Body = $null, $Token = $null) {
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    
    $params = @{
        Uri = $Url
        Method = $Method
        Headers = $headers
        ErrorAction = "Stop"
    }
    if ($Body) { $params["Body"] = ($Body | ConvertTo-Json) }
    
    try {
        $res = Invoke-RestMethod @params
        return @{ status = "OK"; data = $res }
    } catch {
        $errBody = ""
        $code = 0
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errBody = $reader.ReadToEnd()
        }
        return @{ status = "ERROR"; code = $code; error = $errBody; message = $_.Exception.Message }
    }
}

# ============================================================
# STEP 1: Login to get JWT Token
# ============================================================
Write-Host "`n[1/5] Logging in..." -ForegroundColor Yellow
$loginBody = @{ email="test_admin@example.com"; password="password123" }
$loginRes = Test "POST" "$BASE/api/v1/auth/login" $loginBody

if ($loginRes.status -eq "OK") {
    $MY_TOKEN = $loginRes.data.data.accessToken
    Write-Host "  Token obtained." -ForegroundColor Green
} else {
    Write-Host "  Login failed! Trying to register first..." -ForegroundColor Red
    $regBody = @{ name="Admin Test"; email="test_admin@example.com"; password="password123" }
    Test "POST" "$BASE/api/v1/auth/register" $regBody | Out-Null
    $loginRes = Test "POST" "$BASE/api/v1/auth/login" $loginBody
    $MY_TOKEN = $loginRes.data.data.accessToken
    Write-Host "  Token obtained after registration." -ForegroundColor Green
}

# ============================================================
# STEP 2: Test WITHOUT Token (should fail with 401)
# ============================================================
Write-Host "`n[2/5] Testing sync WITHOUT token (expect 401)..." -ForegroundColor Yellow
$noAuthRes = Test "POST" "$BASE/api/admin/inventory/aggregate/sync"

if ($noAuthRes.code -eq 401) {
    Write-Host "  PASS - Correctly blocked: 401 Unauthorized" -ForegroundColor Green
} else {
    Write-Host "  FAIL - Expected 401, got: $($noAuthRes.code) $($noAuthRes.message)" -ForegroundColor Red
}

# ============================================================
# STEP 3: Test WITH Token for today (should succeed)
# ============================================================
Write-Host "`n[3/5] Testing sync WITH token (today)..." -ForegroundColor Yellow
$syncRes = Test "POST" "$BASE/api/admin/inventory/aggregate/sync" $null $MY_TOKEN

if ($syncRes.status -eq "OK") {
    $syncData = $syncRes.data
    Write-Host "  PASS - Sync succeeded!" -ForegroundColor Green
    Write-Host "  Message: $($syncData.message)" -ForegroundColor DarkCyan
    Write-Host "  Date: $($syncData.data.date)" -ForegroundColor DarkCyan
    Write-Host "  Products Synced: $($syncData.data.productsSynced)" -ForegroundColor DarkCyan
} else {
    Write-Host "  FAIL - $($syncRes.message)" -ForegroundColor Red
    Write-Host "  Body: $($syncRes.error)" -ForegroundColor DarkRed
}

# ============================================================
# STEP 4: Test WITH Token for specific date
# ============================================================
Write-Host "`n[4/5] Testing sync WITH token (specific date 2026-05-01)..." -ForegroundColor Yellow
$syncDateRes = Test "POST" "$BASE/api/admin/inventory/aggregate/sync?date=2026-05-01" $null $MY_TOKEN

if ($syncDateRes.status -eq "OK") {
    $syncData2 = $syncDateRes.data
    Write-Host "  PASS - Sync for specific date succeeded!" -ForegroundColor Green
    Write-Host "  Message: $($syncData2.message)" -ForegroundColor DarkCyan
    Write-Host "  Date: $($syncData2.data.date)" -ForegroundColor DarkCyan
    Write-Host "  Products Synced: $($syncData2.data.productsSynced)" -ForegroundColor DarkCyan
} else {
    Write-Host "  FAIL - $($syncDateRes.message)" -ForegroundColor Red
}

# ============================================================
# STEP 5: Test with INVALID date format
# ============================================================
Write-Host "`n[5/5] Testing sync with INVALID date format (expect 400)..." -ForegroundColor Yellow
$badDateRes = Test "POST" "$BASE/api/admin/inventory/aggregate/sync?date=NOT-A-DATE" $null $MY_TOKEN

if ($badDateRes.code -eq 400) {
    Write-Host "  PASS - Correctly rejected: 400 Bad Request" -ForegroundColor Green
} else {
    Write-Host "  FAIL - Expected 400, got: $($badDateRes.code)" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  ALL SYNC ENDPOINT TESTS COMPLETED" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan
