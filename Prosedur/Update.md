# 📝 Update Implementasi Frontend (V4.1)

Dokumen ini berisi panduan untuk Frontend Developer mengenai perubahan terbaru di Backend untuk fitur Reporting, Bar Chart, dan Fix Threshold.

---

## 1. Perubahan Logika Threshold (Status Stok)
**PENTING:** Ada perubahan operator perbandingan di Backend.
- **Dulu:** `stok < threshold` baru LOW_STOCK.
- **Sekarang:** `stok <= threshold` sudah LOW_STOCK.
- **Contoh:** Jika `minStockThreshold` produk adalah **5**, dan stok saat ini adalah **5**, maka status adalah `LOW_STOCK`.
- **Action:** Pastikan di Frontend tidak ada hardcode logika `if (stok < 5)`. Cukup tampilkan field `status` yang dikirim dari API (`SUFFICIENT`, `LOW_STOCK`, `OUT_OF_STOCK`).

---

## 2. Implementasi Bar Chart (Perbandingan Bulan)
Untuk membuat Bar Chart "Bulan Ini vs Bulan Lalu vs 2 Bulan Lalu", gunakan endpoint:
`GET /api/v1/inventory/{productId}/monthly-summary?month=YYYY-MM`

**Cara Request (Frontend Logic):**
Untuk menampilkan 3 bar perbandingan, panggil API ini 3 kali secara paralel (misal menggunakan `Promise.all` di Axios/Fetch):
1. **Bulan Ini (Mei 2026):** `GET .../monthly-summary?month=2026-05`
2. **Bulan Lalu (April 2026):** `GET .../monthly-summary?month=2026-04`
3. **2 Bulan Lalu (Maret 2026):** `GET .../monthly-summary?month=2026-03`

**Contoh Response dari Backend:**
```json
{
  "success": true,
  "data": {
    "productId": "7d698ad1...",
    "month": "2026-04",
    "totalIn": 116,
    "totalOut": 387,
    "netFlow": -271,
    "dailyBreakdown": [...]
  }
}
```

**Data yang diplot ke Chart:**
- Ambil nilai `data.totalIn` (Barang Masuk) dan `data.totalOut` (Barang Keluar) dari masing-masing response.
- Jadikan Label X-Axis sebagai bulannya (Maret, April, Mei).
- Jika response `404` (produk baru dibuat dan belum ada di bulan itu), anggap `totalIn` dan `totalOut` adalah `0`.

---

## 3. Implementasi Line Chart & Prediksi (Prophet)
Endpoint: `GET /api/v1/inventory/chart-data/{productId}`

**Konfigurasi Range:**
- **Historical Data:** Mengembalikan data **30 hari terakhir** (History).
- **Forecasting Data:** Mengembalikan data **30 hari ke depan** (Prediksi ML Prophet).
- **Total Range:** 60 titik data (30 history + 30 forecast).
- **Action:** Gunakan Chart.js atau Recharts. Bedakan warna garis untuk data History (solid) dan data Forecast (dashed/putus-putus) agar user tidak bingung.

---

## 4. Ekspor Laporan Excel (Harian)
Endpoint: `GET /api/v1/reports/daily?date=YYYY-MM-DD`

**Contoh Response:**
```json
{
  "success": true,
  "data": {
    "date": "2026-05-10",
    "products": [
      { "productName": "Kopi", "totalIn": 5, "totalOut": 3, "currentStock": 15, "status": "SUFFICIENT" }
    ],
    "grandTotalIn": 20,
    "grandTotalOut": 12,
    "scanLogs": [
      { "eventType": "OUT", "productName": "Kopi", "quantity": 1, "recordedAt": "2026-05-10T14:30:00" }
    ]
  }
}
```

**Cara Kerja (Frontend Logic):**
1. User memilih tanggal di Kalender/Datepicker, lalu klik "Export Excel".
2. Frontend memanggil API: `GET /api/v1/reports/daily?date=2026-05-10`.
3. Gunakan library seperti `xlsx` (SheetJS) atau `exceljs` di Frontend.
4. **Buat 2 Sheet Excel:**
   - **Sheet 1 (Ringkasan):** Looping array `data.products`. Kolomnya: Nama Produk, Unit, Masuk (`totalIn`), Keluar (`totalOut`), Sisa Stok (`currentStock`), Status.
   - **Sheet 2 (Log Detail):** Looping array `data.scanLogs`. Kolomnya: Waktu (`recordedAt`), Tipe (`eventType`), Produk, Qty, Catatan.
5. Trigger auto-download file `.xlsx` di browser user.

---

## 5. Sinkronisasi Data (Manual Sync)
Admin memiliki tombol **"Sync Data"**.
- Endpoint: `POST /api/admin/inventory/aggregate/sync`
- Fungsi: Memaksa backend menghitung ulang agregasi hari ini.
- **User Experience:** Tampilkan loading state saat proses sync, lalu refresh data dashboard setelah sukses. Beri tahu user bahwa ML perlu waktu ~5 menit untuk memproses ulang prediksi setelah sync.

---

**Dokumentasi API Lengkap:** Lihat `API_DOCUMENTATION.md` seksi 5.4 dan 5.5.
