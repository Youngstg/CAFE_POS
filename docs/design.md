# Desain UI — CoffeeOS ERP

> Dokumen desain visual + panduan implementasi UI. Pasangan dari `description.md`.
> Level konsep (warna, tipografi, layout, komponen, flow) — implementasi Compose
> diserahkan ke developer.

## 1. Prinsip Desain

1. **Kasir dulu, estetika kedua.** Layar Kasir & Dapur dipakai berdiri sambil
   buru-buru di jam sibuk — target tap besar, kontras tinggi, minim langkah.
2. **Status selalu terlihat tanpa harus dibaca.** Warna dan bentuk badge harus
   bisa dipahami sekilas: stok aman/menipis/habis, order queued/cooking/ready,
   sync pending/selesai — semua pakai bahasa warna yang sama di seluruh app.
3. **Offline itu normal, bukan error.** UI tidak boleh terasa "rusak" saat
   offline — badge informatif, bukan alert merah menakutkan, kecuali benar-benar
   butuh perhatian (mis. shift tak bisa ditutup).
4. **Satu APK, lima wajah.** Owner/Admin butuh kepadatan informasi (tabel,
   dashboard); Kasir/Dapur/Gudang butuh kesederhanaan lapangan. Sistem desain
   sama, densitas beda per peran.
5. **Warm, bukan norak.** Nuansa coffeeshop — hangat, sedikit personal — tapi
   tetap fungsional seperti alat kerja, bukan aplikasi konsumen.

## 2. Palet Warna

### Warna Inti (Brand)
| Token | Hex | Pemakaian |
|---|---|---|
| `coffee-900` | `#3B2417` | Teks utama, header gelap, AppBar |
| `coffee-700` | `#6B4226` | Primary button, ikon aktif, tab terpilih |
| `coffee-500` | `#9C6644` | Aksen sedang, border aktif, progress |
| `latte-200` | `#E8D5C4` | Card surface sekunder, chip default |
| `latte-100` | `#F5EBE0` | Background utama (light) |
| `cream-50` | `#FBF7F2` | Background permukaan/card |

### Warna Sekunder
| Token | Hex | Pemakaian |
|---|---|---|
| `sage-700` | `#4B6350` | Aksen alternatif, ikon Gudang/Supply, header sekunder |
| `sage-200` | `#D3DFD4` | Background badge sukses lembut |

### Warna Semantik Status (dipakai konsisten di seluruh app)
| Status | Token | Hex | Konteks |
|---|---|---|---|
| Aman / Sukses | `status-safe` | `#4B6350` (sage-700) | Stok aman, PO received, sync selesai |
| Warning | `status-warning` | `#C98A2C` | Stok ≤10%, konflik belum diselesaikan |
| Stop / Kritis | `status-stop` | `#B3402F` | Stok ≤2%, menu mati, error |
| Info / Pending | `status-info` | `#6B7FA6` | Sync pending, PO draft, order queued |
| Netral | `status-neutral` | `#8A7B6D` | Nonaktif, arsip, disabled |

**Aturan:** jangan pakai merah murni (`#FF0000` dsb) — semua warna semantik
diturunkan dari palet earthy agar tetap terasa "coffeeshop", bukan alarm pabrik.

### Dark Mode (opsional, prioritas rendah)
Gunakan `coffee-900` sebagai base background, `latte-100` untuk teks, dan
warna semantik dinaikkan saturasinya sedikit agar tetap kontras di gelap.
Tidak wajib untuk versi pertama — Kasir/Dapur biasanya di lokasi terang.

## 3. Tipografi

- **Font:** satu keluarga font geometris-humanis yang jelas dibaca di jarak
  (mis. Inter / Plus Jakarta Sans / Manrope) — hindari font dekoratif/serif
  untuk elemen fungsional. Boleh pakai satu font display berkarakter
  (mis. Fraunces/Lora) HANYA untuk nama aplikasi di splash/login.
- **Skala:**
  | Peran | Ukuran relatif | Contoh pemakaian |
  |---|---|---|
  | Display | Terbesar | Nama produk di Kasir, total harga di cart |
  | Title | Besar | Judul layar, nama menu |
  | Body | Menengah | Deskripsi, label form |
  | Label/Caption | Kecil | Badge, timestamp, metadata |
- **Berat:** Bold untuk angka penting (harga, qty, sisa stok), Medium untuk
  judul, Regular untuk body. Jangan campur lebih dari 3 berat dalam satu layar.
- **Angka:** gunakan tabular figures untuk harga/stok agar kolom rapi saat
  berderet (laporan Owner, daftar stok).

## 4. Grid, Spacing & Bentuk

- **Skala spacing:** kelipatan 4 (4, 8, 12, 16, 24, 32, 48) — konsisten di
  seluruh layar agar ritme visual sama antar modul.
- **Radius:** sudut membulat sedang (mis. 12–16 untuk card, 8 untuk chip/badge,
  full-round untuk avatar & FAB) — kesan hangat, bukan tajam/korporat.
