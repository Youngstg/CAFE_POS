# Business Flow — CoffeeOS

## 1. Alur Order via Web (Kasir/Customer)

```
[Customer datang] 
        │
        ├── Scan QR Code ──► E-Menu (React Web)
        │                         │
        │                    Pilih Menu & Qty
        │                         │
        │                    Submit Order ──► POST /api/v1/orders
        │                                          │
        │                              Backend buat Order + OrderItems
        │                                          │
        │                              Kitchen Display refresh (polling)
        │                                          │
        │                              Kasir konfirmasi ──► PATCH /orders/{id}/status
        │
        └── Bayar ke kasir ──► POST /api/v1/transactions
                                        │
                              Stok di-deduct otomatis
                                        │
                              Shift balance di-update
```

---

## 2. Alur Chatbot (WhatsApp/Telegram)

```
[Customer kirim pesan WA/Telegram]
        │
        ▼
[chatbot-gateway menerima webhook]
        │
        ├── resolveRole() ──► GET /api/v1/identity/resolve
        │                           │
        │                    Return: role, tenantId
        │
        ├── LLM Orchestrator (Gemini/OpenAI)
        │       │
        │       ├── RAG Retrieval ──► GET /api/v1/chatbot/rag/retrieve
        │       │                   (konteks menu, promo, FAQ)
        │       │
        │       └── Function Calling
        │               │
        │               ├── get_menu ──► GET /api/v1/chatbot/menu
        │               ├── create_order ──► POST /api/v1/chatbot/orders
        │               ├── check_stock ──► GET /api/v1/chatbot/stock
        │               └── get_promo ──► GET /api/v1/chatbot/promos
        │
        └── Balas ke customer via WA/Telegram API
```

---

## 3. Alur Shift (Buka/Tutup Kasir)

```
[Kasir mulai shift]
        │
        ▼
POST /api/v1/shifts (buka shift, catat modal awal)
        │
        ▼
[Kasir proses transaksi sepanjang hari]
        │
        ▼
[Kasir tutup shift]
        │
        ▼
POST /api/v1/shifts/{id}/close
        │
        ▼
ShiftReconciliationService:
  - Hitung total pendapatan
  - Bandingkan dengan kas fisik
  - Generate laporan selisih
        │
        ▼
Owner bisa lihat laporan di dashboard
```

---

## 4. Alur Manajemen Stok

```
[Owner/Kasir input stok harian]
        │
        ▼
POST /api/v1/stock/daily-input
        │
        ▼
StockService:
  - Update saldo stok bahan baku
  - Catat riwayat mutasi
        │
        ▼
[Order masuk] ──► StockService.deduct()
                    Kurangi stok per bahan (via MenuRecipe)
                        │
                        ▼
              Jika stok < threshold ──► notifikasi owner
```

---

## 5. Alur Promo

```
[Owner buat promo]
        │
        ▼
POST /api/v1/promos
(tipe: diskon %, buy-X-get-Y, minimum order)
        │
        ▼
[Customer order]
        │
        ▼
PromoEngine.calculate(order, promos)
  - Cek eligibilitas promo
  - Hitung diskon
  - Return: harga akhir + promo yang dipakai
```
