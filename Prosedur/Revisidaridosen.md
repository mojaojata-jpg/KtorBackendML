Buatkan backend feature untuk aggregasi data RFID scan log menjadi data harian.

Kebutuhan:

1. Ambil data raw RFID log yang berisi:

   * timestamp
   * product_id
   * type (IN / OUT)

2. Buat proses aggregate harian yang:

   * mengelompokkan data berdasarkan tanggal dan product_id
   * menghitung total IN
   * menghitung total OUT
   * menghitung net_flow = total_in - total_out

3. Simpan hasil aggregate ke tabel baru, misalnya:

   * date
   * product_id
   * total_in
   * total_out
   * net_flow
   * created_at
   * updated_at

4. Proses aggregate harus dijalankan otomatis menggunakan scheduler / cron job.

   * Jalankan 1x setiap hari pada jam tertentu, misalnya 23:59
   * Tambahkan juga endpoint manual untuk menjalankan aggregate secara on-demand saat testing atau maintenance

5. Gunakan clean architecture:

   * controller / handler
   * service / usecase
   * repository
   * scheduler / job runner

6. Pastikan:

   * tidak ada duplicate data aggregate untuk tanggal dan product_id yang sama
   * jika data kosong, proses tetap aman
   * hanya data valid yang diproses
   * proses aggregate bisa diulang tanpa merusak data

7. Output yang diharapkan:

   * kode backend lengkap
   * struktur database
   * scheduler job
   * endpoint manual trigger
   * logika anti-duplicate / upsert
