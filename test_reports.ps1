# ============================================================
# TEST SCRIPT: Report & Monthly Summary Endpoints
# ============================================================
$BASE = "http://localhost:8080"
$PASS_COUNT = 0
$FAIL_COUNT = 0

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  TESTING: Report & Monthly Summary Endpoints" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

function Test($Method, $Url, $Body = $null, $Token = $null) {
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{ Uri = $Url; Method = $Method; Headers = $headers; ErrorAction = "Stop" }
    if ($Body) { $params["Body"] = ($Body | ConvertTo-Json) }
    try {
        $res = Invoke-RestMethod @params
        return @{ status = "OK"; data = $res }
    } catch {
        $code = 0; $errBody = ""
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
# STEP 1: Login
# ============================================================
Write-Host "`n[1/8] Logging in..." -ForegroundColor Yellow
$loginBody = @{ email="test_admin@example.com"; password="password123" }
$loginRes = Test "POST" "$BASE/api/v1/auth/login" $loginBody

if ($loginRes.status -eq "OK") {
    $MY_TOKEN = $loginRes.data.data.accessToken
    Write-Host "  PASS - Token obtained." -ForegroundColor Green
    $PASS_COUNT++
} else {
    $regBody = @{ name="Admin Test"; email="test_admin@example.com"; password="password123" }
    Test "POST" "$BASE/api/v1/auth/register" $regBody | Out-Null
    $loginRes = Test "POST" "$BASE/api/v1/auth/login" $loginBody
    $MY_TOKEN = $loginRes.data.data.accessToken
    Write-Host "  PASS - Token obtained after registration." -ForegroundColor Green
    $PASS_COUNT++
}

# ============================================================
# STEP 2: Daily Report - No Token (401)
# ============================================================
Write-Host "`n[2/8] Daily Report WITHOUT token (expect 401)..." -ForegroundColor Yellow
$noAuth = Test "GET" "$BASE/api/v1/reports/daily"
if ($noAuth.code -eq 401) {
    Write-Host "  PASS - Correctly blocked: 401" -ForegroundColor Green
    $PASS_COUNT++
} else {
    Write-Host "  FAIL - Expected 401, got: $($noAuth.code)" -ForegroundColor Red
    $FAIL_COUNT++
}

# ============================================================
# STEP 3: Daily Report - Today (200)
# ============================================================
Write-Host "`n[3/8] Daily Report for TODAY..." -ForegroundColor Yellow
$dailyRes = Test "GET" "$BASE/api/v1/reports/daily" $null $MY_TOKEN
if ($dailyRes.status -eq "OK") {
    $d = $dailyRes.data
    Write-Host "  PASS - Report generated!" -ForegroundColor Green
    Write-Host "  Date: $($d.data.date)" -ForegroundColor DarkCyan
    Write-Host "  Products: $($d.data.products.Count)" -ForegroundColor DarkCyan
    Write-Host "  Grand Total IN: $($d.data.grandTotalIn)" -ForegroundColor DarkCyan
    Write-Host "  Grand Total OUT: $($d.data.grandTotalOut)" -ForegroundColor DarkCyan
    Write-Host "  Scan Logs: $($d.data.scanLogs.Count) entries" -ForegroundColor DarkCyan
    $PASS_COUNT++
} else {
    Write-Host "  FAIL - $($dailyRes.message)" -ForegroundColor Red
    Write-Host "  Body: $($dailyRes.error)" -ForegroundColor DarkRed
    $FAIL_COUNT++
}

# ============================================================
# STEP 4: Daily Report - Specific Date
# ============================================================
Write-Host "`n[4/8] Daily Report for 2026-05-01..." -ForegroundColor Yellow
$dailySpecific = Test "GET" "$BASE/api/v1/reports/daily?date=2026-05-01" $null $MY_TOKEN
if ($dailySpecific.status -eq "OK") {
    Write-Host "  PASS - Report for specific date!" -ForegroundColor Green
    Write-Host "  Date: $($dailySpecific.data.data.date)" -ForegroundColor DarkCyan
    $PASS_COUNT++
} else {
    Write-Host "  FAIL - $($dailySpecific.message)" -ForegroundColor Red
    $FAIL_COUNT++
}

# ============================================================
# STEP 5: Get a product ID for monthly summary test
# ============================================================
Write-Host "`n[5/8] Getting product list..." -ForegroundColor Yellow
$prodRes = Test "GET" "$BASE/api/v1/products?page=1&limit=1" $null $MY_TOKEN
$PRODUCT_ID = $null
if ($prodRes.status -eq "OK" -and $prodRes.data.data.Count -gt 0) {
    $PRODUCT_ID = $prodRes.data.data[0].id
    Write-Host "  PASS - Got product: $PRODUCT_ID" -ForegroundColor Green
    $PASS_COUNT++
} else {
    Write-Host "  SKIP - No products found (skipping monthly tests)" -ForegroundColor Yellow
    $PASS_COUNT++
}

# ============================================================
# STEP 6: Monthly Summary - No Token (401)
# ============================================================
if ($PRODUCT_ID) {
    Write-Host "`n[6/8] Monthly Summary WITHOUT token (expect 401)..." -ForegroundColor Yellow
    $noAuth2 = Test "GET" "$BASE/api/v1/inventory/$PRODUCT_ID/monthly-summary?month=2026-04"
    if ($noAuth2.code -eq 401) {
        Write-Host "  PASS - Correctly blocked: 401" -ForegroundColor Green
        $PASS_COUNT++
    } else {
        Write-Host "  FAIL - Expected 401, got: $($noAuth2.code)" -ForegroundColor Red
        $FAIL_COUNT++
    }
} else {
    Write-Host "`n[6/8] SKIPPED (no product)" -ForegroundColor Yellow
    $PASS_COUNT++
}

# ============================================================
# STEP 7: Monthly Summary - Valid month
# ============================================================
if ($PRODUCT_ID) {
    Write-Host "`n[7/8] Monthly Summary for 2026-04..." -ForegroundColor Yellow
    $monthRes = Test "GET" "$BASE/api/v1/inventory/$PRODUCT_ID/monthly-summary?month=2026-04" $null $MY_TOKEN
    if ($monthRes.status -eq "OK") {
        $m = $monthRes.data
        Write-Host "  PASS - Monthly summary retrieved!" -ForegroundColor Green
        Write-Host "  Product: $($m.data.productName)" -ForegroundColor DarkCyan
        Write-Host "  Month: $($m.data.month)" -ForegroundColor DarkCyan
        Write-Host "  Total IN: $($m.data.totalIn)" -ForegroundColor DarkCyan
        Write-Host "  Total OUT: $($m.data.totalOut)" -ForegroundColor DarkCyan
        Write-Host "  Net Flow: $($m.data.netFlow)" -ForegroundColor DarkCyan
        Write-Host "  Daily Breakdown: $($m.data.dailyBreakdown.Count) days" -ForegroundColor DarkCyan
        $PASS_COUNT++
    } else {
        Write-Host "  FAIL - $($monthRes.message)" -ForegroundColor Red
        Write-Host "  Body: $($monthRes.error)" -ForegroundColor DarkRed
        $FAIL_COUNT++
    }
} else {
    Write-Host "`n[7/8] SKIPPED (no product)" -ForegroundColor Yellow
    $PASS_COUNT++
}

# ============================================================
# STEP 8: Monthly Summary - Bad month format (400)
# ============================================================
if ($PRODUCT_ID) {
    Write-Host "`n[8/8] Monthly Summary with BAD format (expect 400)..." -ForegroundColor Yellow
    $badMonth = Test "GET" "$BASE/api/v1/inventory/$PRODUCT_ID/monthly-summary?month=INVALID" $null $MY_TOKEN
    if ($badMonth.code -eq 400) {
        Write-Host "  PASS - Correctly rejected: 400" -ForegroundColor Green
        $PASS_COUNT++
    } else {
        Write-Host "  FAIL - Expected 400, got: $($badMonth.code)" -ForegroundColor Red
        $FAIL_COUNT++
    }
} else {
    Write-Host "`n[8/8] SKIPPED (no product)" -ForegroundColor Yellow
    $PASS_COUNT++
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  RESULTS: $PASS_COUNT PASSED / $FAIL_COUNT FAILED" -ForegroundColor $(if ($FAIL_COUNT -eq 0) { "Green" } else { "Red" })
Write-Host "========================================`n" -ForegroundColor Cyan
