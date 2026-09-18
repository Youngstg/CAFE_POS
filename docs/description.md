# Deskripsi Aplikasi — CoffeeOS ERP

> Dokumen deskripsi + inventaris fitur. Referensi untuk `design.md`.
> **Tidak berisi kredensial apa pun** — akun testing ada di `akun.md` (lokal, tidak ikut git).

## Ringkasan

CoffeeOS ERP adalah aplikasi **ERP coffeeshop Android-native dalam 1 APK**:
Kasir (POS) + Gudang (stok) + Owner (laporan) + Stock + Supply (supplier & PO)
yang terintegrasi dan tersinkron antar device.

- **Platform:** Android only (minSdk 26), Kotlin + Jetpack Compose + Material3
- **Arsitektur:** MVVM + Clean Architecture + Hilt, offline-first (Room sebagai
  source of truth, Firestore untuk sync antar HP via WorkManager FIFO)
- **Backend:** Firebase (Auth + Firestore + FCM + Crashlytics), `applicationId com.cafe.pos`
- **Peran (RBAC, 1 APK):** Owner, Admin Cabang, Kasir, Dapur, Gudang —
  navigasi bercabang otomatis setelah login

## Fitur per modul

### Auth & Sesi
- Login 3 jalur: Demo PIN offline, Email Firebase (role/tenant/outlet dari
  Custom Claims), PIN cepat offline (hash SHA-256, dibuat sekali setelah login email)
- Sesi tersimpan di DataStore (tetap login saat offline); logout menghapus
  sesi + PIN + melepas listener realtime
- Seed data demo otomatis saat login pertama (bahan, menu + resep, supplier, promo)

### Kasir (POS)
- Daftar menu + status jual/habis otomatis mengikuti aturan stok 10%/2%
- Keranjang (tambah, hitung qty, bersihkan), pilih 1 promo aktif, bayar tunai
- Struk Fake-PDF tersimpan di cache aplikasi, siap dibagikan
- Badge "Offline • n pending" + tombol "Sync sekarang"
- Jualan mensyaratkan shift aktif

### Shift Kasir
- Buka shift (modal awal), tutup shift (input kas fisik → selisih otomatis),
  riwayat shift per outlet
- Tutup shift **diblokir** selama masih ada antrean sync yang belum terkirim

### Dapur (Kitchen Display)
- Antrean order QUEUED/COOKING/READY yang terupdate realtime antar HP
- Tombol Masak → Siap per order; setiap perubahan masuk antrean sync

### Gudang (Stok)
- Daftar stok berkode status (aman / WARNING ≤10% / STOP ≤2%)
- Stock opname fisik, ubah kapasitas acuan 10%/2% (+satuan), terima PO per ID
- Daftar bahan menipis sebagai dasar pembuatan PO

### Supply (Supplier & PO)
- Tambah supplier; Purchase Order beralur DRAFT → APPROVED → RECEIVED
- Terima PO ganda ditolak otomatis (idempotency via ID PO)
- Menerima PO menambah stok dan dapat menyalakan kembali menu yang mati

### Katalog (Owner/Admin)
- CRUD menu (nama + harga; hapus menu ikut membersihkan resepnya di server)
- Editor resep BOM per menu (takaran per porsi; bahan yang masih dipakai
  resep tidak bisa dihapus)
- CRUD promo (diskon %, potongan tetap, minimal order) + toggle aktif

### Owner (Dashboard & Konflik)
- Ringkasan: omzet lunas, jumlah order, bahan warning/STOP, konflik,
  pending sync, shift aktif
- Layar Konflik (semi-manual): order yang kalah rebutan stok diselesaikan
  dengan Refund atau Paksa Lunas

### Fondasi tak terlihat
- Offline-first penuh: tulis lokal dulu, sync belakangan (batas aman shift)
- Sync keluar FIFO + transaksi Firestore anti-oversell; sync masuk realtime
  8 koleksi (order, bahan, menu, resep, PO, shift, supplier, promo)
- Aturan konflik: lokal pending menang, bahan last-write-wins, hapus katalog
  disebarkan, arsip order dipertahankan
- Notifikasi FCM 3 kanal (order masuk, stok kritis, PO disetujui)
- Security Rules Firestore per peran + tenant

## Aturan bisnis utama
- Stok: WARNING ≤10% (tetap jual + anjuran PO), STOP ≤2% (menu terkait mati
  otomatis), nyala lagi ≥5%; fallback absolut ≤5 bila kapasitas tak diketahui
- Semua menu yang memakai bahan STOP ikut mati; order yang terlanjur masuk
  tetap dimasak sampai selesai

## Dokumen terkait
- `design.md` — desain UI (milik perancang, terpisah dari file ini)
- `akun.md` — akun testing (LOKAL SAJA, tidak ikut git)
- `firebase-auth.md` — setup login Firebase; `firestore-schema.md` — skema data
- `legacy-spec/` — spek konsep awal (arsip)
