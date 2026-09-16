package com.coffeeos.erp.core.domain.sync

/**
 * Kebijakan konflik multi-device (cermin tools/verify_mvp.py).
 * - FIFO: createdAt terkecil menang. Yang kalah -> CONFLICT_NEED_REVIEW (opsi B semi-manual).
 * - Idempotency: mutationId/orderId/poId yang sudah diproses tidak diproses ulang.
 * - Kritis (stok/PO/shift) wajib transaction; non-kritis (nama menu) last-write-wins.
 */
object ConflictPolicy {

    enum class Resolution { ACCEPT, CONFLICT_REVIEW, DUPLICATE_IGNORED }

    fun resolveOrder(
        orderCreatedAt: Long,
        currentServerStock: Double,
        neededQty: Double,
        alreadyProcessedIds: Set<String>,
        orderId: String,
    ): Resolution {
        if (orderId in alreadyProcessedIds) return Resolution.DUPLICATE_IGNORED
        return if (currentServerStock >= neededQty) Resolution.ACCEPT
        else Resolution.CONFLICT_REVIEW
    }

    /** Urutkan pemenang FIFO berdasarkan createdAt. */
    fun orderWinners(orderIdsByTime: List<Pair<String, Long>>): List<String> =
        orderIdsByTime.sortedBy { it.second }.map { it.first }
}
