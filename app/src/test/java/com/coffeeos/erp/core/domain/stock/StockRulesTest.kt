package com.coffeeos.erp.core.domain.stock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirror dari tools/verify_stock_rules.py — dua-duanya harus hijau. */
class StockRulesTest {

    private fun ing(current: Double, max: Double? = 10_000.0) =
        IngredientStock("susu", "Susu", current, max, "ml")

    @Test fun `sehat di atas 10 persen`() {
        val e = evaluateIngredient(ing(2_000.0))
        assertEquals(StockLevel.HEALTHY, e.level)
        assertTrue(e.usable)
        assertFalse(e.shouldAlert)
    }

    @Test fun `warning tepat di 10 persen`() {
        val e = evaluateIngredient(ing(1_000.0))
        assertEquals(StockLevel.LOW_WARNING, e.level)
        assertTrue(e.usable)
        assertTrue(e.shouldAlert)
    }

    @Test fun `stop tepat di 2 persen`() {
        val e = evaluateIngredient(ing(200.0))
        assertEquals(StockLevel.CRITICAL_STOP, e.level)
        assertFalse(e.usable)
        assertTrue(e.shouldAlert)
    }

    @Test fun `menu mati jika satu bahan kritis`() {
        val stocks = mapOf(
            "susu" to ing(100.0), // 1% -> STOP
            "kopi" to IngredientStock("kopi", "Kopi", 4_000.0, 5_000.0, "g")
        )
        val req = listOf(
            RecipeRequirement("susu", 150.0),
            RecipeRequirement("kopi", 18.0)
        )
        assertFalse(isMenuSellable(req, stocks))
    }

    @Test fun `menu hidup saat warning tapi cukup 1 porsi`() {
        val stocks = mapOf("susu" to ing(900.0)) // 9% warning, cukup 150ml
        assertTrue(isMenuSellable(listOf(RecipeRequirement("susu", 150.0)), stocks))
    }

    @Test fun `menu mati jika tidak cukup 1 porsi walau belum 2 persen`() {
        // max kecil: 9% dari 1000 = 90ml < 150ml kebutuhan -> tidak sellable
        val stocks = mapOf(
            "susu" to IngredientStock("susu", "Susu", 90.0, 1_000.0, "ml")
        )
        assertFalse(isMenuSellable(listOf(RecipeRequirement("susu", 150.0)), stocks))
    }

    @Test fun `hysteresis nyala lagi di 5 persen`() {
        assertFalse(shouldReenable(4.9))
        assertTrue(shouldReenable(5.0))
        assertFalse(shouldClearWarning(12.0))
        assertTrue(shouldClearWarning(12.1))
    }

    @Test fun `fallback absolut tanpa maxCapacity`() {
        val stop = evaluateIngredient(IngredientStock("x", "X", 5.0, null, "pcs"))
        assertEquals(StockLevel.CRITICAL_STOP, stop.level)
        val ok = evaluateIngredient(IngredientStock("x", "X", 6.0, null, "pcs"))
        assertEquals(StockLevel.HEALTHY, ok.level)
    }
}
