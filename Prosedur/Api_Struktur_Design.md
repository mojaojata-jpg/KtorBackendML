a# 📘 API Design Specification - IoT Stock Prediction System

## 1. API Principles

* RESTful
* JSON-based
* Stateless
* Versioned (recommended: `/api/v1`)
* Secure (JWT for protected endpoints)
* Consistent response format

---

## 2. Base URL

```text
/api/v1
```

---

## 3. Standard Response Format

### Success Response

```json
{
  "success": true,
  "data": {},
  "message": "Request successful"
}
```

---

### Error Response

```json
{
  "success": false,
  "message": "Error message",
  "error_code": "ERROR_CODE"
}
```

---

## 4. Authentication

### 4.1 Login

```http
POST /api/v1/auth/login
```

#### Request

```json
{
  "email": "admin@mail.com",
  "password": "password123"
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "access_token": "jwt_token",
    "expires_in": 3600
  }
}
```

---

### 4.2 Protected Routes

Gunakan header:

```http
Authorization: Bearer <token>
```

---

## 5. Admin Management

### 5.1 Create Admin

```http
POST /api/v1/admins
```

#### Request

```json
{
  "name": "Admin 1",
  "email": "admin@mail.com",
  "password": "password123"
}
```

---

## 6. Products

### 6.1 Create Product

```http
POST /api/v1/products
```

#### Request

```json
{
  "name": "Tepung Terigu",
  "code": "TPG001",
  "unit_weight": 5.0,
  "min_stock_threshold": 10,
  "description": "Produk tepung"
}
```

---

### 6.2 Get Products (Pagination)

```http
GET /api/v1/products?page=1&limit=10
```

---

## 7. IoT Sensor Data

### 7.1 Send Sensor Data (Core Endpoint)

```http
POST /api/v1/sensor-data
```

#### Request

```json
{
  "device_code": "DEV001",
  "filtered_weight": 12.53,
  "raw_weight": 12.80,
  "timestamp": "2026-03-24T22:00:00Z"
}
```

---

#### Processing Logic (Backend)

1. Validasi input
2. Ambil product berdasarkan device
3. Hitung:

```text
estimated_stock = floor(filtered_weight / unit_weight)
```

4. Tentukan status:

* VALID
* UNSTABLE
* SUSPICIOUS
* INVALID

5. Simpan ke:

* `sensor_readings`
* `stock_snapshots`

---

#### Response

```json
{
  "success": true,
  "data": {
    "estimated_stock": 2,
    "status": "VALID"
  }
}
```

---

## 8. Stock Monitoring

### 8.1 Get Current Stock (Snapshot)

```http
GET /api/v1/stocks
```

#### Response

```json
{
  "success": true,
  "data": [
    {
      "product_name": "Tepung",
      "current_stock": 20,
      "current_weight": 100.5,
      "status": "VALID",
      "last_updated": "2026-03-24T22:00:00Z"
    }
  ]
}
```

---

### 8.2 Stock History

```http
GET /api/v1/stocks/history?product_id=xxx&limit=50
```

---

## 9. Predictions

### 9.1 Get Prediction

```http
GET /api/v1/predictions?product_id=xxx
```

#### Response

```json
{
  "success": true,
  "data": {
    "predicted_days_remaining": 5,
    "predicted_stock_out_date": "2026-03-30",
    "confidence_score": 0.87
  }
}
```

---

## 10. Stock Transactions (Manual Adjustment)

### 10.1 Create Transaction

```http
POST /api/v1/transactions
```

#### Request

```json
{
  "product_id": "uuid",
  "transaction_type": "IN",
  "quantity": 10,
  "notes": "Restock"
}
```

---

## 11. IoT Device Management

### 11.1 Register Device

```http
POST /api/v1/devices
```

---

### 11.2 Get Devices

```http
GET /api/v1/devices
```

---

## 12. Validation Rules

* `filtered_weight` ≥ 0
* `device_code` wajib ada
* `timestamp` valid ISO format
* reject jika data null / invalid

---

## 13. Rate Limiting

* `/sensor-data` → max 10 req/sec/device
* `/auth/login` → max 5 req/min

---

## 14. Security

* JWT authentication
* Password hashing (BCrypt)
* Input validation
* Jangan expose internal error

---

## 15. Pagination Format

```json
{
  "success": true,
  "data": [...],
  "meta": {
    "page": 1,
    "limit": 10,
    "total": 100
  }
}
```

---

## 16. Error Codes (Example)

| Code          | Description          |
| ------------- | -------------------- |
| INVALID_INPUT | Request tidak valid  |
| UNAUTHORIZED  | Token invalid        |
| NOT_FOUND     | Data tidak ditemukan |
| SERVER_ERROR  | Internal error       |

---

## 17. Summary

API ini mencakup:

* Authentication
* IoT ingestion (core)
* Monitoring
* Prediction
* Transaction

Dirancang untuk:

* scalable
* maintainable
* secure
* production-ready
