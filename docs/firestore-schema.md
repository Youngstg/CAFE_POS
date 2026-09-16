# Firestore Schema — CoffeeOS ERP

Semua di-scope: `/tenants/{tenantId}/outlets/{outletId}/{collection}/{doc}`.

## Koleksi

- `users/{uid}`: `{ role: owner|admin_outlet|cashier|kitchen|warehouse, pinHash, outletId }`
  Role juga di Firebase Auth Custom Claims (`role`, `tenantId`) untuk Security Rules.
- `ingredients/{ingId}`: `{ name, currentStock, maxCapacity, unit, isLow, isStopped, updatedAt }`
- `menus/{menuId}`: `{ name, price, isAvailable, updatedAt }`
- `recipes/{menuId}_{ingId}`: `{ menuId, ingredientId, qtyPerPortion }` (BOM)
- `orders/{orderId}`: `{ status: QUEUED|COOKING|READY|PAID|CONFLICT_NEED_REVIEW, items[], total, createdAt, deviceId }`
- `shifts/{shiftId}`: `{ openedBy, modalAwal, total, closedAt, selisih, pendingSync }`
- `suppliers/{supId}`: `{ name, phone, address }`
- `purchaseOrders/{poId}`: `{ supplierId, items[], status: draft|approved|received, createdBy }`
  `poId` = idempotency key cegah terima ganda.
- `stockMutations/{mutId}`: `{ ingredientId, type: IN|OUT|OPNAME|LOW_ALERT|OUT_OF_STOCK, qty, actorId, refId, createdAt }`

## Aturan tulis kritis (lihat firestore.rules)

- Stok/PO/shift: hanya `owner|admin_outlet|warehouse` (+ transaction client).
- Orders: kasir/dapur boleh tulis status order saja.
- Deduct terakhir + terima PO + tutup shift wajib `runTransaction` + cek ulang server.

## Sinkron offline-first

Room = source of truth. Setiap tulis lokal enqueue `pending_mutations`
(`mutationId` UUID). `SyncWorker` (WorkManager, `NetworkType.CONNECTED`,
backoff exponential) mengirim FIFO. Konflik kalah -> order `CONFLICT_NEED_REVIEW`
masuk layar Konflik Owner.
