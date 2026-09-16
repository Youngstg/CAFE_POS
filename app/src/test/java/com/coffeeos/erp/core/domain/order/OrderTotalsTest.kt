package com.coffeeos.erp.core.domain.order

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderTotalsTest {
    private val items = listOf(
        ReceiptItem("Kopi Susu", "Large", 2, 18_000),
        ReceiptItem("Croissant", null, 1, 15_000)
    )

    @Test fun `tanpa promo`() {
        val t = OrderTotals.compute(items)
        assertEquals(51_000L, t.subtotal)
        assertEquals(0L, t.discount)
        assertEquals(51_000L, t.total)
    }

    @Test fun `promo persen dengan min order`() {
        val t = OrderTotals.compute(items, OrderTotals.Promo(percentOff = 10, minOrder = 50_000))
        assertEquals(5_100L, t.discount)
        assertEquals(45_900L, t.total)
    }

    @Test fun `promo tidak berlaku di bawah min order`() {
        val t = OrderTotals.compute(items, OrderTotals.Promo(percentOff = 10, minOrder = 100_000))
        assertEquals(0L, t.discount)
    }

    @Test fun `diskon tidak boleh melebihi subtotal`() {
        val t = OrderTotals.compute(items, OrderTotals.Promo(fixedDiscount = 99_000))
        assertEquals(51_000L, t.discount)
        assertEquals(0L, t.total)
    }
}
