package com.coffeeos.erp.core.domain.order

import com.coffeeos.erp.core.util.toDateTimeString
import com.coffeeos.erp.core.util.toRupiah

data class ReceiptItem(
    val name: String,
    val variant: String? = null,
    val qty: Int,
    val unitPrice: Long,
)

data class Receipt(
    val tenantName: String,
    val outletName: String,
    val orderId: String,
    val cashierName: String,
    val items: List<ReceiptItem>,
    val discount: Long = 0,
    /** ID pembayaran / metode (TUNAI, QRIS, TRANSFER) */
    val paymentRef: String,
    val tax: Long = 0,
    val customerName: String = "",
    val orderType: String = "DINE_IN", // DINE_IN atau TAKE_AWAY
    val createdAt: Long = System.currentTimeMillis(),
) {
    val subtotal: Long get() = items.sumOf { it.qty * it.unitPrice }
    val total: Long get() = (subtotal - discount + tax).coerceAtLeast(0)
}

/** Formatter teks struk — dipakai FakePdfPrinter, preview, dan share ke WhatsApp. */
object ReceiptFormatter {
    fun format(r: Receipt): String = buildString {
        appendLine("=".repeat(32))
        appendLine(center(r.tenantName.uppercase()))
        appendLine(center(r.outletName))
        appendLine("-".repeat(32))
        appendLine("Order : ${r.orderId}")
        val typeLabel = if (r.orderType == "TAKE_AWAY") "Bawa Pulang (Take Away)" else "Minum di Tempat (Dine In)"
        appendLine("Tipe  : $typeLabel")
        if (r.customerName.isNotBlank()) {
            appendLine("Nama  : ${r.customerName}")
        }
        appendLine("Kasir : ${r.cashierName}")
        appendLine("Waktu : ${r.createdAt.toDateTimeString()}")
        appendLine("-".repeat(32))
        r.items.forEach { item ->
            val label = if (item.variant != null) "${item.name} (${item.variant})" else item.name
            appendLine("$label x${item.qty}")
            appendLine("  ${(item.qty * item.unitPrice).toRupiah()}")
        }
        appendLine("-".repeat(32))
        appendLine("Subtotal : ${r.subtotal.toRupiah()}")
        if (r.discount > 0) {
            appendLine("Diskon   : -${r.discount.toRupiah()}")
        }
        if (r.tax > 0) {
            appendLine("PB1 (10%): ${r.tax.toRupiah()}")
        }
        appendLine("TOTAL    : ${r.total.toRupiah()}")
        appendLine("Bayar    : ${r.paymentRef}")
        appendLine("=".repeat(32))
        appendLine(center("Terima kasih!"))
        appendLine(center("Selamat Menikmati ☕"))
    }

    private fun center(text: String, width: Int = 32): String {
        if (text.length >= width) return text
        val pad = (width - text.length) / 2
        return " ".repeat(pad) + text
    }
}

