package com.coffeeos.erp.core.data.repo

import com.coffeeos.erp.core.data.local.PendingMutation
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.sync.SyncTrigger
import java.util.UUID
import javax.inject.Inject

/** Owner: ringkasan + approve PO + selesaikan konflik (opsi B semi-manual). */
class OwnerRepository @Inject constructor(
    private val dao: PosDao,
    private val sync: SyncTrigger,
) {

    data class Dashboard(
        val revenuePaid: Long,
        val ordersPaid: Int,
        val lowCount: Int,
        val stoppedCount: Int,
        val conflictCount: Int,
        val pendingSync: Int,
        val activeShiftId: String?,
    )

    suspend fun dashboard(outletId: String): Dashboard {
        val orders = dao.listOrders(outletId)
        val paid = orders.filter { it.status == "PAID" }
        val ingredients = dao.listIngredients(outletId)
        return Dashboard(
            revenuePaid = paid.sumOf { it.total },
            ordersPaid = paid.size,
            lowCount = ingredients.count { it.isLow && !it.isStopped },
            stoppedCount = ingredients.count { it.isStopped },
            conflictCount = orders.count { it.status == "CONFLICT_NEED_REVIEW" },
            pendingSync = dao.pendingCount(),
            activeShiftId = dao.activeShift(outletId)?.id,
        )
    }

    fun observeConflicts(outletId: String) =
        dao.observeOrdersByStatus(outletId, "CONFLICT_NEED_REVIEW")

    /**
     * Selesaikan konflik: refund (order dibatalkan, stok TIDAK dikembalikan otomatis —
     * barang sudah terlanjur dipakai; selisih dicatat) atau forceSell (paksa anggap PAID).
     */
    suspend fun resolveConflict(orderId: String, refund: Boolean) {
        val order = dao.orderById(orderId) ?: throw IllegalArgumentException("Order tidak ada")
        require(order.status == "CONFLICT_NEED_REVIEW") { "Bukan order konflik" }
        dao.updateOrderStatus(orderId, if (refund) "REFUNDED" else "PAID")
        dao.enqueue(
            PendingMutation(
                UUID.randomUUID().toString(), "CONFLICT_RESOLVE",
                """{"orderId":"$orderId","refund":$refund}"""
            )
        )
        sync.request()
    }
}
