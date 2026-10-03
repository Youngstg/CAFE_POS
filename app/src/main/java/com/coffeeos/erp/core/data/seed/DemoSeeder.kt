package com.coffeeos.erp.core.data.seed

import com.coffeeos.erp.core.data.local.IngredientEntity
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.data.local.RecipeEntity
import com.coffeeos.erp.core.data.local.SupplierEntity
import javax.inject.Inject

/**
 * Data demoidempotency: hanya jalan jika menus outlet masih kosong.
 * Susu 10.000ml, Kopi 5.000g, Cup 500pcs, Croissant 50pcs.
 */
class DemoSeeder @Inject constructor(private val dao: PosDao) {

    suspend fun seedIfEmpty(outletId: String) {
        if (dao.listMenus(outletId).isNotEmpty()) return
        val ings = listOf(
            IngredientEntity("ing-susu", outletId, "Susu Full Cream", 8_000.0, 10_000.0, "ml"),
            IngredientEntity("ing-kopi", outletId, "Biji Arabika", 4_000.0, 5_000.0, "g"),
            IngredientEntity("ing-cup", outletId, "Cup 12oz", 400.0, 500.0, "pcs"),
            IngredientEntity("ing-croissant", outletId, "Croissant Beku", 40.0, 50.0, "pcs"),
            IngredientEntity("ing-matcha", outletId, "Matcha Powder", 1_000.0, 2_000.0, "g"),
            IngredientEntity("ing-roti", outletId, "Roti Brioche", 30.0, 50.0, "pcs"),
        )
        ings.forEach { dao.upsertIngredient(it) }
        val menus = listOf(
            MenuEntity("m-kopsus", outletId, "Kopi Susu Aren", 18_000, category = "Minuman"),
            MenuEntity("m-latte", outletId, "Caffe Latte", 22_000, category = "Minuman"),
            MenuEntity("m-americano", outletId, "Iced Americano", 16_000, category = "Minuman"),
            MenuEntity("m-matcha", outletId, "Matcha Latte", 24_000, category = "Minuman"),
            MenuEntity("m-croissant", outletId, "Butter Croissant", 15_000, category = "Makanan"),
            MenuEntity("m-toast", outletId, "Kaya Butter Toast", 16_000, category = "Makanan"),
        )
        menus.forEach { dao.upsertMenu(it) }
        listOf(
            RecipeEntity(menuId = "m-kopsus", ingredientId = "ing-susu", qtyPerPortion = 150.0),
            RecipeEntity(menuId = "m-kopsus", ingredientId = "ing-kopi", qtyPerPortion = 18.0),
            RecipeEntity(menuId = "m-kopsus", ingredientId = "ing-cup", qtyPerPortion = 1.0),
            RecipeEntity(menuId = "m-latte", ingredientId = "ing-susu", qtyPerPortion = 200.0),
            RecipeEntity(menuId = "m-latte", ingredientId = "ing-kopi", qtyPerPortion = 18.0),
            RecipeEntity(menuId = "m-latte", ingredientId = "ing-cup", qtyPerPortion = 1.0),
            RecipeEntity(menuId = "m-americano", ingredientId = "ing-kopi", qtyPerPortion = 18.0),
            RecipeEntity(menuId = "m-americano", ingredientId = "ing-cup", qtyPerPortion = 1.0),
            RecipeEntity(menuId = "m-matcha", ingredientId = "ing-susu", qtyPerPortion = 180.0),
            RecipeEntity(menuId = "m-matcha", ingredientId = "ing-matcha", qtyPerPortion = 15.0),
            RecipeEntity(menuId = "m-matcha", ingredientId = "ing-cup", qtyPerPortion = 1.0),
            RecipeEntity(menuId = "m-croissant", ingredientId = "ing-croissant", qtyPerPortion = 1.0),
            RecipeEntity(menuId = "m-toast", ingredientId = "ing-roti", qtyPerPortion = 1.0),
        ).forEach { dao.upsertRecipe(it) }
        dao.upsertSupplier(SupplierEntity("SUP-DEMO", outletId, "PT Susu Segar", "0812-0000-111", "Jl. Demo 1"))
        dao.upsertPromo(
            com.coffeeos.erp.core.data.local.PromoEntity(
                "P-HEMAT10", outletId, "Hemat 10% min 50rb",
                percentOff = 10, minOrder = 50_000, active = true
            )
        )
    }
}
