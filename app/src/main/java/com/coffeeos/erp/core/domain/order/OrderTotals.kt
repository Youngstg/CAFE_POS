package com.coffeeos.erp.core.domain.order

/**
 * Total order + promo sederhana MVP (cermin tools/verify_mvp.py).
 * Promo legacy (%, buy-X-get-Y, min order) disederhanakan jadi:
 * - percentOff (0-100) dengan minOrder, atau
 * - fixedDiscount dengan minOrder.
 * PromoEngine penuh menyusul; yang penting total deterministik & ter-test.
 */
object OrderTotals {

    data class Promo(
        val percentOff: Int = 0,
        val fixedDiscount: Long = 0,
        val minOrder: Long = 0,
    )

    data class Totals(val subtotal: Long, val discount: Long, val total: Long)

    fun compute(items: List<ReceiptItem>, promo: Promo? = null): Totals {
        val subtotal = items.sumOf { it.qty.toLong() * it.unitPrice }
        var discount = 0L
        if (promo != null && subtotal >= promo.minOrder) {
            discount += subtotal * promo.percentOff / 100
            discount += promo.fixedDiscount
            if (discount > subtotal) discount = subtotal
        }
        return Totals(subtotal, discount, subtotal - discount)
    }
}
