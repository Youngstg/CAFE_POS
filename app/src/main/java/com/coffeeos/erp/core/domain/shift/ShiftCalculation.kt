package com.coffeeos.erp.core.domain.shift

/**
 * Rekonsiliasi shift murni — tanpa Android. Cermin di tools/verify_mvp.py.
 *
 * Aturan dari business-flow legacy:
 * - Buka: catat modalAwal + kasir + outlet.
 * - Selama shift: akumulasi transaksi tunai (paidOrdersTotal) + hitung ekspektasi.
 * - Tutup: bandingkan kas fisik vs ekspektasi -> selisih (bisa +/-, 0 = pas).
 * - Tutup DIBLOKIR jika masih ada pendingSync (ditegakkan di ViewModel, bukan di sini).
 */
object ShiftCalculation {

    data class CloseResult(
        val expectedCash: Long,
        val countedCash: Long,
        val difference: Long, // counted - expected; negatif = kurang
        val isBalanced: Boolean,
    )

    fun expectedCash(modalAwal: Long, paidOrdersTotal: Long): Long =
        modalAwal + paidOrdersTotal

    fun close(modalAwal: Long, paidOrdersTotal: Long, countedCash: Long): CloseResult {
        val expected = expectedCash(modalAwal, paidOrdersTotal)
        val diff = countedCash - expected
        return CloseResult(expected, countedCash, diff, diff == 0L)
    }
}
