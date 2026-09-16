# Arsitektur Android — CoffeeOS ERP

Single-module `:app` dengan paket Clean Architecture (dipecah multi-module saat membesar).

```
UI (Compose + Navigation + ViewModel)
  -> Domain (UseCase murni: StockRules, ReceiptFormatter — tanpa Android)
  -> Data (Repository: Room DAO + Firestore + DataStore)
  -> Sync (WorkManager SyncWorker + pending_mutations)
```

- **Offline-first:** UI collect `Flow` dari Room. Tulis lokal dulu, enqueue mutasi,
  sync belakangan. Indikator `Offline • n pending` global. `Tutup Shift` diblokir
  saat antrean belum kosong.
- **DI:** Hilt (`CoffeeosApp`, `PrinterRepository` binding Fake dulu).
- **Printer:** `PrinterRepository` interface. `FakePdfPrinter` (cache file, format dari
  `ReceiptFormatter`) -> nanti `BluetoothEscPosPrinter` tanpa ubah kasir.
- **Scan:** kamera + ML Kit (QR meja, barcode supplier). Scanner tembak = keyboard HID,
  tanpa kode khusus.
- **Notifikasi:** FCM untuk order baru (KDS) + stok kritis; notifikasi lokal saat offline.
- **Test:** `StockRulesTest` (JUnit) mirror `tools/verify_stock_rules.py`. Tambah
  ViewModel test + 1 UI test sebelum rilis porto.
