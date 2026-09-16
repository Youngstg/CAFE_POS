package com.coffeeos.erp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.local.IngredientEntity
import com.coffeeos.erp.core.data.repo.InventoryRepository
import com.coffeeos.erp.core.data.repo.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repo: InventoryRepository,
    private val catalog: MenuRepository,
) : ViewModel() {

    data class UiState(val busy: Boolean = false, val message: String? = null)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun ingredients(outletId: String): StateFlow<List<IngredientEntity>> =
        repo.observeIngredients(outletId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun opname(ingredientId: String, physical: Double, actorId: String) {
        viewModelScope.launch {
            _ui.value = UiState(busy = true)
            try {
                repo.opname(ingredientId, physical, actorId)
                _ui.value = UiState(message = "Opname tersimpan")
            } catch (e: Exception) { _ui.value = UiState(message = e.message) }
        }
    }

    fun receivePo(poId: String, actorId: String) {
        viewModelScope.launch {
            _ui.value = UiState(busy = true)
            try {
                repo.receivePo(poId, actorId)
                _ui.value = UiState(message = "PO $poId diterima, stok bertambah")
            } catch (e: Exception) { _ui.value = UiState(message = e.message) }
        }
    }

    /** Ubah kapasitas acuan 10%/2% (+satuan) tanpa mengganggu stok berjalan. */
    fun updateCapacity(ingredientId: String, maxCapacity: Double, unit: String) {
        viewModelScope.launch {
            try {
                catalog.updateCapacity(ingredientId, maxCapacity, unit)
                _ui.value = UiState(message = "Kapasitas diperbarui")
            } catch (e: Exception) { _ui.value = UiState(message = e.message) }
        }
    }

    /** Tambah bahan baru (nama + stok awal + kapasitas + satuan). */
    fun addIngredient(outletId: String, name: String, stock: Double, max: Double?, unit: String) {
        viewModelScope.launch {
            _ui.value = UiState(busy = true)
            try {
                catalog.saveIngredient(outletId, null, name, stock, max, unit)
                _ui.value = UiState(message = "Bahan ditambah")
            } catch (e: Exception) { _ui.value = UiState(message = e.message) }
        }
    }
}
