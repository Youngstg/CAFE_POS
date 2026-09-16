package com.coffeeos.erp.core.domain.order

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
    /** ID pembayaran / QR dummy untuk MVP. */
    val paymentRef: String,
) {
    val subtotal: Long get() = items.sumOf { it.qty * it.unitPrice }
    val total: Long get() = (subtotal - discount).coerceAtLeast(0)
}

/** Formatter teks struk — dipakai FakePdfPrinter DAN printer ESC/POS asli nanti. */
object ReceiptFormatter {
    fun format(r: Receipt): String = buildString {
        appendLine("=".repeat(32))
        appendLine(center(r.tenantName))
        appendLine(center(r.outletName))
        appendLine("-".repeat(32))
        appendLine("Order : ${r.orderId}")
        appendLine("Kasir : ${r.cashierName}")
        appendLine("-".repeat(32))
        r.items.forEach { item ->
            val label = if (item.variant != null) "${item.name} (${item.variant})" else item.name
            appendLine("$label x${item.qty}")
            appendLine("  Rp${item.qty * item.unitPrice}")
        }
        appendLine("-".repeat(32))
        appendLine("Subtotal : Rp${r.subtotal}")
        appendLine("Diskon   : Rp${r.discount}")
        appendLine("TOTAL    : Rp${r.total}")
        appendLine("Bayar    : ${r.paymentRef}")
        appendLine("=".repeat(32))
        appendLine(center("Terima kasih!"))
    }

    private fun center(text: String, width: Int = 32): String {
        if (text.length >= width) return text
        val pad = (width - text.length) / 2
        return " ".repeat(pad) + text
    }
}
