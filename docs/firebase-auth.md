# Firebase Auth — CoffeeOS ERP

Login 3 jalur (lihat `AuthRepository`):

1. **Email (produksi):** `signInWithEmailAndPassword` → `getIdToken(true)` →
   Custom Claims `role`, `tenantId`, `outletId`, `displayName` → sesi DataStore.
   Wajib online sekali; setelah itu sesi + listener bertahan offline.
2. **PIN cepat (kasir offline):** setelah login email, buat PIN 4+ digit
   (hash SHA-256 `uid:pin` di DataStore). Masuk berikutnya cukup PIN, tanpa internet.
3. **Demo (evaluator tanpa Firebase):** `owner/admin/kasir/dapur/gudang`, PIN `123456`.

## Setup Firebase (sekali, ±10 menit — harus manusia, tidak bisa otomatis)

> Service account key & user hanya bisa dibuat pemilik project via Console.
> Setelah itu semua otomatis via 1 perintah.

1. Console → project `cafepos-b3cb8` → **Authentication** → Sign-in method →
   aktifkan **Email/Password**.
2. Console → **Firestore Database** → tab **Rules** → ganti SEMUA isi
   (saat ini `allow read, write: if false`) dengan isi file `firestore.rules`
   di repo ini → **Publish**.
3. Console → Project settings → Service accounts → **Generate new private key** →
   simpan sebagai `serviceAccountKey.json` di folder `tools/`
   (**JANGAN commit** — sudah di `.gitignore`).
4. Provisioning sekali-jalan (bikin 5 user + claims, boleh diulang):
   ```bash
   cd tools && npm i firebase-admin@12   # v12 CommonJS; v13+ ESM-only
   cd .. && node tools/provision.js ./tools/serviceAccountKey.json "Cafe1234!" tenant-1 outlet-1
   ```
   Hasil: `owner/admin/kasir/dapur/gudang @cafepos.local` + claims sesuai peran.
   Untuk 1 user saja: `tools/set-claims.js`.
5. Android: `app/google-services.json` sudah cocok (paket `com.cafe.pos` =
   `applicationId`). Tidak perlu download ulang kecuali ganti project.
6. Login di HP tab **Email**, misal `kasir@cafepos.local` / `Cafe1234!`.

CATATAN: snippet `databaseURL: ...firebasedatabase.app` dari Firebase boleh
diabaikan — itu Realtime Database, kita memakai **Firestore** (Auth Admin SDK
tidak butuh databaseURL).

## Kenapa Custom Claims (bukan koleksi users)?

`firestore.rules` membaca `request.auth.token.role` / `.tenantId` langsung di
server — tidak bisa dipalsukan client. Koleksi `users/{uid}` tetap ada sebagai
profil baca, tapi otorisasi tulis mengacu ke token.

## Catatan keamanan porto

- PIN cepat = kunci lokal saja (kalah penting dari password server). Hash, bukan plaintext.
- Password tidak pernah disimpan di aplikasi.
- Logout menghapus sesi + PIN + melepas listener realtime.