- **Elevasi:** card memakai elevasi tipis + border halus warna `latte-200`,
  bukan shadow berat — biar terasa ringan di layar terang toko.
- **Target sentuh:** minimal ukuran nyaman untuk tap cepat di Kasir/Dapur —
  lebih besar dari standar minimum karena dipakai sambil berdiri/terburu-buru.

## 5. Komponen Dasar

| Komponen | Catatan Desain |
|---|---|
| **Primary Button** | `coffee-700` solid, teks `cream-50`, dipakai untuk aksi utama per layar (Bayar, Simpan, Terima PO) |
| **Secondary Button** | Outline `coffee-700`, dipakai untuk aksi sekunder (Batal, Lihat Detail) |
| **Status Badge (pill)** | Background warna semantik versi 20% opacity + teks warna semantik penuh; ikon kecil di depan teks (●/▲/✕) |
| **Stat Card (Owner)** | Card `cream-50`, angka besar bold di atas, label kecil di bawah, ikon warna sesuai kategori |
| **Menu Card (Kasir)** | Foto/placeholder + nama + harga; state "habis" → overlay abu-abu + badge STOP, tidak bisa ditap |
| **Order Ticket (Dapur)** | Card lebar penuh, kolom kiri = info order, kanan = tombol aksi besar; border kiri tebal berwarna sesuai status |
| **Sync Badge** | Chip kecil pojok atas/bawah: "Offline • n pending" (warna info) → berubah hijau saat sinkron selesai |
| **Stock Row (Gudang)** | Nama bahan + progress bar tipis (isi = % stok tersisa, warna ikut status) + angka eksak di kanan |
| **PO Status Stepper** | DRAFT → APPROVED → RECEIVED ditampilkan sebagai stepper horizontal dengan warna terisi sesuai progres |
| **Empty/Offline State** | Ilustrasi garis sederhana bertema kopi (cangkir, biji kopi) — hangat, bukan ikon error generik |

## 6. Ikonografi
Gunakan satu set ikon outline konsisten (mis. Phosphor/Lucide). Untuk aksi
domain-spesifik (stok, resep, shift, PO) pilih ikon yang literal dan mudah
dikenali dari jarak — hindari ikon abstrak untuk aksi kritis seperti "Tutup
Shift" atau "Terima PO".

## 7. Navigasi & Percabangan Peran

Setelah login, root navigation bercabang otomatis per role (bottom nav atau
nav rail tergantung device):

| Role | Tab/Menu Utama |
|---|---|
| Kasir | Kasir (POS) → Keranjang → Shift |
| Dapur | Antrean Dapur (single screen, fullscreen board) |
| Gudang | Stok → Opname → Terima PO |
| Owner/Admin | Dashboard → Katalog → Supply → Gudang → Kasir (opsional lihat semua) |

**Prinsip:** Kasir & Dapur = sesedikit mungkin tab (fokus satu tugas).
Owner/Admin = nav lebih kaya karena butuh berpindah konteks antar modul.

## 8. Layout per Layar

### 8.1 Login
- Header hangat dengan nama aplikasi (font display) di atas latar `latte-100`.
- 3 pilihan jalur login sebagai card terpisah: Demo PIN, Email, PIN Cepat —
  bukan tab, karena jalur ini jarang dipakai bersamaan dan butuh kejelasan.
- Indikator role hanya muncul setelah berhasil login (tidak perlu dipilih manual).

### 8.2 Kasir (POS)
- **Layout 2 kolom (tablet) / stack (ponsel):** kiri grid Menu Card, kanan
  panel Keranjang persisten.
- Filter kategori sebagai chip horizontal scroll di atas grid menu.
- Menu habis (STOP) tetap tampil tapi non-aktif — jangan disembunyikan, agar
  kasir tahu kenapa tidak bisa jual, bukan mengira menu hilang.
- Panel Keranjang: daftar item + qty stepper besar, 1 slot promo aktif
  (dropdown/chip), tombol Bayar besar di bawah — selalu terjangkau ibu jari.
- Sync Badge menempel di AppBar, tidak mengganggu area tap utama.
- Jika shift belum dibuka: seluruh layar Kasir terkunci lembut dengan CTA
  "Buka Shift" di tengah — bukan dialog error.

### 8.3 Shift Kasir
- Buka shift: form sederhana satu field (modal awal) + tombol besar.
- Tutup shift: input kas fisik → tampilkan selisih otomatis dengan warna
  semantik (hijau pas, kuning selisih kecil, merah selisih besar).
- Jika terkunci karena masih ada antrean sync: tampilkan alasan eksplisit +
  jumlah item pending + tombol "Sync Sekarang", bukan tombol disabled tanpa
  penjelasan.

### 8.4 Dapur (Kitchen Display)
- Board 3 kolom horizontal: **QUEUED / COOKING / READY** (mirip kanban),
  full-screen, tanpa nav bar tambahan.
