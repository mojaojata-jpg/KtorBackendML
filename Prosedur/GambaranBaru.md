

````md
# 📘 System Overview - RFID-Based Stock Monitoring and Prediction Application

## 1. System Purpose

Aplikasi ini adalah sistem monitoring stok berbasis RFID yang bertujuan untuk:

* Mendaftarkan tag RFID ke item produk secara unik
* Mencatat pergerakan stok barang masuk dan barang keluar secara real-time berbasis event scan
* Menyimpan histori aktivitas stok untuk monitoring dan audit
* Menghasilkan snapshot stok terkini agar informasi stok dapat diakses dengan cepat
* Melakukan prediksi kapan stok akan habis menggunakan machine learning
* Menyediakan dashboard monitoring melalui aplikasi client (Android / Web)

Sistem ini dirancang untuk lingkungan usaha yang memiliki bahan operasional yang dipakai terus-menerus, seperti tepung, kopi, keju, saus, atau bahan produksi lain yang perlu dipantau ketersediaannya secara konsisten.

---

## 2. High-Level Architecture

```text
[ RFID Tag / Item ]
        ↓
[ ESP32 + RFID Reader ]
        ↓
[ Backend API (Ktor) ]  ← (Daily Aggregation Job)
        ↓
[ PostgreSQL (Neon) ]  ← (daily_aggregates table)
        ↓
[ ML Service (Python) ] ← (Prophet Engine)
        ↓
[ PostgreSQL (Neon) ]  ← (forecasting_results table)
        ↓
[ Backend API (Ktor) ]  ← (Real-time Prediction Calculation)
        ↓
[ Client App (Android / Web) ]
````

---

## 3. Component Responsibilities

### 3.1 RFID Tag Layer

**Peran: Identitas Item**

Setiap item fisik memiliki RFID tag unik.

Contoh:

* 1 tepung terigu = 1 RFID tag
* 1 keju = 1 RFID tag
* 1 saus = 1 RFID tag

RFID tag digunakan untuk:

* Registrasi item ke produk
* Identifikasi barang saat barang masuk
* Identifikasi barang saat barang keluar

RFID tag bukan penyimpan data bisnis, hanya identitas unik untuk item fisik.

---

### 3.2 IoT Layer (ESP32 + RFID Reader)

**Peran: Data Collector / Scanner**

IoT hanya bertanggung jawab untuk:

* Membaca UID dari tag RFID
* Mengirim hasil scan ke backend melalui API
* Menyediakan feedback sederhana seperti LED, buzzer, atau button mode
* Menjalankan mode operasional seperti:

  * Register Tag
  * Barang Masuk (IN)
  * Barang Keluar (OUT)

IoT **tidak memiliki business logic utama**. IoT tidak menghitung prediksi, tidak mengolah histori stok secara mendalam, dan tidak menyimpan data master produk secara lokal.

**Data yang dikirim:**

```json
{
  "tag_uid": "A1B2C3D4",
  "event_type": "IN",
  "mode": "REGISTER",
  "timestamp": "2026-03-24T22:00:00Z"
}
```

---

### 3.3 Backend (Ktor)

**Peran: Core System (Business Logic Layer)**

Backend adalah pusat dari seluruh sistem dan bertanggung jawab untuk:

#### a. Product Registration

* Mendaftarkan produk ke sistem
* Mengaitkan tag RFID ke produk tertentu saat mode register
* Menyimpan mapping `tag_uid → product_id`

#### b. Event Processing

* Menerima data scan dari ESP32
* Mengecek apakah tag RFID sudah terdaftar
* Menentukan produk berdasarkan UID tag
* Mencatat event:

  * REGISTER
  * IN
  * OUT
  * ADJUSTMENT

#### c. Stock Calculation

* Menghitung stok berdasarkan histori event
* Menentukan stok terkini per produk
* Menyimpan stok terkini ke tabel snapshot

#### d. Validation

* Menolak tag yang belum terdaftar saat mode operasional
* Menolak tag duplikat saat registrasi
* Mencegah event yang tidak valid
* Menentukan status stok:

  * OUT_OF_STOCK
  * LOW_STOCK
  * SUFFICIENT

#### e. API Provider

* Menyediakan endpoint untuk:

  * Admin (`/auth`, `/products`, `/register-tag`, `/admin/inventory/aggregate/sync`)
  * IoT Scanner (`/scan`)
  * Client (`/inventory/dashboard`, `/inventory/chart-data/{id}`, `/inventory/{id}/history`)

#### f. Aggregation Engine (NEW)
* Menjalankan Background Job tiap malam (23:59) untuk merangkum ribuan scan harian menjadi satu baris rekapitulasi (Total IN, Total OUT).
* Menyediakan endpoint Admin Sync (`/api/admin/inventory/aggregate/sync`) untuk memperbarui rekap secara real-time saat Admin menekan tombol "Sync" di frontend.
* Menyimpan rekap tersebut ke tabel `daily_aggregates` sebagai bahan baku ML.

#### g. Real-time Prediction Calculation
* Mengambil data ramalan harian dari Python.
* Menghitung "Estimated Stock Out Date" secara langsung (dynamic) berdasarkan stok riil terbaru saat ini dikurangi ramalan pengeluaran harian.
* Menyajikan data grafik gabungan (histori + ramalan) untuk Client.

---

### 3.4 Database (PostgreSQL - Neon)

**Peran: Single Source of Truth**

Database menyimpan:

* Data master produk (`products`)
* Mapping tag RFID ke produk (`product_rfid_tags`)
* Histori scan pergerakan stok (`inventory_events`)
* Stok terkini (`inventory_snapshots`)
* Rekapitulasi harian (`daily_aggregates`)
* Ramalan deret waktu masa depan (`forecasting_results`)
* Data admin (`admins`)

Database digunakan untuk:

* Query real-time stok terkini
* Audit histori barang masuk dan keluar
* Training data untuk machine learning
* Analisis pola konsumsi stok

---

### 3.5 Machine Learning Service (Python)

**Peran: Prediction Engine**

ML service berjalan terpisah dari backend (decoupled).

#### a. Input
Mengambil data agregat dari database:
* `daily_aggregates` (Data yang sudah diringkas oleh Ktor)

#### b. Processing
* Menggunakan model **Prophet (Meta)** untuk menangani tren dan musiman (seasonality).
* Menghasilkan ramalan pengeluaran barang (`yhat`) untuk 30-90 hari ke depan.

#### c. Output
Menghasilkan ramalan harian berupa:
* `predicted_value` (Estimasi jumlah keluar)
* `lower_bound` & `upper_bound`

#### d. Storage
Hasil ramalan disimpan ke:
* `forecasting_results`

---

### 3.6 Client (Android - Jetpack Compose / Web Dashboard)

**Peran: Monitoring Interface**

Client hanya bertugas untuk:

* Menampilkan data stok
* Menampilkan histori barang masuk dan keluar
* Menampilkan hasil prediksi
* Menampilkan status stok real-time
* Menampilkan peringatan stok menipis

#### Data yang ditampilkan:

* Stok terkini per produk
* Riwayat event IN / OUT
* Status stok
* Grafik histori stok
* Prediksi stok habis

---

## 4. Data Flow (End-to-End)

### 4.1 Register Tag Flow

```text
Admin pilih product
→ ESP32 masuk mode REGISTER
→ scan RFID tag
→ backend simpan tag_uid ke product_rfid_tags
→ backend catat event REGISTER
→ backend update / buat snapshot stok awal
```

### 4.2 Barang Masuk (IN) Flow

```text
Admin / petugas pilih mode IN
→ scan RFID tag
→ backend cari tag_uid
→ backend ambil product_id
→ backend simpan event IN
→ backend update inventory_snapshots
```

### 4.3 Barang Keluar (OUT) Flow

```text
Admin / petugas pilih mode OUT
→ scan RFID tag
→ backend cari tag_uid
→ backend ambil product_id
→ backend simpan event OUT
→ backend update inventory_snapshots
```

### 4.4 Backend to Database
```text
Backend → INSERT product_rfid_tags
Backend → INSERT inventory_events
Backend → UPDATE / INSERT inventory_snapshots
Backend → INSERT daily_aggregates (via Nightly Job)
```

### 4.5 Database to ML
```text
ML Service → SELECT daily_aggregates
```

### 4.6 ML to Database
```text
ML → INSERT forecasting_results
```

### 4.7 Backend to Client
```text
Client → GET /inventory/dashboard
Client → GET /inventory/chart-data/{id}
Client → GET /inventory/{id}/history
```

Backend mengambil:

* snapshot terbaru
* histori event
* hasil prediksi terbaru

---

## 5. Communication Strategy

### IoT → Backend

* REST API (HTTP)
* JSON payload
* Stateless
* Scanner hanya mengirim data scan, bukan menyimpan logika stok

### Backend ↔ Database

* JDBC (PostgreSQL)
* Connection pooling (HikariCP)

### ML ↔ Database

* Direct DB access (read/write)
* Tidak perlu API call untuk efisiensi

### Client → Backend

* REST API
* JSON response

---

## 6. Design Principles

* RFID tag adalah identitas unik item
* Satu tag mewakili satu item
* Satu produk dapat memiliki banyak tag
* Backend sebagai pusat logika
* IoT hanya sebagai scanner / data collector
* ML terpisah (loosely coupled)
* Client hanya sebagai viewer
* Database sebagai sumber data utama

---

## 7. Key Constraints

* Tag RFID harus diregistrasi terlebih dahulu sebelum digunakan pada mode operasional
* Sistem harus membedakan mode REGISTER, IN, dan OUT
* Tag yang belum terdaftar tidak boleh diproses sebagai barang keluar
* Data histori harus tersimpan agar prediksi bisa dilakukan
* Response API harus cepat → gunakan snapshot stok terbaru
* ML harus menggunakan data event yang konsisten dan bersih

---

## 8. Summary

Sistem ini terdiri dari:

* RFID Tag: identitas unik item
* IoT Scanner: membaca tag dan mengirim data
* Backend: memproses registrasi, scan, stok, dan validasi
* Database: menyimpan master data, event, snapshot, dan prediksi
* ML: memprediksi kapan stok akan habis
* Client: menampilkan hasil monitoring

Arsitektur ini memastikan sistem menjadi:

* Modular
* Scalable
* Maintainable
* Realistis untuk implementasi lapangan
* Cocok untuk monitoring stok bahan operasional berbasis RFID

```

