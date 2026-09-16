# Firebase Auth — CoffeeOS ERP

Login 3 jalur (lihat `AuthRepository`):

1. **Email (produksi):** `signInWithEmailAndPassword` → `getIdToken(true)` →
   Custom Claims `role`, `tenantId`, `outletId`, `displayName` → sesi DataStore.
   Wajib online sekali; setelah itu sesi + listener bertahan offline.
2. **PIN cepat (kasir offline):** setelah login email, buat PIN 4+ digit
   (hash SHA-256 `uid:pin` di DataStore). Masuk berikutnya cukup PIN, tanpa internet.
3. **Demo (evaluator tanpa Firebase):** `owner/admin/kasir/dapur/gudang`, PIN `123456`.

## Setup Firebase (sekali)

1. Firebase Console → buat project → Authentication → aktifkan provider **Email/Password**.
2. Buat user (satu per staff): Authentication → Users → Add user.
3. Service account: Project settings → Service accounts → Generate key →
   simpan sebagai `serviceAccount.json` (JANGAN commit).
4. Set claims per user:
   ```bash
   npm i -g firebase-admin   # atau: npm i firebase-admin di tools/
   GOOGLE_APPLICATION_CREDENTIALS=./serviceAccount.json node tools/set-claims.js kasir@tokomu.com CASHIER tenant-1 outlet-1 "Kasir 1"
   ```
5. Android: `google-services.json` dari Console → `app/google-services.json`
   (lihat `app/google-services.json.example`).
6. User login ulang di HP agar token membawa claims baru.

## Kenapa Custom Claims (bukan koleksi users)?

`firestore.rules` membaca `request.auth.token.role` / `.tenantId` langsung di
server — tidak bisa dipalsukan client. Koleksi `users/{uid}` tetap ada sebagai
profil baca, tapi otorisasi tulis mengacu ke token.

## Catatan keamanan porto

- PIN cepat = kunci lokal saja (kalah penting dari password server). Hash, bukan plaintext.
- Password tidak pernah disimpan di aplikasi.
- Logout menghapus sesi + PIN + melepas listener realtime.
