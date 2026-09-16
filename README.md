# CoffeeOS ERP — Android-Only Rewrite (Kotlin + Compose + Firebase)

> ERP coffeeshop: **Kasir + Gudang + Owner + Stock + Supply** dalam 1 APK multi-role,
> offline-first, printer Fake-PDF dulu (tanpa hardware).

Branch arsip web monorepo lama: `archive/web-monorepo`.
Spek konsep lama (RBAC, business flow, ERD): `docs/legacy-spec/`.

## Status

MVP-0 → MVP-4 selesai di level kode (belum dibuka di Android Studio — env tanpa JDK):

| Modul | Status |
|---|---|
| Auth | ✅ Email Firebase (Custom Claims role/tenant) + PIN cepat offline (hash) + demo |
| Kasir (checkout deduct BOM, validasi STOP, struk Fake PDF, PAID) | ✅ |
| Shift (buka modal, tutup rekonsiliasi, blokir saat pending sync) | ✅ |
| Kitchen Display (antrean Room + tombol Masak/Siap + antrean sync) | ✅ |
| Inventory (10%/2% + hysteresis, opname, terima PO, low-stock) | ✅ |
| Supplier + PO (DRAFT→APPROVED→RECEIVED, anti-ganda via poId) | ✅ |
| Owner (dashboard, approve PO, layar Konflik refund/paksa) | ✅ |
| Printer | ✅ Fake PDF (interface siap untuk ESC/POS Bluetooth) |
| Firebase Auth/Firestore/FCM realtime penuh | ✅ SyncWorker FIFO + transaction (ORDER/STOCK), FCM skeleton, Auth Firebase menyusul |
| Realtime listener Firestore (KDS live antar HP) | ✅ RealtimeSync 5 koleksi + InboundPolicy ter-test |

Verifikasi tanpa JDK: `python tools/verify_stock_rules.py` (10/10) +
`python tools/verify_mvp.py` (15/15) + `python tools/verify_sync.py` (9/9) +
`python tools/verify_inbound.py` (14/14) + `python tools/verify_auth.py` (17/17) —
cermin Python dari domain + sync Kotlin.
Setup login Firebase: `docs/firebase-auth.md` + `node tools/set-claims.js`.
Test JVM mirror: `StockRulesTest`, `ShiftCalculationTest`, `OrderTotalsTest`,
`PoCalculationTest`, `ConflictPolicyTest` → `./gradlew :app:testDebugUnitTest`.

Akun demo (PIN semua `123456`): `owner / admin / kasir / dapur / gudang`.

## Struktur

```
settings.gradle.kts / build.gradle.kts / app/build.gradle.kts
app/src/main/java/com/coffeeos/erp/
  MainActivity.kt, CoffeeosApp.kt
  ui/navigation/ (Routes, CoffeeosNavGraph — cabang per role)
  ui/theme/
  core/domain/auth/ (UserRole)
  core/domain/stock/ (StockRules — WARNING 10% / STOP 2%)
  core/domain/order/ (Receipt, ReceiptFormatter)
  core/data/local/ (AppDatabase: ingredients, menus, recipes, orders, pending_mutations)
  core/sync/ (SyncWorker via WorkManager)
  printing/ (PrinterRepository, FakePdfPrinter)
app/src/test/.../StockRulesTest.kt
firestore.rules + docs/firestore-schema.md
tools/verify_stock_rules.py (cermin Python, bisa jalan tanpa JDK)
```

## Cara Jalan (butuh Android Studio, karena env saat ini tanpa JDK)

1. Install Android Studio (JDK + SDK + Gradle bundled).
2. Copy `app/google-services.json.example` -> `app/google-services.json` isi dari Firebase Console.
3. Buka folder repo ini di Android Studio -> Sync Gradle -> Run `app` di emulator (minSdk 26).
4. Login demo: pilih role di layar awal (ganti Firebase Auth di MVP-1).
5. Cetak struk fake: file tersimpan di cache app (`struk-<orderId>.txt`) — siap dibagikan.

Verifikasi logika stok tanpa Android Studio:

```bash
python tools/verify_stock_rules.py
```

Unit test JVM (di Android Studio): `./gradlew :app:testDebugUnitTest`

## Aturan Stok (final v2)

`percent = currentStock / maxCapacity * 100` — WARNING ≤10% (tetap jual + alert PO),
STOP ≤2% (semua menu berbahan itu auto-mati). Nyala lagi ≥5%. Fallback absolut ≤5
bila `maxCapacity` tidak diketahui. Detail: `docs/firestore-schema.md`.

## Roadmap

MVP-1 Auth+Kasir+Shift → MVP-2 KDS → MVP-3 Supplier/PO → MVP-4 Owner+Konflik →
ganti FakePdfPrinter dengan BluetoothEscPosPrinter (interface sama, tanpa ubah kasir).
