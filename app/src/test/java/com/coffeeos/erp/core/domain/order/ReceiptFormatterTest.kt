package com.coffeeos.erp.core.domain.order

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptFormatterTest {

    @Test
    fun `format struk dine in dengan nama pelanggan dan pajak PB1`() {
        val receipt = Receipt(
            tenantName = "CoffeeOS",
            outletName = "Outlet Sudirman",
            orderId = "ORD-2026-001",
            cashierName = "Siti",
            items = listOf(
                ReceiptItem("Kopi Susu Gula Aren", "Less Ice, 50% Sugar", 2, 22_000L),
                ReceiptItem("Croissant Butter", null, 1, 18_000L)
            ),
            discount = 5_000L,
            paymentRef = "QRIS",
            tax = 5_700L,
            customerName = "Budi (Meja 04)",
            orderType = "DINE_IN"
        )

        val text = ReceiptFormatter.format(receipt)

        // Verifikasi elemen wajib struk
        assertTrue(text.contains("COFFEEOS"))
        assertTrue(text.contains("Outlet Sudirman"))
        assertTrue(text.contains("Order : ORD-2026-001"))
        assertTrue(text.contains("Tipe  : Minum di Tempat (Dine In)"))
        assertTrue(text.contains("Nama  : Budi (Meja 04)"))
        assertTrue(text.contains("Kasir : Siti"))
        assertTrue(text.contains("Kopi Susu Gula Aren (Less Ice, 50% Sugar) x2"))
        assertTrue(text.contains("Croissant Butter x1"))
        assertTrue(text.contains("Subtotal : Rp62.000"))
        assertTrue(text.contains("Diskon   : -Rp5.000"))
        assertTrue(text.contains("PB1 (10%): Rp5.700"))
        assertTrue(text.contains("TOTAL    : Rp62.700"))
        assertTrue(text.contains("Bayar    : QRIS"))
        assertTrue(text.contains("Terima kasih!"))
        assertTrue(text.contains("Selamat Menikmati ☕"))
    }

    @Test
    fun `format struk take away tanpa pajak dan tanpa diskon`() {
        val receipt = Receipt(
            tenantName = "CoffeeOS",
            outletName = "Outlet Utama",
            orderId = "ORD-2026-002",
            cashierName = "Rian",
            items = listOf(
                ReceiptItem("Americano Ice", null, 1, 18_000L)
            ),
            paymentRef = "TUNAI",
            orderType = "TAKE_AWAY"
        )

        val text = ReceiptFormatter.format(receipt)

        assertTrue(text.contains("Tipe  : Bawa Pulang (Take Away)"))
        assertTrue(text.contains("Americano Ice x1"))
        assertTrue(text.contains("TOTAL    : Rp18.000"))
        assertTrue(text.contains("Bayar    : TUNAI"))
    }
}
