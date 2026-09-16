package com.coffeeos.erp.core.domain.stock

/**
 * Aturan stok final v2 — pure Kotlin, tanpa dependensi Android.
 * Bisa di-unit-test di JVM dan dicerminkan di tools/verify_stock_rules.py.
 *
 * - WARNING (masih bisa jual): percent <= 10
 * - STOP (menu auto-mati): percent <= 2
 * - Hysteresis anti-kedip: nyala lagi saat percent >= 5, warning hilang saat > 12
 * - Fallback absolut untuk barang tanpa maxCapacity jelas: currentStock <= 5 dianggap STOP.
 */
object StockRules {
    const val WARNING_PERCENT = 10.0
    const val STOP_PERCENT = 2.0
    const val REENABLE_PERCENT = 5.0
    const val WARNING_CLEAR_PERCENT = 12.0
    const val ABSOLUTE_FALLBACK_STOP = 5.0
}

enum class StockLevel {
    HEALTHY,
    LOW_WARNING,
    CRITICAL_STOP,
    UNKNOWN,
}

data class IngredientStock(
    val id: String,
    val name: String,
    /** Stok saat ini dalam satuan terkecil (ml / gram / pcs). */
    val currentStock: Double,
    /** Kapasitas normal gudang. Null = tidak diketahui -> pakai fallback absolut. */
    val maxCapacity: Double?,
    val unit: String,
)

data class StockEvaluation(
    val level: StockLevel,
    /** Null jika maxCapacity tidak diketahui. */
    val percent: Double?,
    /** True = bahan ini sendirian masih boleh dipakai. */
    val usable: Boolean,
    /** True = tampilkan di daftar butuh beli / kirim FCM. */
    val shouldAlert: Boolean,
)

/** Satu kebutuhan bahan untuk 1 porsi menu (BOM). */
data class RecipeRequirement(
    val ingredientId: String,
    val qtyPerPortion: Double,
)

fun evaluateIngredient(stock: IngredientStock): StockEvaluation {
    val max = stock.maxCapacity
    if (max == null || max <= 0) {
        val stopped = stock.currentStock <= StockRules.ABSOLUTE_FALLBACK_STOP
        return StockEvaluation(
            level = if (stopped) StockLevel.CRITICAL_STOP else StockLevel.HEALTHY,
            percent = null,
            usable = !stopped,
            shouldAlert = stopped
        )
    }
    val percent = stock.currentStock / max * 100.0
    return when {
        percent <= StockRules.STOP_PERCENT ->
            StockEvaluation(StockLevel.CRITICAL_STOP, percent, usable = false, shouldAlert = true)
        percent <= StockRules.WARNING_PERCENT ->
            StockEvaluation(StockLevel.LOW_WARNING, percent, usable = true, shouldAlert = true)
        else ->
            StockEvaluation(StockLevel.HEALTHY, percent, usable = true, shouldAlert = false)
    }
}

/**
 * Menu boleh dijual HANYA jika SEMUA bahannya:
 * 1. tidak CRITICAL_STOP, dan
 * 2. stok cukup untuk minimal 1 porsi (currentStock >= qtyPerPortion).
 */
fun isMenuSellable(
    requirements: List<RecipeRequirement>,
    stocksById: Map<String, IngredientStock>,
): Boolean {
    if (requirements.isEmpty()) return false
    return requirements.all { req ->
        val stock = stocksById[req.ingredientId] ?: return@all false
        val eval = evaluateIngredient(stock)
        eval.usable && stock.currentStock >= req.qtyPerPortion
    }
}

/** Dipanggil setelah restock (PO diterima / opname): boleh nyala lagi? */
fun shouldReenable(percent: Double?): Boolean =
    percent != null && percent >= StockRules.REENABLE_PERCENT

/** Warning boleh dihapus dari daftar? */
fun shouldClearWarning(percent: Double?): Boolean =
    percent != null && percent > StockRules.WARNING_CLEAR_PERCENT
