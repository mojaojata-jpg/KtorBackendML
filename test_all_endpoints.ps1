# ============================================================
# FULL ENDPOINT TESTING SCRIPT - Ktor RFID Stock Backend
# ============================================================

$BASE_URL = "http://localhost:8080/api/v1"
$TOKEN = ""
$PRODUCT_ID = ""
$PASS_COUNT = 0
$FAIL_COUNT = 0
$TOTAL_COUNT = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [string]$Body = $null,
        [int[]]$ExpectedCodes = @(200),
        [switch]$UseToken,
        [switch]$ShowBody
    )

    $script:TOTAL_COUNT++
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host "TEST #$($script:TOTAL_COUNT): $Name" -ForegroundColor Yellow
    Write-Host "  $Method $Url" -ForegroundColor Gray

    $headers = @{ "Content-Type" = "application/json" }
    if ($UseToken -and $script:TOKEN) {
        $headers["Authorization"] = "Bearer $($script:TOKEN)"
    }

    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $headers
            ErrorAction = "Stop"
        }
        if ($Body) {
            $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($Body)
            Write-Host "  BODY: $Body" -ForegroundColor DarkGray
        }

        $response = Invoke-WebRequest @params
        $statusCode = $response.StatusCode
        $content = $response.Content

        if ($ExpectedCodes -contains $statusCode) {
            Write-Host "  RESULT: PASS (HTTP $statusCode)" -ForegroundColor Green
            $script:PASS_COUNT++
        } else {
            Write-Host "  RESULT: FAIL (HTTP $statusCode, expected $($ExpectedCodes -join '/'))" -ForegroundColor Red
            $script:FAIL_COUNT++
        }

        if ($ShowBody) {
            $json = $content | ConvertFrom-Json | ConvertTo-Json -Depth 5
            Write-Host "  RESPONSE:" -ForegroundColor DarkCyan
            Write-Host $json -ForegroundColor DarkGray
        }

        return $content
    }
    catch {
        $statusCode = 0
        $errorBody = ""
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errorBody = $reader.ReadToEnd()
        }

        if ($ExpectedCodes -contains $statusCode) {
            Write-Host "  RESULT: PASS (HTTP $statusCode - Expected Error)" -ForegroundColor Green
            $script:PASS_COUNT++
        } else {
            Write-Host "  RESULT: FAIL (HTTP $statusCode)" -ForegroundColor Red
            Write-Host "  ERROR: $_" -ForegroundColor Red
            if ($errorBody) { Write-Host "  BODY: $errorBody" -ForegroundColor DarkRed }
            $script:FAIL_COUNT++
        }
        return $errorBody
    }
}

Write-Host ""
Write-Host "########################################################" -ForegroundColor Magenta
Write-Host "#    KTOR RFID STOCK BACKEND - FULL ENDPOINT TESTING   #" -ForegroundColor Magenta
Write-Host "########################################################" -ForegroundColor Magenta
Write-Host "  Base URL: $BASE_URL"
Write-Host "  Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

# ============================================================
# 1. AUTH ENDPOINTS
# ============================================================
Write-Host ""
Write-Host "========== AUTH ENDPOINTS ==========" -ForegroundColor Magenta

# Register
$registerBody = @{
    name = "Test Admin"
    email = "testadmin_$(Get-Random -Maximum 99999)@test.com"
    password = "password123"
} | ConvertTo-Json

$regResult = Test-Endpoint -Name "POST /auth/register" `
    -Method "POST" `
    -Url "$BASE_URL/auth/register" `
    -Body $registerBody `
    -ExpectedCodes @(200, 201, 409) `
    -ShowBody

# Login
$loginEmail = ($registerBody | ConvertFrom-Json).email
$loginBody = @{
    email = $loginEmail
    password = "password123"
} | ConvertTo-Json

$loginResult = Test-Endpoint -Name "POST /auth/login" `
    -Method "POST" `
    -Url "$BASE_URL/auth/login" `
    -Body $loginBody `
    -ExpectedCodes @(200) `
    -ShowBody

try {
    $loginData = $loginResult | ConvertFrom-Json
    if ($loginData.data -and $loginData.data.token) {
        $TOKEN = $loginData.data.token
    } elseif ($loginData.token) {
        $TOKEN = $loginData.token
    }
    Write-Host "  TOKEN SAVED: $($TOKEN.Substring(0, [Math]::Min(30, $TOKEN.Length)))..." -ForegroundColor Green
} catch {
    Write-Host "  WARNING: Could not extract token." -ForegroundColor Yellow
}

