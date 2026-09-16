package com.coffeeos.erp.core.data.repo

import com.coffeeos.erp.core.data.local.OrderEntity
import com.coffeeos.erp.core.data.local.PendingMutation
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.domain.order.OrderTotals
import com.coffeeos.erp.core.sync.SyncTriggerimport com.coffeeos.erp.core.domain.order.ReceiptItem
import com.coffeeos.erp.core.domain.stock.IngredientStock
import com.coffeeos.erp.core.domain.stock.RecipeRequirement
import com.coffeeos.erp.core.domain.stock.StockLevel
import com.coffeeos.erp.core.domain.stock.evaluateIngredient
import com.coffeeos.erp.core.domain.stock.isMenuSellable
import java.util.UUID
import javax.inject.Inject

data class CartLine(val menuId: String, val name: String, val variant: String?, val qty: Int, val unitPrice: Long)

/**
 * Kasir offline-first: validasi -> deduct Room -> enqueue sync.
 * Firestore transaction final (anti-oversell) jalan di SyncWorker saat online.
 */
class OrderRepository @Inject constructor(
    private val dao: PosDao,
    private val sync: SyncTrigger,
) {

    fun observeMenus(outletId: String) = dao.observeMenus(outletId)
    fun observeOrders(outletId: String) = dao.observeOrders(outletId)
    fun observeIngredients(outletId: String) = dao.observeIngredients(outletId)

    suspend fun pendingCount(): Int = dao.pendingCount()

    /**
     * Checkout 1 order. Mengembalikan orderId. Throw IllegalStateException jika stok
     * tidak cukup (menu sudah STOP atau < 1 porsi). All-or-nothing: tidak ada deduct
     * parsial jika satu line gagal validasi.
     */
    suspend fun checkout(
        outletId: String,
        lines: List<CartLine>,
        promo: OrderTotals.Promo? = null,
    ): String {
        require(lines.isNotEmpty()) { "Keranjang kosong" }
        val menuIds = lines.map { it.menuId }.distinct()
        val recipes = dao.recipesForMenus(menuIds).groupBy { it.menuId }
        val ingredients = dao.listIngredients(outletId)
        val stocksById = ingredients.associate { e ->
            e.id to IngredientStock(e.id, e.name, e.currentStock, e.maxCapacity, e.unit)
        }

        lines.forEach { line ->
            val reqs = (recipes[line.menuId] ?: emptyList())
                .map { RecipeRequirement(it.ingredientId, it.qtyPerPortion * line.qty) }
            if (reqs.isNotEmpty() && !isSellableForQty(reqs, stocksById)) {
                throw IllegalStateException("Stok tidak cukup untuk ${line.name}")
            }
        }

        val totals = OrderTotals.compute(
            lines.map { ReceiptItem(it.name, it.variant, it.qty, it.unitPrice) }, promo
        )
        val orderId = "O-" + UUID.randomUUID().toString().take(8).uppercase()

        val needByIngredient = mutableMapOf<String, Double>()
        lines.forEach { line ->
            (recipes[line.menuId] ?: emptyList()).forEach { r ->
                needByIngredient[r.ingredientId] =
                    (needByIngredient[r.ingredientId] ?: 0.0) + r.qtyPerPortion * line.qty
            }
        }
        needByIngredient.forEach { (ingId, need) ->
            val e = ingredients.first { it.id == ingId }
            val newStock = (e.currentStock - need).coerceAtLeast(0.0)
            val eval = evaluateIngredient(
                IngredientStock(e.id, e.name, newStock, e.maxCapacity, e.unit)
            )
            dao.upsertIngredient(
                e.copy(
                    currentStock = newStock,
                    isLow = eval.level != StockLevel.HEALTHY,
                    isStopped = eval.level == StockLevel.CRITICAL_STOP,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        refreshMenus(outletId)
        dao.upsertOrder(OrderEntity(orderId, outletId, "QUEUED", totals.total, pendingSync = true))
        // Payload membawa needs (BOM x qty) agar SyncWorker bisa verifikasi stok
        // di Firestore transaction tanpa baca Room lagi.
        val needsJson = needByIngredient.entries.joinToString(",") { (id, qty) ->
            """{"ingredientId":"$id","qty":$qty}"""
        }
        dao.enqueue(
            PendingMutation(
                UUID.randomUUID().toString(), "ORDER_UPSERT",
                """{"orderId":"$orderId","total":${totals.total},"needs":[$needsJson]}"""
            )
        )
        sync.request()
        return orderId
    }

    suspend fun markPaid(orderId: String) {
        dao.updateOrderStatus(orderId, "PAID")
        dao.enqueue(
            PendingMutation(UUID.randomUUID().toString(), "ORDER_PAID", """{"orderId":"$orderId"}""")
        )
        sync.request()
    }

    suspend fun updateKitchenStatus(orderId: String, status: String) {
        require(status in setOf("COOKING", "READY")) { "Status dapur tidak valid" }
        dao.updateOrderStatus(orderId, status)
        dao.enqueue(
            PendingMutation(
                UUID.randomUUID().toString(), "ORDER_STATUS",
                """{"orderId":"$orderId","status":"$status"}"""
            )
        )
        sync.request()
    }

    private suspend fun refreshMenus(outletId: String) {
        val ingredients = dao.listIngredients(outletId)
        val stocksById = ingredients.associate { e ->
            e.id to IngredientStock(e.id, e.name, e.currentStock, e.maxCapacity, e.unit)
        }
        val menus = dao.listMenus(outletId)
        val allRecipes = dao.recipesForMenus(menus.map { it.id }).groupBy { it.menuId }
        menus.forEach { menu ->
            val reqs = (allRecipes[menu.id] ?: emptyList())
                .map { RecipeRequirement(it.ingredientId, it.qtyPerPortion) }
            val sellable = if (reqs.isEmpty()) true else isMenuSellable(reqs, stocksById)
            if (menu.isAvailable != sellable) dao.upsertMenu(menu.copy(isAvailable = sellable))
        }
    }

    private fun isSellableForQty(
        reqs: List<RecipeRequirement>,
        stocks: Map<String, IngredientStock>,
    ): Boolean {
        if (reqs.isEmpty()) return true
        return reqs.all { req ->
            val s = stocks[req.ingredientId] ?: return@all false
            evaluateIngredient(s).usable && s.currentStock >= req.qtyPerPortion
        }
    }
}
