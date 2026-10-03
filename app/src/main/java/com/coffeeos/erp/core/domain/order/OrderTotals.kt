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

    data class Totals(
        val subtotal: Long,
        val discount: Long,
        val total: Long,
        val tax: Long = 0L,
    )

    fun compute(items: List<ReceiptItem>, promo: Promo? = null, applyTax: Boolean = false): Totals {
        val subtotal = items.sumOf { it.qty.toLong() * it.unitPrice }
        var discount = 0L
        if (promo != null && subtotal >= promo.minOrder) {
            discount += subtotal * promo.percentOff / 100
            discount += promo.fixedDiscount
            if (discount > subtotal) discount = subtotal
        }
        val afterDiscount = (subtotal - discount).coerceAtLeast(0L)
        val tax = if (applyTax) (afterDiscount * 10 / 100) else 0L
        return Totals(subtotal, discount, afterDiscount + tax, tax)
    }
}