# Login with invalid credentials
$badLoginBody = @{
    email = "nonexistent@test.com"
    password = "wrongpassword"
} | ConvertTo-Json

Test-Endpoint -Name "POST /auth/login (INVALID)" `
    -Method "POST" `
    -Url "$BASE_URL/auth/login" `
    -Body $badLoginBody `
    -ExpectedCodes @(401, 400)

# ============================================================
# 2. PRODUCT ENDPOINTS (CRUD)
# ============================================================
Write-Host ""
Write-Host "========== PRODUCT ENDPOINTS ==========" -ForegroundColor Magenta

# CREATE Product
$productBody = @{
    name = "Tepung Terigu Test"
    code = "TPG-TEST-$(Get-Random -Maximum 99999)"
    unitLabel = "kg"
    minStockThreshold = 10
    description = "Tepung terigu untuk testing"
} | ConvertTo-Json

$createResult = Test-Endpoint -Name "POST /products (CREATE)" `
    -Method "POST" `
    -Url "$BASE_URL/products" `
    -Body $productBody `
    -ExpectedCodes @(200, 201) `
    -UseToken `
    -ShowBody

try {
    $prodData = $createResult | ConvertFrom-Json
    if ($prodData.data -and $prodData.data.id) {
        $PRODUCT_ID = $prodData.data.id
    } elseif ($prodData.id) {
        $PRODUCT_ID = $prodData.id
    }
    Write-Host "  PRODUCT_ID SAVED: $PRODUCT_ID" -ForegroundColor Green
} catch {
    Write-Host "  WARNING: Could not extract product ID." -ForegroundColor Yellow
}

# GET All Products
Test-Endpoint -Name "GET /products (LIST ALL)" `
    -Method "GET" `
    -Url "$BASE_URL/products" `
    -ExpectedCodes @(200) `
    -UseToken `
    -ShowBody

# GET Product by ID
if ($PRODUCT_ID) {
    Test-Endpoint -Name "GET /products/{id}" `
        -Method "GET" `
        -Url "$BASE_URL/products/$PRODUCT_ID" `
        -ExpectedCodes @(200) `
        -UseToken `
        -ShowBody
}

# UPDATE Product
if ($PRODUCT_ID) {
    $updateBody = @{
        name = "Tepung Terigu UPDATED"
        code = ($productBody | ConvertFrom-Json).code
        unitLabel = "kg"
        minStockThreshold = 15
        description = "Updated description"
    } | ConvertTo-Json

    Test-Endpoint -Name "PUT /products/{id} (UPDATE)" `
        -Method "PUT" `
        -Url "$BASE_URL/products/$PRODUCT_ID" `
        -Body $updateBody `
        -ExpectedCodes @(200) `
        -UseToken `
        -ShowBody
}

# GET Product by invalid ID
Test-Endpoint -Name "GET /products/{invalidId}" `
    -Method "GET" `
    -Url "$BASE_URL/products/00000000-0000-0000-0000-000000000000" `
    -ExpectedCodes @(404, 400) `
    -UseToken

# CREATE without token (should fail)
Test-Endpoint -Name "POST /products (NO TOKEN - SHOULD FAIL)" `
    -Method "POST" `
    -Url "$BASE_URL/products" `
    -Body $productBody `
    -ExpectedCodes @(401)

# ============================================================
# 3. INVENTORY ENDPOINTS
# ============================================================
Write-Host ""
Write-Host "========== INVENTORY ENDPOINTS ==========" -ForegroundColor Magenta

# Register RFID Tag
if ($PRODUCT_ID) {
    $tagBody = @{
        tagUid = "TAG-TEST-$(Get-Random -Maximum 99999)"
        productId = $PRODUCT_ID
    } | ConvertTo-Json

    Test-Endpoint -Name "POST /inventory/register-tag" `
        -Method "POST" `
        -Url "$BASE_URL/inventory/register-tag" `
        -Body $tagBody `
        -ExpectedCodes @(200, 201) `
        -UseToken `
        -ShowBody
}

# Scan IN
if ($PRODUCT_ID) {
    $scanInBody = @{
        tag_uid = ($tagBody | ConvertFrom-Json).tagUid
        event_type = "IN"
    } | ConvertTo-Json

    Test-Endpoint -Name "POST /inventory/scan (IN)" `
        -Method "POST" `
        -Url "$BASE_URL/inventory/scan" `
        -Body $scanInBody `
        -ExpectedCodes @(200, 201) `
        -ShowBody
}

# Scan OUT
if ($PRODUCT_ID) {
    $scanOutBody = @{
        tag_uid = ($tagBody | ConvertFrom-Json).tagUid
        event_type = "OUT"
    } | ConvertTo-Json

    Test-Endpoint -Name "POST /inventory/scan (OUT)" `
        -Method "POST" `
        -Url "$BASE_URL/inventory/scan" `
        -Body $scanOutBody `
        -ExpectedCodes @(200, 201) `
        -ShowBody
}

