package com.coffeeos.erp.core.data.repo

import com.coffeeos.erp.core.data.local.PendingMutation
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.domain.stock.IngredientStock
import com.coffeeos.erp.core.domain.stock.StockLevel
import com.coffeeos.erp.core.domain.stock.evaluateIngredient
import com.coffeeos.erp.core.domain.stock.shouldClearWarning
import com.coffeeos.erp.core.domain.stock.shouldReenable
import com.coffeeos.erp.core.domain.supply.PoCalculation
import com.coffeeos.erp.core.sync.SyncTrigger
import java.util.UUID
import javax.inject.Inject

/** Gudang: opname, adjust, terima PO (guard anti-ganda), daftar low-stock. */
class InventoryRepository @Inject constructor(
    private val dao: PosDao,
    private val sync: SyncTrigger,
) {

    fun observeIngredients(outletId: String) = dao.observeIngredients(outletId)

    suspend fun lowStock(outletId: String) =
        dao.listIngredients(outletId).filter { it.isLow || it.isStopped }

    /** Stock opname: set stok fisik hasil hitung manual. */
    suspend fun opname(ingredientId: String, physicalStock: Double, actorId: String) {
        require(physicalStock >= 0) { "Stok fisik tidak valid" }
        val e = dao.ingredientById(ingredientId) ?: throw IllegalArgumentException("Bahan tidak ada")
        applyStock(e.id, physicalStock, "OPNAME", actorId, e)
        sync.request()
    }

    /** Detail PO untuk pratinjau sebelum konfirmasi terima (design.md §8.5). */
    suspend fun getPo(poId: String) = dao.poById(poId)

    /** Terima PO: guard canReceive ( APPROVED + belum pernah diterima). */
    suspend fun receivePo(poId: String, actorId: String) {
        val po = dao.poById(poId) ?: throw IllegalArgumentException("PO tidak ada")
        val receivedIds = dao.listPos(po.outletId)
            .filter { it.status == "RECEIVED" }.map { it.id }.toSet()
        if (!PoCalculation.canReceive(
                runCatching { PoCalculation.PoStatus.valueOf(po.status) }
                    .getOrDefault(PoCalculation.PoStatus.DRAFT),
                receivedIds, poId
            )
        ) throw IllegalStateException("PO tidak bisa diterima (status=${po.status} / sudah diterima)")
        val items = PoJson.parse(po.itemsJson)
        items.forEach { (ingId, qty) ->
            val e = dao.ingredientById(ingId) ?: return@forEach
            applyStock(ingId, e.currentStock + qty, "IN", actorId, e, refId = poId)
        }
        dao.upsertPo(po.copy(status = "RECEIVED", pendingSync = true))
        dao.enqueue(PendingMutation(UUID.randomUUID().toString(), "PO_RECEIVE", """{"poId":"$poId"}"""))
        sync.request()
    }

    private suspend fun applyStock(
        ingId: String,
        newStock: Double,
        type: String,
        actorId: String,
        current: com.coffeeos.erp.core.data.local.IngredientEntity,
        refId: String? = null,
    ) {
        val eval = evaluateIngredient(IngredientStock(ingId, current.name, newStock, current.maxCapacity, current.unit))
        // Hysteresis: flag turun hanya jika melewati ambang balik.
        val stopped = when {
            eval.level == com.coffeeos.erp.core.domain.stock.StockLevel.CRITICAL_STOP -> true
            current.isStopped && !shouldReenable(eval.percent) -> true
            else -> false
        }
        val low = when {
            stopped -> true
            eval.level == StockLevel.LOW_WARNING -> true
            current.isLow && !shouldClearWarning(eval.percent) -> true
            else -> false
        }
        dao.upsertIngredient(
            current.copy(currentStock = newStock, isLow = low, isStopped = stopped, updatedAt = System.currentTimeMillis())
        )
        dao.enqueue(
            PendingMutation(
                UUID.randomUUID().toString(), "STOCK_$type",
                """{"ingredientId":"$ingId","qty":$newStock,"actor":"$actorId","ref":"${refId ?: ""}"}"""
            )
        )
    }
}

/** Parser minimal itemsJson PO: [{"ingredientId":"..","qty":..}, ...]. Tanpa lib JSON. */
object PoJson {
    fun parse(json: String): List<Pair<String, Double>> {
        val out = mutableListOf<Pair<String, Double>>()
        Regex(""""ingredientId"\s*:\s*"([^"]+)"\s*,\s*"qty"\s*:\s*([0-9.]+)""")
            .findAll(json).forEach { m -> out += m.groupValues[1] to m.groupValues[2].toDouble() }
        return out
    }

    fun of(items: List<PoCalculation.PoItem>): String = items.joinToString(",", "[", "]") {
        """{"ingredientId":"${it.ingredientId}","qty":${it.qty},"unitCost":${it.unitCost}}"""
    }
}
