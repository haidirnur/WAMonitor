# WA Monitor

Aplikasi Android (Kotlin) untuk memantau notifikasi WhatsApp dari kontak/nomor
tertentu. Pesan yang cocok akan dicatat dan ditampilkan di dalam app.

## Cara membuka project
1. Buka Android Studio (versi terbaru, minimal Iguana/2023.2+).
2. File > Open > pilih folder `WAMonitor`.
3. Tunggu Gradle sync selesai (butuh koneksi internet untuk download dependency).

## Cara menjalankan
1. Sambungkan HP Android (minSdk 24 / Android 7.0 ke atas) atau pakai emulator.
2. Run > Run 'app'.

## Cara pakai di HP
1. Buka app **WA Monitor**.
2. Tap **"Buka Pengaturan Akses Notifikasi"** — ini akan membuka halaman
   sistem Android (Settings > Notification access). Cari "WA Monitor" di
   daftar, aktifkan. Android **tidak** mengizinkan permission ini lewat
   dialog biasa, harus manual lewat Settings.
3. Kembali ke app, isi kolom target dengan nama kontak persis seperti
   tersimpan di kontak HP kamu (mis. "Budi Kantor"), atau nomor mentah kalau
   kontak belum tersimpan (mis. "+62812xxxxxxx" — sesuai apa yang tampil
   sebagai judul notifikasi WhatsApp). Bisa isi beberapa, pisah pakai koma.
4. Tap **Simpan Target**.
5. Setiap notifikasi WhatsApp yang judulnya cocok (mengandung teks target,
   tidak case-sensitive) akan otomatis tercatat dan muncul di daftar.

## Catatan teknis
- Menggunakan `NotificationListenerService` bawaan Android — ini API resmi,
  bukan reverse-engineering WhatsApp.
- Data disimpan lokal di HP pakai Room (SQLite), tidak dikirim ke server
  manapun.
- Notifikasi ringkasan grup ("X pesan baru") otomatis diabaikan karena
  tidak punya isi pesan yang jelas.
- Kalau WhatsApp/WhatsApp Business menerapkan enkripsi tampilan notifikasi
  (mis. mode privasi "Sembunyikan konten notifikasi" di setelan WA), isi
  pesan yang tertangkap bisa jadi kosong/generik — pastikan setelan privasi
  notifikasi WA di HP kamu menampilkan preview pesan.
- Karena ini menyangkut privasi (membaca notifikasi orang lain), pastikan
  penggunaannya sesuai hukum privasi/ITE yang berlaku dan hanya untuk nomor
  yang memang berhak kamu pantau (mis. nomor sendiri di HP lain, atau
  dengan izin pemilik nomor).

## Setup Firebase (WAJIB agar bisa sinkron antar HP)

App ini pakai Firebase Firestore supaya pesan yang tertangkap di satu HP
otomatis muncul di HP lain. Anda perlu buat project Firebase sendiri
(gratis) dan download 1 file konfigurasi:

1. Buka https://console.firebase.google.com, login pakai akun Google Anda.
2. **Add project** → kasih nama bebas (mis. "WA Monitor") → lanjutkan
   sampai selesai (boleh matikan Google Analytics, tidak wajib).
3. Di dashboard project, klik ikon **Android** (</>) untuk tambah app Android.
4. Isi **Android package name** dengan persis: `com.ucok.wamonitor`
   (harus sama persis, ini yang menghubungkan app ke project Firebase).
5. Download file **`google-services.json`** yang ditawarkan.
6. Taruh file itu di dalam folder `app/` (sejajar dengan `build.gradle.kts`
   yang ada di dalam folder `app`), jadi path-nya:
   `WAMonitor/app/google-services.json`
7. Di GitHub, upload file tersebut ke folder `app/` di repo Anda (Add file
   > Upload files, drag file `google-services.json` ke situ, commit).
8. Masih di Firebase Console, buka menu **Firestore Database** di sidebar
   kiri → **Create database** → pilih lokasi (mis. asia-southeast) →
   mode **Start in test mode** (biar tidak perlu setup security rules
   dulu; cukup untuk pemakaian pribadi).
9. Push/commit lagi ke GitHub → Actions akan build ulang dengan Firebase
   sudah terpasang.

Setelah itu, install APK hasil build ke HP mana saja — selama pakai APK
yang sama (dengan `google-services.json` yang sama), semua HP akan lihat
feed pesan yang sama secara real-time.

⚠️ **Penting soal privasi:** karena app ini tidak pakai sistem login, semua
orang yang punya APK dengan konfigurasi Firebase yang sama bisa membaca
data yang sama di Firestore. Cukup aman untuk dipakai sendiri/keluarga,
tapi jangan sebarkan APK ke orang luar kalau tidak mau mereka bisa akses
data yang sama. Kalau butuh keamanan lebih (login per user, dsb), itu
pengembangan tambahan.

## Build APK otomatis lewat GitHub Actions (tanpa Android Studio)

Project ini sudah dilengkapi workflow `.github/workflows/build.yml` yang
otomatis build APK setiap kali Anda push ke branch `main`/`master`.

Langkah-langkah:
1. Buat repository baru di GitHub (bisa private).
2. Push seluruh isi folder `WAMonitor` ke repo tersebut:
   ```bash
   cd WAMonitor
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/USERNAME/NAMA_REPO.git
   git push -u origin main
   ```
3. Buka repo di GitHub → tab **Actions**. Workflow "Build Debug APK" akan
   otomatis berjalan (butuh 3-5 menit).
4. Setelah selesai (centang hijau), klik run tersebut → scroll ke bagian
   **Artifacts** → download `WAMonitor-debug-apk` (berupa .zip berisi
   `app-debug.apk`).
5. Extract zip-nya, pindahkan `app-debug.apk` ke HP, lalu install (mungkin
   perlu izinkan "Install dari sumber tidak dikenal" di HP Anda).

Kalau tidak mau push otomatis tiap commit, workflow ini juga bisa dipicu
manual lewat tab Actions > "Build Debug APK" > **Run workflow** (karena ada
`workflow_dispatch` di konfigurasinya).

## Struktur project
```
app/src/main/java/com/ucok/wamonitor/
  data/       -> Entity, DAO, Room DB, SharedPreferences helper
  service/    -> NotificationListenerService (inti penangkap notifikasi)
  ui/         -> MainActivity + RecyclerView adapter
```
