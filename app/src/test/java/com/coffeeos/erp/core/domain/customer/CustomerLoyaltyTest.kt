package com.coffeeos.erp.core.domain.customer

import com.coffeeos.erp.core.data.local.CustomerEntity
import com.coffeeos.erp.core.data.repo.CustomerRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerLoyaltyTest {

    private fun createResult(stamps: Int, added: Int, freeReward: Boolean): CustomerRepository.StampResult {
        return CustomerRepository.StampResult(
            customer = CustomerEntity(
                phone = "081234567890",
                name = "Budi Santoso",
                stamps = stamps,
                totalOrders = 1,
                totalSpent = 50_000L
            ),
            addedStamps = added,
            currentStamps = stamps,
            freeRewardEarned = freeReward
        )
    }

    @Test
    fun `visual bar menampilkan jumlah kotak terisi sesuai stempel`() {
        val res0 = createResult(0, 0, false)
        assertEquals("[□□□□□□□□□□] 0/10", res0.formatVisualBar())

        val res4 = createResult(4, 4, false)
        assertEquals("[■■■■□□□□□□] 4/10", res4.formatVisualBar())

        val res9 = createResult(9, 5, false)
        assertEquals("[■■■■■■■■■□] 9/10", res9.formatVisualBar())
    }

    @Test
    fun `properti progressBar alias menghasilkan string yang sama dengan formatVisualBar`() {
        val res = createResult(7, 3, false)
        assertEquals(res.formatVisualBar(), res.progressBar)
    }

    @Test
    fun `stempel mencapai 10 berhak mendapatkan reward free drink`() {
        val res = createResult(0, 2, true) // 8 + 2 = 10 -> reward!
        assertTrue(res.freeRewardEarned)
        assertTrue(res.earnedFreeDrink)
    }

    @Test
    fun `stempel di bawah 10 belum berhak reward free drink`() {
        val res = createResult(5, 2, false)
        assertFalse(res.freeRewardEarned)
        assertFalse(res.earnedFreeDrink)
    }
}
