package com.coffeeos.erp.core.data.repo

import com.coffeeos.erp.core.data.local.PendingMutation
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.data.local.ShiftEntity
import com.coffeeos.erp.core.domain.shift.ShiftCalculation
import com.coffeeos.erp.core.sync.SyncTrigger
import java.util.UUID
import javax.inject.Inject

class BlockedByPendingSyncException(val pending: Int) :
    IllegalStateException("Masih ada $pending mutasi belum tersinkron. Sync dulu sebelum tutup shift.")

/** Shift kasir: buka (modal) -> akumulasi -> tutup (rekonsiliasi, diblokir jika antrean sync > 0). */
class ShiftRepository @Inject constructor(
    private val dao: PosDao,
    private val sync: SyncTrigger,
) {

    fun observeShifts(outletId: String) = dao.observeShifts(outletId)
    suspend fun activeShift(outletId: String) = dao.activeShift(outletId)
    suspend fun pendingCount() = dao.pendingCount()

    /** Omzet shift berjalan (order PAID) untuk strip KPI kasir. */
    suspend fun salesTotal(outletId: String): Long = dao.paidTotal(outletId)

    /** Tiket terbuka (QUEUED/COOKING/READY) untuk strip KPI kasir. */
    suspend fun openTickets(outletId: String): Int =
        dao.listOrders(outletId).count { it.status == "QUEUED" || it.status == "COOKING" || it.status == "READY" }

    suspend fun openShift(outletId: String, openedBy: String, openedByName: String, modalAwal: Long): String {
        require(modalAwal >= 0) { "Modal tidak valid" }
        if (dao.activeShift(outletId) != null) throw IllegalStateException("Shift masih aktif")
        val id = "S-" + UUID.randomUUID().toString().take(8).uppercase()
        dao.upsertShift(ShiftEntity(id, outletId, openedBy, openedByName, modalAwal))
        dao.enqueue(PendingMutation(UUID.randomUUID().toString(), "SHIFT_OPEN", """{"shiftId":"$id"}"""))
        sync.request()
        return id
    }

    suspend fun closeShift(outletId: String, countedCash: Long): ShiftCalculation.CloseResult {
        val pending = dao.pendingCount()
        if (pending > 0) throw BlockedByPendingSyncException(pending)
        val shift = dao.activeShift(outletId) ?: throw IllegalStateException("Tidak ada shift aktif")
        val paidTotal = dao.paidTotal(outletId)
        val result = ShiftCalculation.close(shift.modalAwal, paidTotal, countedCash)
        dao.upsertShift(
            shift.copy(
                paidTotal = paidTotal,
                countedCash = countedCash,
                difference = result.difference,
                isClosed = true,
                closedAt = System.currentTimeMillis(),
                pendingSync = true
            )
        )
        dao.enqueue(
            PendingMutation(UUID.randomUUID().toString(), "SHIFT_CLOSE", """{"shiftId":"${shift.id}"}""")
        )
        sync.request()
        return result
    }
}
