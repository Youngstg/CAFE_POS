package com.coffeeos.erp.core.data.repo

import com.coffeeos.erp.core.data.local.IngredientEntity
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.data.local.PendingMutation
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.data.local.PromoEntity
import com.coffeeos.erp.core.data.local.RecipeEntity
import com.coffeeos.erp.core.sync.SyncTrigger
import java.util.UUID
import javax.inject.Inject

/**
 * Katalog: menu, bahan (+maxCapacity untuk aturan 10%/2%), resep BOM, promo.
 * Semua tulis offline-first (Room + enqueue). Tulis menu/resep/promo oleh
 * owner/admin (lihat firestore.rules); bahan juga oleh gudang.
 */
class MenuRepository @Inject constructor(
    private val dao: PosDao,
    private val sync: SyncTrigger,
) {
    fun observeMenus(outletId: String) = dao.observeMenus(outletId)
    fun observeIngredients(outletId: String) = dao.observeIngredients(outletId)
    fun observePromos(outletId: String) = dao.observePromos(outletId)
    suspend fun recipesForMenu(menuId: String) = dao.recipesForMenu(menuId)
    suspend fun activePromos(outletId: String) = dao.listActivePromos(outletId)

    suspend fun saveMenu(outletId: String, menuId: String?, name: String, price: Long, category: String = "Umum"): String {
        require(name.isNotBlank()) { "Nama menu wajib diisi" }
        require(price > 0) { "Harga harus > 0" }
        val id = menuId ?: ("M-" + UUID.randomUUID().toString().take(6).uppercase())
        val existing = dao.listMenus(outletId).firstOrNull { it.id == id }
        dao.upsertMenu(
            (existing ?: MenuEntity(id, outletId, "", 0)).copy(
                name = name.trim(), price = price,
                category = category.trim().ifBlank { "Umum" }
            )
        )
        dao.enqueue(PendingMutation(UUID.randomUUID().toString(), "MENU_UPSERT", """{"menuId":"$id"}"""))
        sync.request()
        return id
    }

    suspend fun deleteMenu(menuId: String) {
        dao.deleteRecipesForMenu(menuId)
        dao.deleteMenu(menuId)
        dao.enqueue(PendingMutation(UUID.randomUUID().toString(), "MENU_DELETE", """{"menuId":"$menuId"}"""))
        sync.request()
    }

    suspend fun saveIngredient(
        outletId: String,
        ingredientId: String?,
        name: String,
        currentStock: Double,
        maxCapacity: Double?,
        unit: String,
    ): String {
        require(name.isNotBlank()) { "Nama bahan wajib diisi" }
        require(currentStock >= 0) { "Stok tidak valid" }
        if (maxCapacity != null) require(maxCapacity > 0) { "Kapasitas harus > 0" }
        require(unit.isNotBlank()) { "Satuan wajib diisi" }
        val id = ingredientId ?: ("ING-" + UUID.randomUUID().toString().take(6).uppercase())
        val existing = dao.ingredientById(id)
        dao.upsertIngredient(
            (existing ?: IngredientEntity(id, outletId, "", 0.0, null, "pcs")).copy(
                name = name.trim(),
                currentStock = if (existing == null) currentStock else existing.currentStock,
                maxCapacity = maxCapacity,
                unit = unit.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
        dao.enqueue(
            PendingMutation(UUID.randomUUID().toString(), "INGREDIENT_UPSERT", """{"ingredientId":"$id"}""")
        )
        sync.request()
        return id
    }

    /** Ubah kapasitas (+satuan) tanpa mengganggu stok berjalan — untuk layar Gudang. */
    suspend fun updateCapacity(ingredientId: String, maxCapacity: Double, unit: String) {
        require(maxCapacity > 0) { "Kapasitas harus > 0" }
        val e = dao.ingredientById(ingredientId) ?: throw IllegalArgumentException("Bahan tidak ada")
        dao.upsertIngredient(
            e.copy(maxCapacity = maxCapacity, unit = unit.trim().ifBlank { e.unit },
                updatedAt = System.currentTimeMillis())
        )
        dao.enqueue(
            PendingMutation(UUID.randomUUID().toString(), "INGREDIENT_UPSERT", """{"ingredientId":"$ingredientId"}""")
        )
        sync.request()
    }

    suspend fun deleteIngredient(ingredientId: String) {
        if (dao.countRecipesUsing(ingredientId) > 0) {
            throw IllegalStateException("Bahan masih dipakai resep — hapus dari resep dulu")
        }
        dao.deleteIngredient(ingredientId)
        dao.enqueue(
            PendingMutation(UUID.randomUUID().toString(), "INGREDIENT_DELETE", """{"ingredientId":"$ingredientId"}""")
        )
        sync.request()
    }

    suspend fun saveRecipe(menuId: String, ingredientId: String, qtyPerPortion: Double) {
        require(qtyPerPortion > 0) { "Takaran harus > 0" }
        dao.upsertRecipe(RecipeEntity(menuId = menuId, ingredientId = ingredientId, qtyPerPortion = qtyPerPortion))
        dao.enqueue(PendingMutation(UUID.randomUUID().toString(), "RECIPE_UPSERT", """{"menuId":"$menuId"}"""))
        sync.request()
    }

    suspend fun deleteRecipe(menuId: String, ingredientId: String) {
        dao.deleteRecipe(menuId, ingredientId)
        dao.enqueue(PendingMutation(UUID.randomUUID().toString(), "RECIPE_UPSERT", """{"menuId":"$menuId"}"""))
        sync.request()
    }

    suspend fun savePromo(
        outletId: String,
        promoId: String?,
        name: String,
        percentOff: Int,
        fixedDiscount: Long,
        minOrder: Long,
        active: Boolean,
    ): String {
        require(name.isNotBlank()) { "Nama promo wajib diisi" }
        require(percentOff in 0..100) { "Persen 0-100" }
        require(fixedDiscount >= 0 && minOrder >= 0) { "Nilai tidak valid" }
        val id = promoId ?: ("P-" + UUID.randomUUID().toString().take(6).uppercase())
        val existing = dao.promoById(id)
        dao.upsertPromo(
            (existing ?: PromoEntity(id, outletId, "")).copy(
                name = name.trim(), percentOff = percentOff,
                fixedDiscount = fixedDiscount, minOrder = minOrder, active = active,
                pendingSync = true
            )
        )
        dao.enqueue(PendingMutation(UUID.randomUUID().toString(), "PROMO_UPSERT", """{"promoId":"$id"}"""))
        sync.request()
        return id
    }

    suspend fun deletePromo(promoId: String) {
        dao.deletePromo(promoId)
        dao.enqueue(PendingMutation(UUID.randomUUID().toString(), "PROMO_DELETE", """{"promoId":"$promoId"}"""))
        sync.request()
    }
}