- Setiap kolom scrollable independen; Order Ticket besar dengan waktu tunggu
  relatif ("5 mnt lalu") untuk urgensi visual.
- Tombol aksi (Masak/Siap) sebesar mungkin — layar ini disentuh dengan
  tangan basah/kotor di dapur, prioritaskan ukuran di atas estetika.
- Update realtime antar HP → animasi pindah kolom halus, bukan reload penuh.

### 8.5 Gudang (Stok)
- List bahan dengan Stock Row (nama, progress bar, angka, badge status),
  bisa di-sort/filter by status (WARNING/STOP dulu di atas).
- Stock opname: mode edit terpisah dari mode lihat (toggle jelas), agar tidak
  ada input tak sengaja.
- Terima PO: form ringkas berbasis ID PO, tampilkan detail PO (dari Supply)
  sebelum konfirmasi — cegah salah terima.

### 8.6 Supply (Supplier & PO)
- Daftar Supplier sebagai list sederhana + tombol tambah (FAB).
- Daftar PO ditampilkan dengan PO Status Stepper per baris (DRAFT/APPROVED/
  RECEIVED) — status harus terbaca tanpa buka detail.
- PO ganda yang ditolak (idempotency): tampilkan toast/snackbar informatif
  "PO ini sudah diterima sebelumnya", bukan error generik.

### 8.7 Katalog (Owner/Admin)
- CRUD Menu: list + form tambah/edit sederhana (nama, harga, foto opsional).
- Editor Resep BOM: layout dua panel — kiri daftar bahan tersedia, kanan
  komposisi resep menu terpilih (drag/tap untuk tambah, stepper untuk takaran).
- Bahan yang masih dipakai resep → tombol hapus disabled dengan tooltip
  penjelasan, bukan hilang begitu saja.
- CRUD Promo: card promo dengan toggle aktif langsung terlihat (switch besar
  di pojok kanan card), badge tipe diskon (%, potongan tetap) berwarna beda.

### 8.8 Owner (Dashboard & Konflik)
- Dashboard: grid Stat Card (omzet, jumlah order, bahan warning/STOP, konflik,
  pending sync, shift aktif) — 2 kolom di ponsel, kaya info tapi tetap discan.
- Angka kritis (bahan STOP, konflik) selalu diberi warna semantik meski di
  ringkasan, agar owner langsung tahu yang butuh perhatian.
- Layar Konflik: tiap konflik sebagai card dengan konteks jelas (order mana,
  bahan apa, kenapa kalah rebutan stok) + dua tombol aksi besar berdampingan:
  **Refund** (outline, netral) vs **Paksa Lunas** (solid, tegas) — hindari
  membuat salah satu terlihat "default"/lebih mudah ditekan tanpa disengaja.

## 9. Bahasa Status Terpadu

Karena banyak modul punya status masing-masing, gunakan bentuk visual yang
sama di semua tempat:

| Domain | Aman/Selesai | Perhatian | Kritis |
|---|---|---|---|
| Stok bahan | Badge hijau | Badge kuning (WARNING ≤10%) | Badge merah (STOP ≤2%) |
| Order Dapur | READY (hijau) | COOKING (kuning) | QUEUED lama (merah, jika lewat ambang waktu) |
| PO | RECEIVED (hijau) | APPROVED (kuning) | DRAFT lama (netral abu) |
| Sync | Selesai (hijau, badge hilang) | Pending (biru info) | Gagal berulang (merah) |
| Shift | Ditutup rapi (hijau) | Selisih kecil (kuning) | Selisih besar/terkunci (merah) |

## 10. Aksesibilitas & Kondisi Lapangan

- Kontras teks-background minimal setara AA, terutama untuk badge status
  (jangan andalkan warna saja — selalu sertai ikon/teks).
- Ukuran font dasar tidak boleh terlalu kecil — layar Kasir/Dapur sering
  dilihat sambil bergerak.
- Uji tampilan di kondisi cahaya toko yang terang (dekat jendela) — hindari
  kombinasi warna pastel tipis yang hilang kontras di bawah sinar matahari.

## 11. Motion & Interaksi

- Transisi antar tab: cepat, minim animasi berlebihan (utamakan kecepatan
  kerja di atas keindahan transisi).
- Perubahan status realtime (Dapur, Sync Badge, Stock Row) memakai animasi
  halus (fade/slide singkat) agar perubahan terasa "hidup", bukan berkedip
  tiba-tiba yang mengagetkan.
- Aksi destruktif (hapus menu, paksa lunas) selalu lewat konfirmasi ringkas
  (bottom sheet/dialog), bukan swipe-to-delete tanpa undo pada data finansial.

## 12. Dokumen Terkait
- `description.md` — deskripsi fitur & arsitektur aplikasi (sumber kebenaran fitur)
- `akun.md` — akun testing (lokal saja)
- `firebase-auth.md`, `firestore-schema.md` — referensi teknis backend
