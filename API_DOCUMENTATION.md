# 📘 IoT Stock Monitoring & Prediction System - Ultimate Technical Specification (V4.0 - MASTER)

Dokumentasi ini adalah "Single Source of Truth" untuk seluruh tim pengembang (Android, Hardware IoT, dan ML Service). Backend dibangun menggunakan **Ktor Framework** dengan arsitektur **Clean Architecture**, menggunakan **Exposed ORM** dan database **PostgreSQL (Neon)**.

---

## 1. Standar Global & Protokol

### 1.1 Base URL
- **Development:** `http://localhost:8080`
- **Production:** `https://your-production-url.com`

### 1.2 Format Data & Encoding
- **Content-Type:** `application/json`
- **Encoding:** `UTF-8`
- **Timestamp Format:** ISO 8601 (`YYYY-MM-DDTHH:mm:ssZ`) atau (`YYYY-MM-DDTHH:mm:ss.SSS`)

### 1.3 Keamanan (JWT Authentication)
Sebagian besar endpoint memerlukan autentikasi.
- **Header:** `Authorization: Bearer <access_token>`
- **Token Type:** JWT (HMAC256)
- **Expiration:** 3600 detik (1 Jam)

### 1.4 Rate Limiting (Ktor RateLimit Plugin)
Sistem membatasi jumlah request untuk menjaga stabilitas:
- **Rute Autentikasi (`/auth/*`):** Maksimal 10 request per 60 detik per IP.
- **Rute Ingest Sensor (`/sensor-readings`):** Maksimal 10 request per 1 detik per Device ID.

---

## 2. Struktur Response Standar (`BaseResponse`)

Backend secara konsisten menggunakan pembungkus (wrapper) untuk semua response agar parsing di sisi Client (Retrofit/Ktor Client) menjadi seragam.

### 2.1 Format Success (HTTP 200/201)
```json
{
  "success": true,
  "data": { ... }, // Bisa berupa Objek atau Array
  "message": "Operasi berhasil diselesaikan"
}
```

### 2.2 Format Error (HTTP 4xx/5xx)
```json
{
  "success": false,
  "data": null,
  "message": "Alasan kegagalan secara spesifik (e.g., Password minimal 8 karakter)"
}
```

---

## 3. Modul Autentikasi (Public)

### 3.1 Register Admin
Mendaftarkan akun administrator baru.
- **Method:** `POST`
- **Path:** `/api/v1/auth/register`
- **Input Validation:**
    - `name`: String, Required, Min 3 karakter.
    - `email`: String, Required, Harus format email valid (e.g., `user@mail.com`).
    - `password`: String, Required, **Minimal 8 karakter**.
- **Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securepassword123"
}
```
- **Responses:**
    - `201 Created`: Akun berhasil dibuat.
    - `400 Bad Request`: Validasi gagal (e.g., email tidak valid atau password < 8 char).
    - `409 Conflict`: Email sudah terdaftar di sistem.
    - `429 Too Many Requests`: Melebihi batas percobaan registrasi.

### 3.2 Login Admin
Mendapatkan token akses.
- **Method:** `POST`
- **Path:** `/api/v1/auth/login`
- **Request Body:**
```json
{
  "email": "john@example.com",
  "password": "securepassword123"
}
```
- **Response Success (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600
  },
  "message": "Login successful"
}
```
- **Response Error (401 Unauthorized):**
```json
{
  "success": false,
  "data": null,
  "message": "Invalid email or password"
}
```

---

## 4. Modul Manajemen Produk (Protected)

### 4.1 Create Product
- **Method:** `POST`
- **Path:** `/api/v1/products`
- **Request Body:**
```json
{
  "name": "Beras Pandan Wangi 5kg", // Required, String
  "code": "BRS-PW-001", // Required, String (Unique)
  "unitWeight": 5.0, // Required, Double (Kg per unit)
  "minStockThreshold": 15, // Required, Int (Pemicu LOW_STOCK)
  "description": "Beras kualitas premium aroma pandan" // Optional, String (nullable)
}
```
- **Error 409 Conflict:** Muncul jika `code` sudah ada di produk lain.

### 4.2 List Products (Paginated)
- **Method:** `GET`
- **Path:** `/api/v1/products`
- **Query Params:**
    - `page` (Int, Default: 1)
    - `limit` (Int, Default: 10)

---

## 5. Modul IoT & Ingest Data (Public & High Freq)

