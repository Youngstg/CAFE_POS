package com.coffeeos.erp.feature.supply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.local.PurchaseOrderEntity
import com.coffeeos.erp.core.data.local.SupplierEntity
import com.coffeeos.erp.core.data.repo.SupplyRepository
import com.coffeeos.erp.core.domain.supply.PoCalculation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupplyViewModel @Inject constructor(private val repo: SupplyRepository) : ViewModel() {

    data class UiState(val busy: Boolean = false, val message: String? = null)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun suppliers(outletId: String): StateFlow<List<SupplierEntity>> =
        repo.observeSuppliers(outletId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun pos(outletId: String): StateFlow<List<PurchaseOrderEntity>> =
        repo.observePos(outletId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSupplier(outletId: String, name: String, phone: String, address: String) {
        viewModelScope.launch {
            try { repo.addSupplier(outletId, name, phone, address); _ui.value = UiState(message = "Supplier ditambah") }
            catch (e: Exception) { _ui.value = UiState(message = e.message) }
        }
    }

    fun createDraft(outletId: String, supplierId: String, items: List<PoCalculation.PoItem>, by: String) {
        viewModelScope.launch {
            _ui.value = UiState(busy = true)
            try {
                val id = repo.createDraftPo(outletId, supplierId, items, by)
                _ui.value = UiState(message = "Draft $id dibuat, menunggu approve Owner")
            } catch (e: Exception) { _ui.value = UiState(message = e.message) }
        }
    }

    fun approve(poId: String) {
        viewModelScope.launch {
            try { repo.approvePo(poId); _ui.value = UiState(message = "PO $poId APPROVED") }
            catch (e: Exception) { _ui.value = UiState(message = e.message) }
        }
    }
}
