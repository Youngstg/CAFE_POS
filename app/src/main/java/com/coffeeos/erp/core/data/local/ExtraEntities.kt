package com.coffeeos.erp.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Shift kasir — 1 aktif per outlet. paidTotal diakumulasi tiap order PAID. */
@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val openedBy: String,
    val openedByName: String,
    val modalAwal: Long,
    val paidTotal: Long = 0,
    val countedCash: Long? = null,
    val difference: Long? = null,
    val isClosed: Boolean = false,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val pendingSync: Boolean = true,
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
)

@Entity(tableName = "purchase_orders")
data class PurchaseOrderEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val supplierId: String,
    /** JSON daftar {ingredientId, qty, unitCost} — simpel untuk MVP offline. */
    val itemsJson: String,
    val total: Long,
    val status: String, // DRAFT / APPROVED / RECEIVED
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = true,
)

/** Promo sederhana: persen dan/atau potongan tetap dengan minimal order. */
@Entity(tableName = "promos")
data class PromoEntity(
    @PrimaryKey val id: String,
    val outletId: String,
    val name: String,
    val percentOff: Int = 0,
    val fixedDiscount: Long = 0,
    val minOrder: Long = 0,
    val active: Boolean = true,
    val pendingSync: Boolean = true,
) {
    fun toPromo() = com.coffeeos.erp.core.domain.order.OrderTotals.Promo(
        percentOff = percentOff,
        fixedDiscount = fixedDiscount,
        minOrder = minOrder
    )
}
