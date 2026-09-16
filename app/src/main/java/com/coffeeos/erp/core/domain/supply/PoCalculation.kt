package com.coffeeos.erp.core.domain.supply

/**
 * Logika PO + terima barang murni (cermin tools/verify_mvp.py).
 * - PO berisi item (ingredientId, qty, unitCost).
 * - Status: DRAFT -> APPROVED -> RECEIVED. Terima 2x DITOLAK (idempotency via poId).
 * - Terima barang menambah currentStock (dihitung di repository, rumus di sini).
 */
object PoCalculation {

    enum class PoStatus { DRAFT, APPROVED, RECEIVED }

    data class PoItem(val ingredientId: String, val qty: Double, val unitCost: Long)

    fun total(items: List<PoItem>): Long =
        items.sumOf { (it.qty * it.unitCost).toLong() }

    /** Stok baru setelah terima PO. */
    fun receivedStock(currentStock: Double, receivedQty: Double): Double =
        currentStock + receivedQty

    /** Boleh terima? Hanya APPROVED yang belum RECEIVED. */
    fun canReceive(status: PoStatus, alreadyReceivedIds: Set<String>, poId: String): Boolean =
        status == PoStatus.APPROVED && poId !in alreadyReceivedIds
}
