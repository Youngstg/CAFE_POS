package com.coffeeos.erp.feature.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.local.OrderEntity
import com.coffeeos.erp.core.data.repo.OwnerRepository
import com.coffeeos.erp.core.sync.SyncTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OwnerViewModel @Inject constructor(
    private val repo: OwnerRepository,
    private val sync: SyncTrigger,
) : ViewModel() {

    private val _dash = MutableStateFlow<OwnerRepository.Dashboard?>(null)
    val dashboard: StateFlow<OwnerRepository.Dashboard?> = _dash

    private val _msg = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _msg

    fun conflicts(outletId: String): StateFlow<List<OrderEntity>> =
        repo.observeConflicts(outletId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(outletId: String) {
        viewModelScope.launch {
            try { _dash.value = repo.dashboard(outletId) }
            catch (e: Exception) { _msg.value = e.message }
        }
    }

    /** Picu worker + refresh angka pending di dashboard. */
    fun syncNow(outletId: String) {
        sync.request()
        load(outletId)
    }

    fun resolve(orderId: String, refund: Boolean) {
        viewModelScope.launch {
            try {
                repo.resolveConflict(orderId, refund)
                _msg.value = if (refund) "Order $orderId di-refund" else "Order $orderId dipaksa lunas"
            } catch (e: Exception) { _msg.value = e.message }
        }
    }
}
