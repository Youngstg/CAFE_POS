package com.coffeeos.erp.core.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomerDisplayBridgeTest {

    private lateinit var bridge: CustomerDisplayBridge

    @Before
    fun setup() {
        bridge = CustomerDisplayBridge()
    }

    @Test
    fun `initial state bridge memiliki keranjang kosong`() {
        val state = bridge.state.value
        assertEquals("CoffeeOS", state.cafeName)
        assertTrue(state.items.isEmpty())
        assertEquals(0L, state.total)
        assertFalse(state.isShowingPayment)
        assertFalse(state.isPaymentSuccess)
    }

    @Test
    fun `updateCart memperbarui rincian belanja live`() {
        val lines = listOf(
            CartLine("m1", "Cappuccino", "Normal Ice", 2, 25_000L),
            CartLine("m2", "Croissant", null, 1, 20_000L)
        )
        bridge.updateCart(
            items = lines,
            discount = 5_000L,
            tax = 6_500L,
            total = 71_500L,
            orderType = "DINE_IN",
            customerName = "Budi Meja 03"
        )

        val state = bridge.state.value
        assertEquals(2, state.items.size)
        assertEquals(70_000L, state.subtotal) // 2*25000 + 20000
        assertEquals(5_000L, state.discount)
        assertEquals(6_500L, state.tax)
        assertEquals(71_500L, state.total)
        assertEquals("Budi Meja 03", state.customerName)
        assertEquals("DINE_IN", state.orderType)
    }

    @Test
    fun `showPayment mengaktifkan mode bayar QRIS dengan payload dinamis`() {
        bridge.showPayment("QRIS", 75_000L, "ORD-999")

        val state = bridge.state.value
        assertTrue(state.isShowingPayment)
        assertFalse(state.isPaymentSuccess)
        assertEquals("QRIS", state.paymentMethod)
        assertEquals("ORD-999", state.lastOrderId)
        assertTrue(state.qrisPayload.contains("COFFEEOS"))
        assertTrue(state.qrisPayload.contains("75000"))
    }

    @Test
    fun `markPaymentSuccess dan reset mengembalikan state dengan benar`() {
        bridge.markPaymentSuccess("ORD-100", 50_000L)
        assertTrue(bridge.state.value.isPaymentSuccess)
        assertEquals("ORD-100", bridge.state.value.lastOrderId)

        bridge.reset()
        assertFalse(bridge.state.value.isPaymentSuccess)
        assertTrue(bridge.state.value.items.isEmpty())
    }
}
