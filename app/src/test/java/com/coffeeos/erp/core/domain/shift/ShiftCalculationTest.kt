package com.coffeeos.erp.core.domain.shift

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftCalculationTest {
    @Test fun `kas pas`() {
        val r = ShiftCalculation.close(modalAwal = 500_000, paidOrdersTotal = 1_200_000, countedCash = 1_700_000)
        assertEquals(1_700_000L, r.expectedCash)
        assertEquals(0L, r.difference)
        assertTrue(r.isBalanced)
    }

    @Test fun `kas kurang`() {
        val r = ShiftCalculation.close(500_000, 1_200_000, 1_650_000)
        assertEquals(-50_000L, r.difference)
        assertFalse(r.isBalanced)
    }
}
