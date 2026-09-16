# CoffeeOS ERP — Android-Only Rewrite (Kotlin + Compose + Firebase)

> ERP coffeeshop: **Kasir + Gudang + Owner + Stock + Supply** dalam 1 APK multi-role,
> offline-first, printer Fake-PDF dulu (tanpa hardware).

Branch arsip web monorepo lama: `archive/web-monorepo`.
Spek konsep lama (RBAC, business flow, ERD): `docs/legacy-spec/`.

## Status

MVP-0 scaffold selesai: navigasi role, entity Room, aturan stok 10%/2% + unit test,
abstraksi printer (Fake PDF), schema Firestore + rules.

| Modul | Status |
|---|---|
| Auth + RBAC (PIN, Custom Claims) | ⏳ berikutnya (MVP-1) |
| Kasir + Order + Shift | ⏳ MVP-1 |
| Kitchen Display realtime | ⏳ MVP-2 |
| Inventory (BOM deduct, 10%/2%) | ✅ domain + test |
| Supplier + PO + Stock In | ⏳ MVP-3 (Room entity + Firestore col sudah disiapkan) |
| Owner dashboard + layar Konflik | ⏳ MVP-4 |
| Printer | ✅ Fake PDF (teks struk siap untuk ESC/POS asli) |

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
