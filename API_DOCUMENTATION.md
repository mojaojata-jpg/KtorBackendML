# 📘 IoT Stock Monitoring & Prediction System — API Documentation (V5.2)

Dokumentasi lengkap untuk semua endpoint API backend Ktor. Cocok untuk tim **Mobile (Android)**, **IoT (ESP32)**, dan **ML Service**.

> **Base URL:** `http://localhost:8080` (atau IP server kamu)

---

## 📋 Daftar Isi

1. [Standar Global & Format Response](#1-standar-global--format-response)
2. [Autentikasi](#2-modul-autentikasi-public)
   - [POST /api/v1/auth/register](#21-register-admin)
   - [POST /api/v1/auth/login](#22-login-admin)
3. [Produk](#3-modul-produk-protected--jwt)
   - [POST /api/v1/products](#31-create-product)
   - [GET /api/v1/products](#32-get-all-products)
   - [GET /api/v1/products/{id}](#33-get-product-by-id)
   - [PUT /api/v1/products/{id}](#34-update-product)
   - [DELETE /api/v1/products/{id}](#35-delete-product)
4. [Inventory & RFID](#4-modul-inventory--rfid)
   - [POST /api/v1/inventory/scan](#41-rfid-scan-inout--iot-endpoint-public)
   - [POST /api/v1/inventory/register-tag](#42-register-rfid-tag-protected)
   - [GET /api/v1/inventory/dashboard](#43-dashboard-stok-protected)
   - [GET /api/v1/inventory/{productId}/history](#44-riwayat-pergerakan-stok-protected)
   - [POST /api/v1/inventory/iot-mode](#45-set-iot-operation-mode-protected)
5. [Forecasting & Grafik (New)](#5-forecasting--grafik-new---prophet-based)
   - [GET /api/v1/inventory/chart-data/{productId}](#51-get-data-grafik--estimasi-stok-habis)
   - [POST /api/v1/inventory/aggregate/manual](#52-trigger-agregasi-harian-manual)
   - [POST /api/admin/inventory/aggregate/sync](#53-admin-sync-agregasi-harian-protected)
6. [Error Responses Global](#6-error-responses-global)

---

## 1. Standar Global & Format Response

### 1.1 Autentikasi JWT

Semua endpoint **Protected** wajib menyertakan header berikut:

```
Authorization: Bearer <access_token>
```

Token didapatkan dari endpoint Login. **Expired dalam 3600 detik (1 jam).**

### 1.2 Format Response Standar

Semua response menggunakan struktur berikut:

```json
{
  "success": true | false,
  "data": { ... } | [ ... ] | null,
  "message": "Pesan status" | null,
  "error_code": null
}
```

> **Catatan:** Field `error_code` selalu bernilai `null` di seluruh sistem (tidak diimplementasi saat ini).

### 1.3 HTTP Status Codes

| Status Code | Kapan Terjadi |
|---|---|
| `200 OK` | Request berhasil |
| `201 Created` | Data berhasil dibuat |
| `400 Bad Request` | Input tidak valid, validasi gagal, atau business logic violation |
| `401 Unauthorized` | Token JWT tidak ada, tidak valid, atau expired |
| `404 Not Found` | Endpoint URL tidak terdaftar di server |
| `429 Too Many Requests` | Rate limit terlampaui |
| `500 Internal Server Error` | Error tidak terduga yang tidak dapat ditangani |

> ⚠️ **Penting:** Hampir semua error validasi dan business logic (duplikat, tidak ditemukan, dll) mengembalikan **400 Bad Request**, bukan 404 atau 500. Ini karena semua `IllegalArgumentException` dari UseCase ditangkap secara global oleh `StatusPages` dan dijadikan 400.

---

## 2. Modul Autentikasi (Public)

### 2.1 Register Admin

Membuat akun admin baru.

- **Method:** `POST`
- **URL:** `/api/v1/auth/register`
- **Auth:** ❌ Tidak diperlukan

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "admin@mail.com",
  "password": "password123"
}
```

**Validasi (dicek di Controller & UseCase):**
| Field | Aturan | Error Message |
|---|---|---|
| `name` | Wajib diisi, tidak boleh blank | `"Name is required"` |
| `name` | Minimal 3 karakter | `"Name must be at least 3 characters"` |
| `email` | Wajib diisi, tidak boleh blank | `"Email is required"` |
| `email` | Harus mengandung `@` | `"Invalid email format"` |
| `password` | Wajib diisi, tidak boleh blank | `"Password is required"` |
| `password` | Minimal 8 karakter | `"Password must be at least 8 characters"` |

---

**✅ Response Success — `201 Created`:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "John Doe",
    "email": "admin@mail.com"
  },
  "message": "Admin registered successfully",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Validasi Gagal):**

Contoh body kosong / field tidak sesuai aturan:
```json
{
  "success": false,
  "data": null,
  "message": "Name must be at least 3 characters",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Email Sudah Terdaftar):**
```json
{
  "success": false,
  "data": null,
  "message": "Email already exists",
  "error_code": null
}
```

> ⚠️ Email duplikat menghasilkan **400**, bukan 409 atau 500.

**❌ Response Error — `400 Bad Request` (JSON Tidak Valid / Field Hilang):**
```json
{
  "success": false,
  "data": null,
  "message": "Invalid JSON format or missing required fields",
  "error_code": null
}
```

---

### 2.2 Login Admin

Login dan mendapatkan JWT access token.

- **Method:** `POST`
- **URL:** `/api/v1/auth/login`
- **Auth:** ❌ Tidak diperlukan

**Request Body:**
```json
{
  "email": "admin@mail.com",
  "password": "password123"
}
```

**Validasi:**
| Field | Aturan | Error Message |
|---|---|---|
| `email` | Wajib diisi, tidak boleh blank | `"Email is required"` |
| `password` | Wajib diisi, tidak boleh blank | `"Password is required"` |

---

**✅ Response Success — `200 OK`:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6ImFkbWluQG1haWwuY29tIiwiYXVkIjoiaHR0cDovLzAuMC4wLjA6ODA4MCIsImlzcyI6Imh0dHA6Ly8wLjAuMC4wOjgwODAiLCJleHAiOjE3MTM2MTYwMDB9.example",
    "expiresIn": 3600
  },
  "message": "Login successful",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Email atau Password Salah):**
```json
{
  "success": false,
  "data": null,
  "message": "Invalid email or password",
  "error_code": null
}
```

> ⚠️ Pesan error sama untuk email tidak ditemukan maupun password salah (by design, untuk keamanan).

**❌ Response Error — `400 Bad Request` (Field Kosong):**
```json
{
  "success": false,
  "data": null,
  "message": "Email is required",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (JSON Tidak Valid):**
```json
{
  "success": false,
  "data": null,
  "message": "Invalid JSON format or missing required fields",
  "error_code": null
}
```

---

## 3. Modul Produk (Protected — JWT)

> **Semua endpoint di bagian ini memerlukan header:** `Authorization: Bearer <token>`
> Jika token tidak dikirim atau tidak valid, Ktor mengembalikan **401 Unauthorized**.

### 3.1 Create Product

Membuat produk baru di sistem.

- **Method:** `POST`
- **URL:** `/api/v1/products`
- **Auth:** ✅ JWT Required

**Request Body:**
```json
{
  "name": "Biji Kopi Arabika 1kg",
  "code": "KOPI-ARA-001",
  "unitLabel": "bag",
  "minStockThreshold": 5,
  "description": "Kopi kualitas ekspor dari Aceh",
  "imageUrl": "https://xyz.supabase.co/storage/v1/object/public/products/kopi.jpg"
}
```

**Field Detail:**
| Field | Tipe | Wajib | Default | Keterangan |
|---|---|---|---|---|
| `name` | String | ✅ | - | Nama produk, tidak boleh blank |
| `code` | String | ✅ | - | Kode SKU produk, harus unik |
| `unitLabel` | String | ✅ | `"pcs"` | Satuan (pcs, kg, bag, dll) |
| `minStockThreshold` | Int | ✅ | `0` | Batas stok minimum, tidak boleh negatif |
| `description` | String | ❌ | `null` | Deskripsi produk |
| `imageUrl` | String | ❌ | `null` | URL foto dari Supabase Storage |

**Validasi (dicek di Controller & UseCase):**
| Field | Aturan | Error Message |
|---|---|---|
| `name` | Tidak boleh blank | `"Product name cannot be empty"` |
| `code` | Tidak boleh blank | `"Product code (SKU) is required"` (controller) / `"Product code cannot be empty"` (usecase) |
| `unitLabel` | Tidak boleh blank | `"Unit label (e.g., pcs, kg) is required"` (controller) / `"Unit label cannot be empty"` (usecase) |
| `minStockThreshold` | >= 0 | `"Minimum stock threshold cannot be negative"` |

---

**✅ Response Success — `201 Created`:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Biji Kopi Arabika 1kg",
    "code": "KOPI-ARA-001",
    "unitLabel": "bag",
    "minStockThreshold": 5,
    "description": "Kopi kualitas ekspor dari Aceh",
    "imageUrl": "https://xyz.supabase.co/storage/v1/object/public/products/kopi.jpg",
    "imageUrl": "https://xyz.supabase.co/storage/v1/object/public/products/kopi.jpg",
    "createdAt": "2026-04-20T11:30:00.123456",
    "updatedAt": "2026-04-20T11:30:00.123456"
  },
  "message": "Product created successfully",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Validasi Gagal):**
```json
{
  "success": false,
  "data": null,
  "message": "Product name cannot be empty",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Kode Produk Duplikat):**
```json
{
  "success": false,
  "data": null,
  "message": "Product code already exists",
  "error_code": null
}
```

> ⚠️ Kode duplikat menghasilkan **400**, bukan 409 atau 500.

**❌ Response Error — `400 Bad Request` (JSON Tidak Valid):**
```json
{
  "success": false,
  "data": null,
  "message": "Invalid JSON format or missing required fields",
  "error_code": null
}
```

---

### 3.2 Get All Products

Mengambil semua produk dengan pagination.

- **Method:** `GET`
- **URL:** `/api/v1/products`
- **Auth:** ✅ JWT Required

**Query Parameters (Opsional):**
| Parameter | Tipe | Default | Min | Keterangan |
|---|---|---|---|---|
| `page` | Int | `1` | `1` | Nomor halaman |
| `limit` | Int | `10` | `1` | Jumlah data per halaman |

**Contoh URL:** `/api/v1/products?page=1&limit=10`

> ⚠️ Jika `page < 1` atau `limit < 1`, server mengembalikan **400 Bad Request**.

---

**✅ Response Success — `200 OK`:**
```json
{
  "success": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Biji Kopi Arabika 1kg",
      "code": "KOPI-ARA-001",
      "unitLabel": "bag",
      "minStockThreshold": 5,
      "description": "Kopi kualitas ekspor dari Aceh",
      "imageUrl": "https://xyz.supabase.co/storage/v1/object/public/products/kopi.jpg",
      "createdAt": "2026-04-20T11:30:00",
      "updatedAt": "2026-04-20T11:30:00"
    },
    {
      "id": "660e9500-f30c-52e5-b827-557766551111",
      "name": "Gula Pasir 1kg",
      "code": "GULA-001",
      "unitLabel": "kg",
      "minStockThreshold": 10,
      "description": null,
      "imageUrl": null,
      "createdAt": "2026-04-19T08:00:00",
      "updatedAt": "2026-04-19T08:00:00"
    }
  ],
  "message": null,
  "error_code": null
}
```

**✅ Response Success (Data Kosong) — `200 OK`:**
```json
{
  "success": true,
  "data": [],
  "message": null,
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Pagination Tidak Valid):**
```json
{
  "success": false,
  "data": null,
  "message": "Page number must be at least 1",
  "error_code": null
}
```

> ⚠️ `GET /api/v1/products` mengembalikan **semua produk** termasuk yang `isActive = false`. Filter aktif/nonaktif belum diimplementasi di endpoint ini.

---

### 3.3 Get Product by ID

Mengambil detail satu produk berdasarkan UUID.

- **Method:** `GET`
- **URL:** `/api/v1/products/{id}`
- **Auth:** ✅ JWT Required

**Path Parameter:**
| Parameter | Tipe | Keterangan |
|---|---|---|
| `id` | UUID String | ID produk |

**Contoh URL:** `/api/v1/products/550e8400-e29b-41d4-a716-446655440000`

---

**✅ Response Success — `200 OK`:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Biji Kopi Arabika 1kg",
    "code": "KOPI-ARA-001",
    "unitLabel": "bag",
    "minStockThreshold": 5,
    "description": "Kopi kualitas ekspor dari Aceh",
    "imageUrl": "https://xyz.supabase.co/storage/v1/object/public/products/kopi.jpg",
    "createdAt": "2026-04-20T11:30:00",
    "updatedAt": "2026-04-20T11:30:00"
  },
  "message": null,
  "error_code": null
}
```

**❌ Response Error — `404 Not Found` (ID Tidak Ditemukan atau Format UUID Salah):**
```json
{
  "success": false,
  "data": null,
  "message": "Product not found",
  "error_code": null
}
```

> ⚠️ Jika `id` bukan format UUID yang valid, `ProductRepositoryImpl.findById()` menangkap exception secara internal dan mengembalikan `null` → tetap 404.

---

### 3.4 Update Product

Memperbarui data produk yang sudah ada.

- **Method:** `PUT`
- **URL:** `/api/v1/products/{id}`
- **Auth:** ✅ JWT Required

**Path Parameter:**
| Parameter | Tipe | Keterangan |
|---|---|---|
| `id` | UUID String | ID produk yang akan diupdate |

**Request Body:** (sama seperti Create Product)
```json
{
  "name": "Biji Kopi Arabika Premium 1kg",
  "code": "KOPI-ARA-001",
  "unitLabel": "bag",
  "minStockThreshold": 10,
  "description": "Kopi kualitas ekspor premium dari Aceh",
  "imageUrl": "https://xyz.supabase.co/storage/v1/object/public/products/kopi-premium.jpg"
}
```

**Validasi (dicek di Controller & UseCase):**
| Field | Aturan | Error Message |
|---|---|---|
| `name` | Tidak boleh blank | `"Product name cannot be empty"` |
| `code` | Tidak boleh blank | `"Product code is required"` (controller) / `"Product code cannot be empty"` (usecase) |
| `unitLabel` | Tidak boleh blank | `"Unit label cannot be empty"` |
| `minStockThreshold` | >= 0 | `"Minimum stock threshold cannot be negative"` |

---

**✅ Response Success — `200 OK`:**
```json
{
  "success": true,
  "data": null,
  "message": "Product updated successfully",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Kode Duplikat dengan Produk Lain):**
```json
{
  "success": false,
  "data": null,
  "message": "Product code already exists with another product",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Validasi Gagal):**
```json
{
  "success": false,
  "data": null,
  "message": "Product name cannot be empty",
  "error_code": null
}
```

**❌ Response Error — `404 Not Found` (ID Tidak Ada):**
```json
{
  "success": false,
  "data": null,
  "message": "Product not found or no changes made",
  "error_code": null
}
```

---

### 3.5 Delete Product

Menghapus produk dari sistem. **Semua data terkait (Tag RFID, Events, Snapshots, Predictions) akan ikut terhapus (CASCADE).**

- **Method:** `DELETE`
- **URL:** `/api/v1/products/{id}`
- **Auth:** ✅ JWT Required

**Path Parameter:**
| Parameter | Tipe | Keterangan |
|---|---|---|
| `id` | UUID String | ID produk yang akan dihapus |

**Contoh URL:** `/api/v1/products/550e8400-e29b-41d4-a716-446655440000`

---

**✅ Response Success — `200 OK`:**
```json
{
  "success": true,
  "data": null,
  "message": "Product deleted successfully",
  "error_code": null
}
```

**❌ Response Error — `404 Not Found` (ID Tidak Ada / UUID Tidak Valid):**
```json
{
  "success": false,
  "data": null,
  "message": "Product not found",
  "error_code": null
}
```

> ⚠️ `DeleteProductUseCase` juga menangkap UUID format yang salah dan mengembalikan `false` → 404.

---

## 4. Modul Inventory & RFID

### 4.1 RFID Scan IN/OUT — IoT Endpoint (Public)

Digunakan oleh **ESP32** untuk mencatat pergerakan barang masuk atau keluar melalui scan RFID. Endpoint ini **tidak memerlukan JWT**.

- **Method:** `POST`
- **URL:** `/api/v1/inventory/scan`
- **Auth:** ❌ Tidak diperlukan (Public / IoT)

**Request Body:**
```json
{
  "tag_uid": "E2:00:00:25:39:10:11:45",
  "event_type": "OUT",
  "note": "Diambil untuk produksi batch #12"
}
```

**Field Detail:**
| Field | Tipe | Wajib | Keterangan |
|---|---|---|---|
| `tag_uid` | String | ✅ | UID tag RFID fisik, tidak boleh blank |
| `event_type` | String | ✅ | Hanya `"IN"` atau `"OUT"` (case-sensitive) |
| `note` | String | ❌ | Catatan tambahan |

**Business Logic (Anti-Double Scan):**
| Status Tag Saat Ini | `event_type` Diminta | Hasilnya |
|---|---|---|
| `ACTIVE` (ada di stok) | `OUT` | ✅ Berhasil — stok berkurang 1, tag jadi `INACTIVE` |
| `INACTIVE` (sudah keluar) | `IN` | ✅ Berhasil — stok bertambah 1, tag jadi `ACTIVE` |
| `ACTIVE` | `IN` | ❌ Error 400 |
| `INACTIVE` | `OUT` | ❌ Error 400 |

---

**✅ Response Success (Scan OUT) — `200 OK`:**
```json
{
  "success": true,
  "data": {
    "new_stock": 9,
    "status": "SUFFICIENT"
  },
  "message": "Scan processed: OUT",
  "error_code": null
}
```

**✅ Response Success (Scan IN) — `200 OK`:**
```json
{
  "success": true,
  "data": {
    "new_stock": 10,
    "status": "SUFFICIENT"
  },
  "message": "Scan processed: IN",
  "error_code": null
}
```

**Nilai `status` yang mungkin:**
| Nilai | Kondisi |
|---|---|
| `"SUFFICIENT"` | `current_stock >= minStockThreshold` |
| `"LOW_STOCK"` | `0 < current_stock < minStockThreshold` |
| `"OUT_OF_STOCK"` | `current_stock == 0` |

**❌ Response Error — `400 Bad Request` (event_type Tidak Valid):**
```json
{
  "success": false,
  "data": null,
  "message": "Event type must be either 'IN' or 'OUT'",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Double Scan OUT pada tag sudah INACTIVE):**
```json
{
  "success": false,
  "data": null,
  "message": "Barang (Tag: E2:00:00:25:39:10:11:45) sudah tidak ada di stok (Status: INACTIVE)",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Double Scan IN pada tag masih ACTIVE):**
```json
{
  "success": false,
  "data": null,
  "message": "Barang (Tag: E2:00:00:25:39:10:11:45) sudah ada di dalam stok (Status: ACTIVE)",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Tag UID Tidak Terdaftar):**
```json
{
  "success": false,
  "data": null,
  "message": "RFID Tag not registered: E2:00:00:25:39:10:11:45",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (JSON Tidak Valid / Field Hilang):**
```json
{
  "success": false,
  "data": null,
  "message": "Invalid JSON format or missing required fields (tag_uid, event_type)",
  "error_code": null
}
```

---

### 4.2 Register RFID Tag (Protected)

Mendaftarkan tag RFID fisik ke produk tertentu. **Otomatis menambah stok sebanyak 1 unit** dan mencatat event `REGISTER`.

- **Method:** `POST`
- **URL:** `/api/v1/inventory/register-tag`
- **Auth:** ✅ JWT Required

**Request Body:**
```json
{
  "product_id": "550e8400-e29b-41d4-a716-446655440000",
  "tag_uid": "E2:00:00:25:39:10:11:45",
  "tag_label": "Batch Jan-01"
}
```

**Field Detail:**
| Field | Tipe | Wajib | Keterangan |
|---|---|---|---|
| `product_id` | UUID String | ✅ | ID produk yang akan dihubungkan, tidak boleh blank |
| `tag_uid` | String | ✅ | UID tag RFID fisik, harus unik di sistem, tidak boleh blank |
| `tag_label` | String | ❌ | Label keterangan tag |

---

**✅ Response Success — `201 Created`:**
```json
{
  "success": true,
  "data": {
    "tag_id": "770fa600-g51d-63f6-c938-668877661234",
    "current_stock": 11
  },
  "message": "Tag registered successfully",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Field Kosong):**
```json
{
  "success": false,
  "data": null,
  "message": "Tag UID is required",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Tag UID Sudah Terdaftar):**

Pesan mengandung UID tag dan ID produk yang memilikinya:
```json
{
  "success": false,
  "data": null,
  "message": "RFID Tag already registered: E2:00:00:25:39:10:11:45 (Product: 550e8400-e29b-41d4-a716-446655440000)",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Produk Tidak Ditemukan):**

Pesan mengandung product_id yang dikirim:
```json
{
  "success": false,
  "data": null,
  "message": "Product not found: 550e8400-e29b-41d4-a716-446655440000",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (JSON Tidak Valid / Field Hilang):**
```json
{
  "success": false,
  "data": null,
  "message": "Invalid JSON format or missing required fields (product_id, tag_uid)",
  "error_code": null
}
```

---

### 4.3 Dashboard Stok (Protected)

Menampilkan ringkasan stok terkini semua produk yang aktif (`isActive = true`) beserta statistik pergerakan.

- **Method:** `GET`
- **URL:** `/api/v1/inventory/dashboard`
- **Auth:** ✅ JWT Required

---

**✅ Response Success — `200 OK`:**
```json
{
  "success": true,
  "data": [
    {
      "product_id": "550e8400-e29b-41d4-a716-446655440000",
      "product_name": "Biji Kopi Arabika 1kg",
      "product_code": "KOPI-ARA-001",
      "current_stock": 10,
      "total_incoming": 15,
      "total_outgoing": 5,
      "status": "SUFFICIENT",
      "unit": "bag"
    },
    {
      "product_id": "660e9500-f30c-52e5-b827-557766551111",
      "product_name": "Gula Pasir 1kg",
      "product_code": "GULA-001",
      "current_stock": 2,
      "total_incoming": 20,
      "total_outgoing": 18,
      "status": "LOW_STOCK",
      "unit": "kg"
    },
    {
      "product_id": "770fa600-g51d-63f6-c938-668877661234",
      "product_name": "Tepung Terigu 1kg",
      "product_code": "TEPUNG-001",
      "current_stock": 0,
      "total_incoming": 10,
      "total_outgoing": 10,
      "status": "OUT_OF_STOCK",
      "unit": "kg"
    }
  ],
  "message": null,
  "error_code": null
}
```

**Penjelasan Field:**
| Field | Keterangan |
|---|---|
| `current_stock` | Stok saat ini berdasarkan snapshot terbaru |
| `total_incoming` | Sum quantity dari event `REGISTER` dan `IN` (positif) |
| `total_outgoing` | Sum absolute quantity dari event `OUT` (nilai positif) |
| `status` | `"SUFFICIENT"` / `"LOW_STOCK"` / `"OUT_OF_STOCK"` |
| `unit` | Satuan dari produk (`unitLabel`) |

> ⚠️ Jika produk belum pernah ada snapshot (belum ada tag yang diregistrasi), `current_stock` = `0` dan `status` = `"OUT_OF_STOCK"`.

**✅ Response Success (Tidak Ada Produk Aktif) — `200 OK`:**
```json
{
  "success": true,
  "data": [],
  "message": null,
  "error_code": null
}
```

---

### 4.4 Riwayat Pergerakan Stok (Protected)

Menampilkan histori event untuk satu produk, diurutkan dari yang terbaru. Event yang mungkin muncul: `REGISTER`, `IN`, `OUT`.

- **Method:** `GET`
- **URL:** `/api/v1/inventory/{productId}/history`
- **Auth:** ✅ JWT Required

**Path Parameter:**
| Parameter | Tipe | Keterangan |
|---|---|---|
| `productId` | UUID String | ID produk |

**Query Parameter (Opsional):**
| Parameter | Tipe | Default | Keterangan |
|---|---|---|---|
| `limit` | Int | `50` | Jumlah event yang ditampilkan |

**Contoh URL:** `/api/v1/inventory/550e8400-e29b-41d4-a716-446655440000/history?limit=20`

---

**✅ Response Success — `200 OK`:**
```json
{
  "success": true,
  "data": [
    {
      "event_type": "OUT",
      "quantity": -1,
      "recorded_at": "2026-04-20T11:30:00",
      "note": "Diambil untuk produksi batch #12"
    },
    {
      "event_type": "IN",
      "quantity": 1,
      "recorded_at": "2026-04-20T09:00:00",
      "note": null
    },
    {
      "event_type": "REGISTER",
      "quantity": 1,
      "recorded_at": "2026-04-19T08:00:00",
      "note": "Initial registration of tag E2:00:00:25:39:10:11:45"
    }
  ],
  "message": null,
  "error_code": null
}
```

> ⚠️ **Perhatian:** Field `quantity` untuk event `OUT` bernilai **negatif** (misal: `-1`) karena disimpan sebagai delta stok di database. Hal ini sesuai dengan cara kerja `ProcessRfidScanUseCase` (`quantity = if (eventType == "IN") 1 else -1`).

**❌ Response Error — `400 Bad Request` (Path Parameter Kosong):**
```json
{
  "success": false,
  "data": null,
  "message": "Product ID is required",
  "error_code": null
}
```

**❌ Response Error — `400 Bad Request` (Produk Tidak Ditemukan):**
```json
{
  "success": false,
  "data": null,
  "message": "Product with ID 550e8400-e29b-41d4-a716-446655440000 not found",
  "error_code": null
}
```

**✅ Response Success (Produk Ada tapi Belum Ada Event) — `200 OK`:**
```json
{
  "success": true,
  "data": [],
  "message": null,
  "error_code": null
}
```

---

### 4.5 Set IoT Operation Mode (Protected)

Mengontrol mode operasional pembaca RFID (IoT). Berguna untuk aktivasi registrasi otomatis atau mode scan-out berkelanjutan.

- **Method:** `POST`
- **URL:** `/api/v1/inventory/iot-mode`
- **Auth:** ✅ JWT Required

**Request Body:**
```json
{
  "mode": "SCAN_OUT",
  "product_id": "optional-uuid-here"
}
```

**Penjelasan Mode:**
| Mode | Deskripsi | Syarat |
|---|---|---|
| `NORMAL` | Mode standar untuk scan IN/OUT (tergantung payload ESP32). | `product_id` opsional/diabaikan. |
| `REGISTER` | Setiap scan baru akan **otomatis didaftarkan** ke `product_id`. | `product_id` wajib diisi. |
| `SCAN_OUT` | Setiap scan akan dipaksa menjadi event **OUT**, mengabaikan input ESP32. Pada mode ini, pengecekan status tag (`ACTIVE`/`INACTIVE`) dilewati agar dapat melakukan simulasi/transaksi massal dengan cepat. | `product_id` opsional. |

---

**✅ Response Success — `200 OK`:**
```json
{
  "success": true,
  "data": null,
  "message": "IoT Mode updated to SCAN_OUT for product NONE"
}
```

**❌ Response Error — `400 Bad Request` (Validasi Gagal):**
```json
{
  "success": false,
  "data": null,
  "message": "Product ID is required for REGISTER mode"
}
```

---

---

## 5. Forecasting & Grafik (New - Prophet Based)

Modul baru ini menggantikan sistem regresi linear lama. Data disediakan dalam bentuk deret waktu (time-series) untuk kebutuhan grafik dan estimasi sisa stok. Backend akan menghitung sisa hari secara real-time berdasarkan ramalan demand dari Prophet.

### 5.1 GET Data Grafik & Estimasi Stok Habis

Endpoint utama untuk menampilkan dashboard produk. Mengembalikan data histori (30 hari terakhir) dan data ramalan masa depan dari Prophet.

- **Method:** `GET`
- **URL:** `/api/v1/inventory/chart-data/{productId}`
- **Auth:** ❌ Tidak diperlukan (Public / Dashboard)

**✅ Response Success — `200 OK`:**
```json
{
  "success": true,
  "data": {
    "product": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Biji Kopi Arabika 1kg",
      "code": "KOPI-ARA-001",
      "unitLabel": "bag",
      "minStockThreshold": 5,
      "description": "...",
      "imageUrl": "...",
      "createdAt": "...",
      "updatedAt": "..."
    },
    "currentStock": 15,
    "estimatedStockOutDate": "2026-05-20",
    "remainingDays": 21,
    "historicalData": [
      {
        "date": "2026-04-28",
        "totalIn": 10,
        "totalOut": 2,
        "netFlow": 8
      }
    ],
    "forecastingData": [
      {
        "targetDate": "2026-04-30",
        "predictedValue": 2.5,
        "lowerBound": 2.0,
        "upperBound": 3.0
      }
    ]
  },
  "message": null,
  "error_code": null
}
```

**Keterangan Field:**
| Field | Deskripsi |
|---|---|
| `currentStock` | Jumlah stok riil saat ini di gudang. |
| `estimatedStockOutDate` | Tanggal estimasi stok habis (hasil hitungan Backend terhadap ramalan Prophet). |
| `remainingDays` | Berapa hari lagi stok diprediksi bertahan. |
| `historicalData` | Rekap harian 30 hari terakhir (In, Out, Net). |
| `forecastingData` | Ramalan pengeluaran barang harian ke depan dari Prophet. |

---

### 5.2 Trigger Agregasi Harian Manual

Memicu proses rekapitulasi data scan (`inventory_events`) menjadi data harian (`daily_aggregates`) secara paksa.

- **Method:** `POST`
- **URL:** `/api/v1/inventory/aggregate/manual`
- **Query Parameter:** `date` (format `YYYY-MM-DD`, opsional)

**✅ Response Success — `200 OK`:**
```json
{
  "success": true,
  "data": null,
  "message": "Aggregation for 2026-04-29 triggered successfully",
  "error_code": null
}
```

---

### 5.3 Admin Sync Agregasi Harian (Protected)

Endpoint untuk Admin agar bisa **memperbarui data rekapitulasi stok secara real-time**. Berbeda dengan `aggregate/manual` yang hanya memproses produk yang punya event, endpoint ini memproses **SEMUA produk aktif** — termasuk yang tidak ada event (diisi 0). Ini memastikan ML Service mendapat gambaran data lengkap.

> ⚠️ **Penting untuk ML:** Endpoint ini wajib dipanggil oleh Admin setelah ada aktivitas scan agar ML Service (yang polling setiap 5 menit) bisa mendeteksi perubahan data melalui kolom `updated_at` di tabel `daily_aggregates`.

- **Method:** `POST`
- **URL:** `/api/admin/inventory/aggregate/sync`
- **Auth:** ✅ JWT Required (Admin Only)
- **Query Parameter:** `date` (format `YYYY-MM-DD`, opsional — default: hari ini)

**Contoh URL:**
- `/api/admin/inventory/aggregate/sync` → sync untuk hari ini
- `/api/admin/inventory/aggregate/sync?date=2026-05-01` → sync untuk tanggal tertentu

---

**✅ Response Success — `200 OK`:**
```json
{
  "success": true,
  "data": {
    "date": "2026-05-06",
    "productsSynced": 6
  },
  "message": "Aggregation sync completed for 2026-05-06. 6 product(s) processed.",
  "error_code": null
}
```

**Penjelasan Field:**
| Field | Tipe | Keterangan |
|---|---|---|
| `date` | String | Tanggal yang di-sync (format: `YYYY-MM-DD`) |
| `productsSynced` | Int | Jumlah produk aktif yang diproses |

**❌ Response Error — `401 Unauthorized` (Tanpa Token / Token Expired):**

> Ktor mengembalikan response `401` dengan body kosong dan header `WWW-Authenticate`. Tangani di client dengan mengecek status code.

**❌ Response Error — `400 Bad Request` (Format Tanggal Salah):**
```json
{
  "success": false,
  "data": null,
  "message": "Invalid date format. Use YYYY-MM-DD (e.g., 2026-05-06)",
  "error_code": null
}
```

**❌ Response Error — `500 Internal Server Error` (Database Error):**
```json
{
  "success": false,
  "data": null,
  "message": "Failed to sync aggregation: <detail error>",
  "error_code": null
}
```

## 6. Error Responses Global

Error berikut dapat terjadi di **semua endpoint** dan ditangani secara global oleh `StatusPages` plugin:

### `400 Bad Request` — IllegalArgumentException Global

Semua `IllegalArgumentException` yang tidak tertangkap di controller akan ditangkap di sini:
```json
{
  "success": false,
  "data": null,
  "message": "<pesan dari exception>",
  "error_code": null
}
```

### `401 Unauthorized` — Token Tidak Valid

Terjadi saat mengakses endpoint Protected tanpa token atau token expired/salah. Response ini dibuat oleh Ktor JWT plugin secara otomatis (bukan JSON standard BackResponse):

> Ktor mengembalikan response `401` dengan body kosong atau `WWW-Authenticate` header. Tangani di client dengan mengecek status code, bukan body.

### `404 Not Found` — Endpoint Tidak Ada

Terjadi ketika URL yang diakses tidak terdaftar sama sekali di routing:
```json
{
  "success": false,
  "data": null,
  "message": "Endpoint or Resource not found",
  "error_code": null
}
```

### `429 Too Many Requests` — Rate Limit

Terjadi ketika request terlalu sering:
```json
{
  "success": false,
  "data": null,
  "message": "Too many requests. Please try again later.",
  "error_code": null
}
```

### `500 Internal Server Error` — Error Tidak Terduga

Terjadi untuk error yang bukan `IllegalArgumentException` (misalnya database error):
```json
{
  "success": false,
  "data": null,
  "message": "Internal Server Error: <detail error>",
  "error_code": null
}
```

---

## 📌 Quick Reference — Semua Endpoint

| Method | Endpoint | Auth | HTTP Success | Keterangan |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | ❌ Public | `201` | Register admin baru |
| `POST` | `/api/v1/auth/login` | ❌ Public | `200` | Login & dapat token |
| `POST` | `/api/v1/products` | ✅ JWT | `201` | Buat produk baru |
| `GET` | `/api/v1/products` | ✅ JWT | `200` | List semua produk (pagination) |
| `GET` | `/api/v1/products/{id}` | ✅ JWT | `200` | Detail satu produk |
| `PUT` | `/api/v1/products/{id}` | ✅ JWT | `200` | Update produk |
| `DELETE` | `/api/v1/products/{id}` | ✅ JWT | `200` | Hapus produk (cascade) |
| `POST` | `/api/v1/inventory/scan` | ❌ Public (IoT) | `200` | Scan RFID IN/OUT oleh ESP32 |
| `POST` | `/api/v1/inventory/register-tag` | ✅ JWT | `201` | Daftarkan tag RFID ke produk |
| `GET` | `/api/v1/inventory/dashboard` | ✅ JWT | `200` | Dashboard ringkasan stok |
| `GET` | `/api/v1/inventory/{productId}/history` | ✅ JWT | `200` | Riwayat scan suatu produk |
| `GET` | `/api/v1/inventory/chart-data/{productId}` | ❌ Public | `200` | Grafik & Estimasi Stok Habis (Prophet) |
| `POST` | `/api/v1/inventory/aggregate/manual` | ❌ Public | `200` | Trigger rekap harian manual |
| `POST` | `/api/admin/inventory/aggregate/sync` | ✅ JWT | `200` | Admin Sync agregasi semua produk |

---

*Dokumentasi ini dihasilkan dari audit langsung terhadap source code: controllers, use cases, domain models, repositories, dan plugin configs.*
