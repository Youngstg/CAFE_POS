package com.coffeeos.erp.core.data.repo

import com.coffeeos.erp.core.data.local.OrderEntity
import com.coffeeos.erp.core.data.local.PendingMutation
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.data.local.TopSellingItem
import com.coffeeos.erp.core.sync.SyncTrigger
import com.coffeeos.erp.core.util.toDateTimeString
import com.coffeeos.erp.core.util.toDayName
import com.coffeeos.erp.core.util.toRupiah
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

/** Owner: ringkasan analitik, grafik omzet, menu terlaris, approve PO, dan penyelesaian konflik. */
class OwnerRepository @Inject constructor(
    private val dao: PosDao,
    private val sync: SyncTrigger,
) {

    data class DailySales(
        val dayLabel: String,
        val dateEpoch: Long,
        val revenue: Long,
        val orderCount: Int,
    )

    data class Dashboard(
        val revenuePaid: Long,
        val ordersPaid: Int,
        val lowCount: Int,
        val stoppedCount: Int,
        val conflictCount: Int,
        val pendingSync: Int,
        val activeShiftId: String?,
        val averageOrderValue: Long = 0L,
        val topSellingItems: List<TopSellingItem> = emptyList(),
        val dailyTrend: List<DailySales> = emptyList(),
    )

    suspend fun dashboard(outletId: String, sinceEpoch: Long = 0L): Dashboard {
        val paidOrders = dao.listPaidOrdersSince(outletId, sinceEpoch)
        val allOrders = dao.listOrders(outletId)
        val ingredients = dao.listIngredients(outletId)
        val topItems = dao.topSellingItemsSince(outletId, sinceEpoch)

        val totalRev = paidOrders.sumOf { it.total }
        val ordersCount = paidOrders.size
        val aov = if (ordersCount > 0) totalRev / ordersCount else 0L

        // Trend penjualan harian (7 hari terakhir dari data atau hari ini)
        val trend = calculateDailyTrend(dao.listPaidOrdersSince(outletId, System.currentTimeMillis() - 7L * 24 * 3600 * 1000))

        return Dashboard(
            revenuePaid = totalRev,
            ordersPaid = ordersCount,
            lowCount = ingredients.count { it.isLow && !it.isStopped },
            stoppedCount = ingredients.count { it.isStopped },
            conflictCount = allOrders.count { it.status == "CONFLICT_NEED_REVIEW" },
            pendingSync = dao.pendingCount(),
            activeShiftId = dao.activeShift(outletId)?.id,
            averageOrderValue = aov,
            topSellingItems = topItems,
            dailyTrend = trend
        )
    }

    private fun calculateDailyTrend(orders: List<OrderEntity>): List<DailySales> {
        val now = System.currentTimeMillis()
        val result = mutableListOf<DailySales>()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs = cal.timeInMillis
            val endMs = startMs + 24L * 3600 * 1000 - 1
            val dayLabel = startMs.toDayName()

            val dayOrders = orders.filter { it.createdAt in startMs..endMs }
            result.add(
                DailySales(
                    dayLabel = dayLabel,
                    dateEpoch = startMs,
                    revenue = dayOrders.sumOf { it.total },
                    orderCount = dayOrders.size
                )
            )
        }
        return result
    }

    fun observeConflicts(outletId: String) =
        dao.observeOrdersByStatus(outletId, "CONFLICT_NEED_REVIEW")

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

    /** Format teks rekap penjualan untuk dibagikan ke WhatsApp / aplikasi lain. */
    fun formatReportShare(outletName: String, periodLabel: String, dash: Dashboard): String = buildString {
        appendLine("📊 *LAPORAN PENJUALAN - COFFEEOS POS*")
        appendLine("Outlet  : $outletName")
        appendLine("Periode : $periodLabel")
        appendLine("Waktu   : ${System.currentTimeMillis().toDateTimeString()}")
        appendLine("--------------------------------")
        appendLine("💰 *Total Omzet*  : ${dash.revenuePaid.toRupiah()}")
        appendLine("🧾 *Total Transaksi*: ${dash.ordersPaid} transaksi")
        appendLine("📈 *Rata-rata/Order*: ${dash.averageOrderValue.toRupiah()}")
        if (dash.topSellingItems.isNotEmpty()) {
            appendLine("--------------------------------")
            appendLine("🏆 *Menu Terlaris:*")
            dash.topSellingItems.forEachIndexed { idx, item ->
                appendLine("  ${idx + 1}. ${item.menuName} — ${item.totalQty} porsi (${item.totalRevenue.toRupiah()})")
            }
        }
        if (dash.stoppedCount > 0 || dash.lowCount > 0) {
            appendLine("--------------------------------")
            appendLine("⚠️ *Peringatan Stok:*")
            if (dash.stoppedCount > 0) appendLine("  • ${dash.stoppedCount} bahan HABIS (Stop Menu)")
            if (dash.lowCount > 0) appendLine("  • ${dash.lowCount} bahan Menipis (Stok <=10%)")
        }
        appendLine("--------------------------------")
        appendLine("Generated by CoffeeOS POS ☕")
    }
}
