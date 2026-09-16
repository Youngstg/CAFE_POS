package com.coffeeos.erp.feature.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.local.ShiftEntity
import com.coffeeos.erp.core.data.repo.BlockedByPendingSyncException
import com.coffeeos.erp.core.data.repo.ShiftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShiftViewModel @Inject constructor(private val repo: ShiftRepository) : ViewModel() {

    data class UiState(val busy: Boolean = false, val message: String? = null)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun shifts(outletId: String): StateFlow<List<ShiftEntity>> =
        repo.observeShifts(outletId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun open(outletId: String, uid: String, name: String, modal: Long) {
        viewModelScope.launch {
            _ui.value = UiState(busy = true)
            try {
                val id = repo.openShift(outletId, uid, name, modal)
                _ui.value = UiState(message = "Shift $id dibuka (modal Rp$modal)")
            } catch (e: Exception) {
                _ui.value = UiState(message = e.message)
            }
        }
    }

    fun close(outletId: String, counted: Long) {
        viewModelScope.launch {
            _ui.value = UiState(busy = true)
            try {
                val r = repo.closeShift(outletId, counted)
                _ui.value = UiState(
                    message = if (r.isBalanced) "Shift PAS (Rp${r.expectedCash})"
                    else "Selisih Rp${r.difference} (ekspektasi Rp${r.expectedCash})"
                )
            } catch (e: BlockedByPendingSyncException) {
                _ui.value = UiState(message = e.message)
            } catch (e: Exception) {
                _ui.value = UiState(message = e.message)
            }
        }
    }
}
