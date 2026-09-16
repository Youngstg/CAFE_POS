package com.coffeeos.erp.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.local.IngredientEntity
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.data.local.PromoEntity
import com.coffeeos.erp.core.data.local.RecipeEntity
import com.coffeeos.erp.core.data.repo.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Katalog owner/admin: menu + resep BOM + promo. Semua offline-first. */
@HiltViewModel
class MenuViewModel @Inject constructor(private val repo: MenuRepository) : ViewModel() {

    data class UiState(val busy: Boolean = false, val message: String? = null)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private val _selectedMenu = MutableStateFlow<String?>(null)
    val selectedMenu: StateFlow<String?> = _selectedMenu

    private val _recipes = MutableStateFlow<List<RecipeEntity>>(emptyList())
    val recipes: StateFlow<List<RecipeEntity>> = _recipes

    fun menus(outletId: String): StateFlow<List<MenuEntity>> =
        repo.observeMenus(outletId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun ingredients(outletId: String): StateFlow<List<IngredientEntity>> =
        repo.observeIngredients(outletId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun promos(outletId: String): StateFlow<List<PromoEntity>> =
        repo.observePromos(outletId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectMenu(menuId: String?) {
        _selectedMenu.value = menuId
        menuId?.let { reloadRecipes(it) }
    }

    private fun reloadRecipes(menuId: String) {
        viewModelScope.launch {
            try { _recipes.value = repo.recipesForMenu(menuId) }
            catch (e: Exception) { _ui.value = UiState(message = e.message) }
        }
    }

    private fun run(op: suspend () -> Unit, after: (() -> Unit)? = null) {
        viewModelScope.launch {
            _ui.value = UiState(busy = true)
            try {
                op()
                after?.invoke()
                _ui.value = UiState(message = "Tersimpan & masuk antrean sync")
            } catch (e: Exception) { _ui.value = UiState(message = e.message) }
        }
    }

    fun saveMenu(outletId: String, menuId: String?, name: String, price: Long) =
        run({ repo.saveMenu(outletId, menuId, name, price) })

    fun deleteMenu(menuId: String) = run({
        repo.deleteMenu(menuId)
        if (_selectedMenu.value == menuId) { _selectedMenu.value = null; _recipes.value = emptyList() }
    })

    fun saveRecipe(menuId: String, ingredientId: String, qty: Double) =
        run({ repo.saveRecipe(menuId, ingredientId, qty) }, { reloadRecipes(menuId) })

    fun deleteRecipe(menuId: String, ingredientId: String) =
        run({ repo.deleteRecipe(menuId, ingredientId) }, { reloadRecipes(menuId) })

    fun savePromo(
        outletId: String, promoId: String?, name: String,
        percentOff: Int, fixedDiscount: Long, minOrder: Long, active: Boolean,
    ) = run({ repo.savePromo(outletId, promoId, name, percentOff, fixedDiscount, minOrder, active) })

    fun deletePromo(promoId: String) = run({ repo.deletePromo(promoId) })
}
