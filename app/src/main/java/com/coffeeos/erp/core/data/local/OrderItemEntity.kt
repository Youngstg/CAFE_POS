package com.coffeeos.erp.core.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Item detail per order — diperlukan KDS agar dapur tahu apa yang dimasak.
 * Dibuat bersamaan dengan OrderEntity saat checkout.
 */
@Entity(
    tableName = "order_items",
    foreignKeys = [ForeignKey(
        entity = OrderEntity::class,
        parentColumns = ["id"],
        childColumns = ["orderId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("orderId")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val key: Long = 0,
    val orderId: String,
    val menuName: String,
    val variant: String? = null,
    val qty: Int,
    val unitPrice: Long,
    val notes: String = "",
)
