Buatkan fitur forecasting stok menggunakan Prophet, menggantikan pendekatan regresi linear sebelumnya.

Konteks:

* Sebelumnya sistem memakai regresi linear / forecasting sederhana.
* Data yang tersedia saat ini masih sedikit, sekitar 30 data, sehingga hasil prediksi dengan regresi linear belum stabil dan kurang akurat.
* Karena itu, gunakan Prophet untuk forecasting time series yang lebih cocok untuk menangkap tren dan pola musiman.

Kebutuhan:

1. Gunakan data historis hasil aggregate harian dari RFID scan, khususnya total OUT per hari sebagai target prediksi.
2. Format data untuk Prophet:

   * ds = tanggal
   * y = total_out
3. Bangun proses forecasting untuk memprediksi kebutuhan stok ke depan, misalnya 7, 14, atau 30 hari.
4. Prophet harus mampu menangkap:

   * tren jangka panjang
   * seasonality mingguan / bulanan / tahunan
   * efek event khusus jika ada, seperti promo atau hari besar
5. Jika data masih sedikit, sistem harus memberi warning bahwa prediksi masih bersifat awal / belum stabil.
6. Jika ada missing date, lakukan normalisasi data agar time series tetap konsisten.
7. Output forecasting harus berisi:

   * tanggal prediksi
   * nilai prediksi
   * lower bound
   * upper bound
8. Gunakan hasil forecast ini untuk membantu rekomendasi stok / planning persediaan.

Tambahan:

* Jangan pakai raw RFID log langsung.
* Gunakan data yang sudah di-aggregate per hari.
* Fokus utama prediksi adalah demand / pemakaian barang, bukan stok snapshot.