### 5.1 Ingest Sensor Data
Endpoint yang dipanggil oleh ESP32 setiap kali ada perubahan berat.
- **Method:** `POST`
- **Path:** `/api/v1/sensor-readings`
- **Request Body:**
```json
{
  "device_code": "ESP32-STOK-01", // Required
  "raw_weight": 75.32, // Required, Double
  "filtered_weight": 75.00, // Required, Double (Hasil filter hardware)
  "recorded_at": "2026-03-27T10:00:00Z" // ISO Timestamp
}
```
- **Response Success (200 OK):**
```json
{
  "success": true,
  "data": {
    "estimated_stock": 15, // Hasil pembulatan (weight / unitWeight)
    "validation_status": "VALID", // Enum: VALID, UNSTABLE, ANOMALY
    "is_anomaly": false // Boolean: lonjakan tidak wajar
  },
  "message": "Sensor data processed successfully"
}
```

### 5.2 Device Heartbeat
Menjaga status perangkat tetap "ACTIVE".
- **Method:** `POST`
- **Path:** `/api/v1/devices/{deviceCode}/heartbeat`
- **Logic:** Jika dalam 5 menit tidak ada request ke sini, scheduler backend akan otomatis mengubah status device menjadi `INACTIVE`.

---

## 6. Modul Monitoring Dashboard (Android)

### 6.1 Real-time Stock Dashboard
Menampilkan status semua rak/produk secara agregat.
- **Method:** `GET`
- **Path:** `/api/v1/stocks`
- **Logic Penentuan `stockStatus`:**
    - `currentStock == 0` → `OUT_OF_STOCK`
    - `currentStock < minStockThreshold` → `LOW_STOCK`
    - `else` → `SUFFICIENT`
- **Response Example (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "productId": "UUID-1234-...",
      "productName": "Beras Premium",
      "productCode": "BRS-001",
      "currentWeight": 50.0,
      "currentStock": 10,
      "minStockThreshold": 15,
      "stockStatus": "LOW_STOCK",
      "status": "VALID", // Kondisi sensor hardware terakhir
      "lastUpdated": "2026-03-27T10:05:00Z"
    }
  ],
  "message": "Stock dashboard data retrieved successfully"
}
```

### 6.2 Stock History (Grafik)
Mengambil data histori untuk ditampilkan dalam bentuk Line Chart.
- **Method:** `GET`
- **Path:** `/api/v1/stocks/{productId}/history`
- **Query Params:** `limit` (Int, Default: 50)
- **Response Data:**
```json
[
  { "estimatedStock": 10, "recordedAt": "2026-03-27T10:00:00Z" },
  { "estimatedStock": 9, "recordedAt": "2026-03-27T10:15:00Z" }
]
```

---

## 7. Modul Prediksi Machine Learning (Protected)

### 7.1 Get Prediction Results
Mengambil hasil analisis dari ML Service (Python).
- **Method:** `GET`
- **Path:** `/api/v1/predictions/{productId}`
- **Response Success (200 OK):**
```json
{
  "success": true,
  "data": {
    "predictedDaysRemaining": 5, // Sisa hari sebelum stok habis
    "predictedStockOutDate": "2026-04-01", // Tanggal kritis
    "confidenceScore": 0.95, // Akurasi model (0.0 - 1.0)
    "modelName": "Linear Regression",
    "modelVersion": "v1.0"
  },
  "message": "Prediction retrieved successfully"
}
```
- **Response Null (Data Kurang):**
```json
{
  "success": true,
  "data": null,
  "message": "No prediction data found for this product"
}
```

---

## 8. Master Tabel Error & Troubleshooting

| HTTP Code | Message / Error Key | Penyebab Utama | Solusi Bagi Client |
| :--- | :--- | :--- | :--- |
| **400** | `Bad Request` | JSON tidak valid, field kurang, atau tipe data salah. | Periksa kembali Request Body sesuai spek. |
| **401** | `Unauthorized` | Token Bearer tidak dikirim, salah, atau expired. | Jalankan flow Login ulang. |
| **403** | `Forbidden` | Token valid tapi tidak punya izin akses resource ini. | Hubungi Super Admin. |
| **404** | `Not Found` | ID Produk/Device tidak ditemukan di Database. | Pastikan UUID yang dikirim benar. |
| **409** | `Conflict` | Duplikasi data pada field Unique (Email/Product Code). | Gunakan identitas unik lainnya. |
| **429** | `Too Many Requests`| Melebihi batas Rate Limit (Anti-Spam). | Tunggu sesuai periodik limit (1s/60s). |
| **500** | `Internal Error` | Bug di Backend atau DB Neon terputus. | Lapor ke Tim Backend untuk cek Logcat. |

---

## 9. Catatan Penting untuk IoT (Hardware)
1. **Deduplikasi Histori:** Server hanya akan menyimpan entri baru di tabel `sensor_readings` jika ada perubahan pada `estimated_stock`. Jika berat berubah sedikit tapi jumlah stok tetap sama, server hanya akan mengupdate `stock_snapshots`.
2. **ISO Timestamp:** Hardware disarankan menggunakan NTP Client agar waktu `recorded_at` sinkron dengan server.
3. **Retry Mechanism:** Jika menerima status `500` atau `429`, lakukan retry dengan interval 5-10 detik.
