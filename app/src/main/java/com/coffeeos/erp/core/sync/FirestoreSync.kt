package com.coffeeos.erp.core.sync

import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.data.repo.PoJson
import com.coffeeos.erp.core.data.session.SessionManager
import com.coffeeos.erp.core.domain.stock.IngredientStock
import com.coffeeos.erp.core.domain.stock.StockLevel
import com.coffeeos.erp.core.domain.stock.evaluateIngredient
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Hasil satu putaran sync. failed > 0 = ada yang ditunda (retry via backoff). */
data class SyncResult(val processed: Int, val failed: Int)

/**
 * Sync FIFO pending_mutations Room -> Firestore.
 * - Kritis (ORDER_UPSERT, STOCK_*): Firestore runTransaction (anti-oversell).
 * - Non-kritis: set(merge) idempoten (diulang aman, kunci = orderId/poId/shiftId).
 * - Kalah rebutan FIFO: order server + Room lokal ditandai CONFLICT_NEED_REVIEW
 *   -> muncul di layar Konflik Owner (opsi B semi-manual).
 * Tenant/outlet diambil dari sesi (1 outlet aktif per device).
 */
@Singleton
class FirestoreSync @Inject constructor(
    private val dao: PosDao,
    private val session: SessionManager,
) {
    private val db by lazy { Firebase.firestore }

    suspend fun syncOnce(): SyncResult {
        val s = session.session.first() ?: return SyncResult(0, 0)
        var ok = 0
        var failed = 0
        repeat(10) {
            val batch = dao.peekQueue(20)
            if (batch.isEmpty()) return SyncResult(ok, failed)
            var stop = false
            for (m in batch) {
                try {
                    val conflict = process(m.kind, m.payloadJson, s.tenantId, s.outletId)
                    if (conflict != null) {
                        // Kalah FIFO: catat lokal sebagai konflik (tidak enqueue ulang).
                        dao.updateOrderStatus(conflict, "CONFLICT_NEED_REVIEW")
                    }
                    dao.dequeue(m.mutationId)
                    ok++
                } catch (e: Exception) {
                    dao.bumpRetry(m.mutationId)
                    failed++
                    stop = true
                    break
                }
            }
            if (stop) break
        }
        return SyncResult(ok, failed)
    }

    /**
     * Proses 1 mutasi. Mengembalikan orderId jika mutasi itu kalah rebutan
     * dan harus ditandai konflik, null jika sukses normal.
     */
    private suspend fun process(
        kind: String,
        payload: String,
        tenantId: String,
        outletId: String,
    ): String? {
        fun col(name: String) = db.collection("tenants").document(tenantId)
            .collection("outlets").document(outletId).collection(name)

        when (kind) {
            "ORDER_UPSERT" -> {
                val orderId = str(payload, "orderId") ?: return null
                val total = num(payload, "total")?.toLong() ?: 0L
                val needs = PoJson.parse(payload).associate { it.first to it.second }
                if (needs.isEmpty()) {
                    col("orders").document(orderId)
                        .set(mapOf("status" to "QUEUED", "total" to total), SetOptions.merge()).await()
                    return null
                }
                // Transaction mengembalikan true jika kalah (stok server kurang).
                // maxCapacity lokal dibaca SEBELUM transaksi (DAO suspend).
                val localMax = needs.keys.associateWith { dao.ingredientById(it)?.maxCapacity }
                val loser = db.runTransaction { txn ->
                    val refs = needs.keys.map { col("ingredients").document(it) }
                    val curs = refs.map { txn.get(it).getDouble("currentStock") ?: 0.0 }
                    val isLoser = curs.zip(needs.values).any { (cur, need) -> cur < need }
                    if (isLoser) {
                        txn.set(
                            col("orders").document(orderId),
                            mapOf("status" to "CONFLICT_NEED_REVIEW", "total" to total),
                            SetOptions.merge()
                        )
                    } else {
                        refs.forEachIndexed { i, ref ->
                            val cur = curs[i]
                            val need = needs.values.elementAt(i)
                            val max = txn.get(ref).getDouble("maxCapacity")
                                ?: localMax[ref.id]
                            val eval = evaluateIngredient(
                                IngredientStock(ref.id, "", cur - need, max, "")
                            )
                            txn.set(
                                ref,
                                mapOf(
                                    "currentStock" to (cur - need).coerceAtLeast(0.0),
                                    "isLow" to (eval.level != StockLevel.HEALTHY),
                                    "isStopped" to (eval.level == StockLevel.CRITICAL_STOP),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                ),
                                SetOptions.merge()
                            )
                        }
                        txn.set(
                            col("orders").document(orderId),
                            mapOf("status" to "QUEUED", "total" to total),
                            SetOptions.merge()
                        )
                    }
                    isLoser
                }.await()
                if (loser) return orderId
                return null
            }
            "ORDER_PAID" -> {
                val orderId = str(payload, "orderId") ?: return null
                col("orders").document(orderId)
                    .set(mapOf("status" to "PAID"), SetOptions.merge()).await()
            }
            "ORDER_STATUS" -> {
                val orderId = str(payload, "orderId") ?: return null
                val status = str(payload, "status") ?: return null
                col("orders").document(orderId)
                    .set(mapOf("status" to status), SetOptions.merge()).await()
            }
            "STOCK_IN", "STOCK_OPNAME" -> {
                val ingId = str(payload, "ingredientId") ?: return null
                val qty = num(payload, "qty") ?: return null
                val localMax = dao.ingredientById(ingId)?.maxCapacity
                db.runTransaction { txn ->
                    val ref = col("ingredients").document(ingId)
                    val snap = txn.get(ref)
                    val max = snap.getDouble("maxCapacity") ?: localMax
                    val eval = evaluateIngredient(IngredientStock(ingId, "", qty, max, ""))
                    txn.set(
                        ref,
                        mapOf(
                            "currentStock" to qty,
                            "isLow" to (eval.level != StockLevel.HEALTHY),
                            "isStopped" to (eval.level == StockLevel.CRITICAL_STOP),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    null
                }.await()
            }
            "SHIFT_OPEN", "SHIFT_CLOSE" -> {
                val shiftId = str(payload, "shiftId") ?: return null
                val sh = dao.shiftById(shiftId) ?: return null
                col("shifts").document(shiftId).set(
                    mapOf(
                        "openedBy" to sh.openedBy,
                        "modalAwal" to sh.modalAwal,
                        "paidTotal" to sh.paidTotal,
                        "countedCash" to (sh.countedCash ?: 0L),
                        "difference" to (sh.difference ?: 0L),
                        "isClosed" to sh.isClosed
                    ),
                    SetOptions.merge()
                ).await()
            }
            "PO_CREATE", "PO_APPROVE", "PO_RECEIVE" -> {
                val poId = str(payload, "poId") ?: return null
                val po = dao.poById(poId) ?: return null
                col("purchaseOrders").document(poId).set(
                    mapOf(
                        "supplierId" to po.supplierId,
                        "items" to PoJson.parse(po.itemsJson).map { (id, qty) ->
                            mapOf("ingredientId" to id, "qty" to qty)
                        },
                        "total" to po.total,
                        "status" to po.status
                    ),
                    SetOptions.merge()
                ).await()
            }
            "SUPPLIER_UPSERT" -> {
                val supId = str(payload, "supplierId") ?: return null
                val sup = dao.supplierById(supId) ?: return null
                col("suppliers").document(supId).set(
                    mapOf("name" to sup.name, "phone" to sup.phone, "address" to sup.address),
                    SetOptions.merge()
                ).await()
            }
            "CONFLICT_RESOLVE" -> {
                val orderId = str(payload, "orderId") ?: return null
                val refund = bool(payload, "refund") ?: true
                col("orders").document(orderId).set(
                    mapOf("status" to if (refund) "REFUNDED" else "PAID"),
                    SetOptions.merge()
                ).await()
            }
            else -> { /* kind tak dikenal: anggap sukses agar antrean tidak macet */ }
        }
        return null
    }

    private fun str(json: String, key: String): String? =
        Regex(""""$key"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1)

    private fun num(json: String, key: String): Double? =
        Regex(""""$key"\s*:\s*(-?[0-9.]+)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull()

    private fun bool(json: String, key: String): Boolean? =
        Regex(""""$key"\s*:\s*(true|false)"""").find(json)?.groupValues?.get(1)
            ?.let { it == "true" }
}
