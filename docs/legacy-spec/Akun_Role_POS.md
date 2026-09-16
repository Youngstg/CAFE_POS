# Daftar Akun & Role CoffeeOS POS

Berikut adalah daftar akun default yang dapat digunakan untuk masuk ke dalam sistem CoffeeOS POS. Kata sandi untuk semua akun di bawah ini adalah sama.

**Password Default untuk Semua Akun:** `password123`

| Peran (Role) | Email Login | Deskripsi Akses |
| :--- | :--- | :--- |
| **Owner** | `admin@coffeeos.com` | Memiliki kontrol penuh. Fokus pada **monitoring dan laporan keseluruhan**. Tidak bisa mengakses antarmuka layar Kasir, Dapur, maupun Input Stok secara langsung. |
| **Admin** | `cabangb@coffeeos.com` | Manajer / Admin Cabang. Memiliki akses ke Dashboard, Laporan Cabang, Manajemen Pegawai, serta **Input Stok Harian**. Tidak bisa mengakses layar Kasir maupun Dapur. |
| **Cashier** | `kasir@coffeeos.com` | Akun khusus penjaga kasir. Begitu masuk, langsung diarahkan ke layar **Terminal Kasir layar-penuh** (Full-Screen) untuk menerima pesanan. Tidak bisa melihat Dashboard. |
| **Kitchen** | `dapur@coffeeos.com` | Akun khusus layar dapur (KDS). Begitu masuk, langsung diarahkan ke **Kitchen Display** untuk melihat pesanan yang masuk dan menandai pesanan selesai. |

### Panduan Tambahan
- **Staff Management:** Admin atau Owner dapat membuat pegawai baru di halaman Manajemen Pegawai dan memilih peran (*role*) sesuai kebutuhan di atas.
- **Auto-Redirect:** Sistem akan secara otomatis mengarahkan layar (Kasir ke Kasir, Dapur ke Dapur) sehingga pegawai operasional tidak bingung melihat grafik dashboard.
