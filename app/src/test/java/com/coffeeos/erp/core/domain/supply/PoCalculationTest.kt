package com.coffeeos.erp.core.domain.supply

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoCalculationTest {
    @Test fun `total po`() {
        val items = listOf(
            PoCalculation.PoItem("susu", 10.0, 15_000),
            PoCalculation.PoItem("kopi", 5.0, 120_000)
        )
        assertEquals(750_000L, PoCalculation.total(items))
    }

    @Test fun `terima ganda ditolak`() {
        assertTrue(
            PoCalculation.canReceive(PoCalculation.PoStatus.APPROVED, emptySet(), "PO-1")
        )
        assertFalse(
            PoCalculation.canReceive(PoCalculation.PoStatus.APPROVED, setOf("PO-1"), "PO-1")
        )
        assertFalse(
            PoCalculation.canReceive(PoCalculation.PoStatus.DRAFT, emptySet(), "PO-2")
        )
    }
}
