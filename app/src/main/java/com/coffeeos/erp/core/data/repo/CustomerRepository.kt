package com.coffeeos.erp.core.data.repo

import com.coffeeos.erp.core.data.local.CustomerEntity
import com.coffeeos.erp.core.data.local.PosDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(
    private val dao: PosDao
) {

    suspend fun getCustomer(phone: String): CustomerEntity? {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        if (cleanPhone.isBlank()) return null
        return dao.customerByPhone(cleanPhone)
    }

    /**
     * Catat poin stempel pelanggan saat beli kopi/minuman.
     * Aturan: Setiap 1 cup = 1 stempel. Beli 10 cup = reward gratis 1 kopi.
     */
    suspend fun recordOrderStamps(
        phone: String,
        name: String,
        cupsCount: Int,
        orderTotal: Long
    ): StampResult? {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        if (cleanPhone.isBlank() || cupsCount <= 0) return null

        val existing = dao.customerByPhone(cleanPhone)
        val prevStamps = existing?.stamps ?: 0
        val newStampsTotal = prevStamps + cupsCount
        val effectiveStamps = newStampsTotal % 10
        val rewardsEarned = newStampsTotal / 10

        val updated = CustomerEntity(
            phone = cleanPhone,
            name = name.ifBlank { existing?.name ?: "" },
            stamps = effectiveStamps,
            totalOrders = (existing?.totalOrders ?: 0) + 1,
            totalSpent = (existing?.totalSpent ?: 0L) + orderTotal,
            lastVisit = System.currentTimeMillis()
        )
        dao.upsertCustomer(updated)

        return StampResult(
            customer = updated,
            addedStamps = cupsCount,
            currentStamps = effectiveStamps,
            freeRewardEarned = rewardsEarned > (prevStamps / 10)
        )
    }

    suspend fun recordOrderAndAddStamps(
        phone: String,
        name: String,
        cups: Int,
        amountSpent: Long
    ): StampResult? = recordOrderStamps(phone, name, cups, amountSpent)

    data class StampResult(
        val customer: CustomerEntity,
        val addedStamps: Int,
        val currentStamps: Int,
        val freeRewardEarned: Boolean
    ) {
        val progressBar: String get() = formatVisualBar()
        val earnedFreeDrink: Boolean get() = freeRewardEarned

        fun formatVisualBar(): String {
            val filled = "■".repeat(currentStamps.coerceIn(0, 10))
            val empty = "□".repeat((10 - currentStamps).coerceIn(0, 10))
            return "[$filled$empty] $currentStamps/10"
        }
    }
}