# Dashboard
Test-Endpoint -Name "GET /inventory/dashboard" `
    -Method "GET" `
    -Url "$BASE_URL/inventory/dashboard" `
    -ExpectedCodes @(200) `
    -UseToken `
    -ShowBody

# History per product
if ($PRODUCT_ID) {
    Test-Endpoint -Name "GET /inventory/{productId}/history" `
        -Method "GET" `
        -Url "$BASE_URL/inventory/$PRODUCT_ID/history" `
        -ExpectedCodes @(200) `
        -UseToken `
        -ShowBody
}

# Scan with unregistered tag (should fail)
$badScanBody = @{
    tag_uid = "FAKE-TAG-DOESNOTEXIST"
    event_type = "IN"
} | ConvertTo-Json

Test-Endpoint -Name "POST /inventory/scan (UNREGISTERED TAG)" `
    -Method "POST" `
    -Url "$BASE_URL/inventory/scan" `
    -Body $badScanBody `
    -ExpectedCodes @(400, 404)

# ============================================================
# 4. CHART & AGGREGATION ENDPOINTS (NEW!)
# ============================================================
Write-Host ""
Write-Host "========== CHART & AGGREGATION ENDPOINTS (NEW) ==========" -ForegroundColor Magenta

# Manual Aggregation Trigger
Test-Endpoint -Name "POST /inventory/aggregate/manual (TODAY)" `
    -Method "POST" `
    -Url "$BASE_URL/inventory/aggregate/manual" `
    -ExpectedCodes @(200) `
    -ShowBody

# Manual Aggregation with specific date
Test-Endpoint -Name "POST /inventory/aggregate/manual?date=2026-04-28" `
    -Method "POST" `
    -Url "$BASE_URL/inventory/aggregate/manual?date=2026-04-28" `
    -ExpectedCodes @(200) `
    -ShowBody

# Manual Aggregation with invalid date
Test-Endpoint -Name "POST /inventory/aggregate/manual?date=INVALID" `
    -Method "POST" `
    -Url "$BASE_URL/inventory/aggregate/manual?date=INVALID" `
    -ExpectedCodes @(400) `
    -ShowBody

# Chart Data for product
if ($PRODUCT_ID) {
    Test-Endpoint -Name "GET /inventory/chart-data/{productId}" `
        -Method "GET" `
        -Url "$BASE_URL/inventory/chart-data/$PRODUCT_ID" `
        -ExpectedCodes @(200) `
        -ShowBody
}

# Chart Data for non-existent product
Test-Endpoint -Name "GET /inventory/chart-data/{invalidId}" `
    -Method "GET" `
    -Url "$BASE_URL/inventory/chart-data/00000000-0000-0000-0000-000000000000" `
    -ExpectedCodes @(404, 400) `
    -ShowBody

# ============================================================
# 5. CLEANUP - DELETE Product
# ============================================================
Write-Host ""
Write-Host "========== CLEANUP ==========" -ForegroundColor Magenta

if ($PRODUCT_ID) {
    Test-Endpoint -Name "DELETE /products/{id}" `
        -Method "DELETE" `
        -Url "$BASE_URL/products/$PRODUCT_ID" `
        -ExpectedCodes @(200, 204) `
        -UseToken `
        -ShowBody
}

# ============================================================
# RESULTS SUMMARY
# ============================================================
Write-Host ""
Write-Host "########################################################" -ForegroundColor Magenta
Write-Host "#                  TEST RESULTS SUMMARY                #" -ForegroundColor Magenta
Write-Host "########################################################" -ForegroundColor Magenta
Write-Host ""
Write-Host "  TOTAL : $TOTAL_COUNT" -ForegroundColor White
Write-Host "  PASS  : $PASS_COUNT" -ForegroundColor Green
Write-Host "  FAIL  : $FAIL_COUNT" -ForegroundColor Red
Write-Host ""

if ($FAIL_COUNT -eq 0) {
    Write-Host "  ALL TESTS PASSED! BACKEND IS SOLID!" -ForegroundColor Green
} else {
    Write-Host "  SOME TESTS FAILED! CHECK ABOVE FOR DETAILS." -ForegroundColor Red
}

Write-Host ""
