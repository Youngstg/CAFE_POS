package com.coffeeos.erp.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas pelanggan untuk program loyalitas stempel digital berbasis nomor WhatsApp.
 */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val phone: String, // Nomor WhatsApp / HP
    val name: String = "",
    val stamps: Int = 0,           // Total stempel saat ini (0-10)
    val totalOrders: Int = 0,      // Total transaksi
    val totalSpent: Long = 0L,     // Total belanja
    val lastVisit: Long = System.currentTimeMillis()
)
