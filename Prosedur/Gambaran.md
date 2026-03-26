# 📘 System Overview - IoT Stock Prediction Application

## 1. System Purpose

Aplikasi ini adalah sistem berbasis IoT yang bertujuan untuk:

* Mengumpulkan data berat barang secara real-time menggunakan sensor (load cell)
* Mengonversi berat menjadi estimasi stok
* Menyimpan histori data untuk analisis
* Melakukan prediksi kapan stok akan habis menggunakan machine learning
* Menyediakan monitoring melalui aplikasi client (Android)

---

## 2. High-Level Architecture

```text
[ IoT Device (ESP32 + Load Cell) ]
                ↓
        [ Backend API (Ktor) ]
                ↓
        [ PostgreSQL (Neon) ]
                ↓
        [ ML Service (Python) ]
                ↓
        [ Backend API (Ktor) ]
                ↓
        [ Client App (Android) ]
```

---

## 3. Component Responsibilities

### 3.1 IoT Layer (ESP32 + Load Cell)

**Peran: Data Collector**

IoT hanya bertanggung jawab untuk:

* Membaca data dari sensor (load cell)
* Melakukan filtering sederhana (remove outlier, averaging)
* Mengirim data ke backend

IoT **tidak memiliki business logic**, tidak melakukan:

* Perhitungan stok
* Validasi kompleks
* Prediksi

**Data yang dikirim:**

```json
{
  "device_code": "DEV001",
  "filtered_weight": 12.53,
  "raw_weight": 12.80,
  "timestamp": "2026-03-24T22:00:00Z"
}
```

---

### 3.2 Backend (Ktor)

**Peran: Core System (Business Logic Layer)**

Backend adalah pusat dari seluruh sistem dan bertanggung jawab untuk:

#### a. Data Processing

* Menerima data dari IoT
* Menghitung estimasi stok:

```text
estimated_stock = floor(filtered_weight / unit_weight)
```

#### b. Validation

* Menolak data tidak valid (berat negatif, null, dll)
* Mendeteksi anomali (lonjakan berat tidak wajar)
* Menentukan status:

    * VALID
    * UNSTABLE
    * SUSPICIOUS
    * INVALID

#### c. Persistence

* Menyimpan data ke tabel:

    * `sensor_readings`
    * `stock_snapshots`

#### d. API Provider

* Menyediakan endpoint untuk:

    * IoT (`/sensor-data`)
    * Client (`/products`, `/stocks`, `/predictions`)
    * Admin (`/auth`, `/transactions`)

#### e. Integration Layer

* Menyediakan data untuk ML
* Mengambil hasil prediksi dari ML

---

### 3.3 Database (PostgreSQL - Neon)

**Peran: Single Source of Truth**

Database menyimpan:

* Data master (`products`, `iot_devices`)
* Data historis (`sensor_readings`)
* Data ringkasan (`stock_snapshots`)
* Hasil prediksi (`prediction_results`)
* Transaksi manual (`stock_transactions`)

Database digunakan untuk:

* Query real-time (melalui snapshot)
* Training data untuk ML
* Audit dan histori

---

### 3.4 Machine Learning Service (Python)

**Peran: Prediction Engine**

ML service berjalan terpisah dari backend (decoupled).

#### a. Input

Mengambil data dari database:

* `sensor_readings`
* difilter: hanya data VALID dan bukan anomali

#### b. Processing

* Mengubah data menjadi time-series
* Menggunakan model Linear Regression:

```text
stock(t) = m * t + b
```

#### c. Output

Menghasilkan:

* predicted_days_remaining
* predicted_stock_out_date
* confidence_score

#### d. Storage

Hasil disimpan ke:

* `prediction_results`

---

### 3.5 Client (Android - Jetpack Compose)

**Peran: Monitoring Interface**

Client hanya bertugas untuk:

* Menampilkan data (tidak ada business logic)
* Mengambil data dari backend API

#### Data yang ditampilkan:

* Berat terbaru
* Estimasi stok
* Status validasi
* Grafik histori
* Prediksi stok habis

---

## 4. Data Flow (End-to-End)

### 4.1 IoT to Backend

```text
IoT → POST /sensor-data → Backend
```

Backend:

* Validasi data
* Hitung estimasi stok
* Simpan ke database

---

### 4.2 Backend to Database

```text
Backend → INSERT sensor_readings
        → UPDATE stock_snapshots
```

---

### 4.3 Database to ML

```text
ML Service → SELECT sensor_readings (filtered)
```

---

### 4.4 ML to Database

```text
ML → INSERT prediction_results
```

---

### 4.5 Backend to Client

```text
Client → GET /stocks
Client → GET /predictions
```

Backend mengambil:

* snapshot terbaru
* hasil prediksi

---

## 5. Communication Strategy

### IoT → Backend

* REST API (HTTP)
* JSON payload
* Stateless

---

### Backend ↔ Database

* JDBC (PostgreSQL)
* Connection pooling (HikariCP)

---

### ML ↔ Database (Recommended)

* Direct DB access (read/write)
* Tidak perlu API call untuk efisiensi

---

### Client → Backend

* REST API
* JSON response

---

## 6. Design Principles

* IoT hanya sebagai data collector
* Backend sebagai pusat logika
* ML terpisah (loosely coupled)
* Client hanya sebagai viewer
* Database sebagai sumber data utama

---

## 7. Key Constraints

* Data IoT bersifat noisy → wajib validasi
* Sistem harus scalable (banyak device)
* Response API harus cepat → gunakan snapshot
* ML harus menggunakan data bersih

---

## 8. Summary

Sistem ini terdiri dari:

* IoT: mengumpulkan data
* Backend: memproses dan mengelola data
* Database: menyimpan semua data
* ML: memprediksi masa depan
* Client: menampilkan hasil

Arsitektur ini memastikan:

* Modular
* Scalable
* Maintainable
* Production-ready
