package com.coffeeos.erp.feature.kitchen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.local.OrderEntity
import com.coffeeos.erp.core.data.local.OrderItemEntity
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.data.repo.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** KDS: antrean QUEUED/COOKING/READY realtime dari Room, dengan detail item per order. */
@HiltViewModel
class KitchenViewModel @Inject constructor(
    private val dao: PosDao,
    private val orders: OrderRepository,
) : ViewModel() {

    private val outlet = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val queue: Flow<List<OrderEntity>> = outlet.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList())
        else dao.observeKitchenQueue(id)
    }

    /**
     * Map orderId -> List<OrderItemEntity> untuk ditampilkan di kartu KDS.
     * Diperbarui setiap kali queue berubah.
     */
    private val _orderItemsMap = MutableStateFlow<Map<String, List<OrderItemEntity>>>(emptyMap())
    val orderItemsMap: StateFlow<Map<String, List<OrderItemEntity>>> = _orderItemsMap

    private val _msg = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _msg

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled

    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
    }

    private val _newOrderAlert = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val newOrderAlert: kotlinx.coroutines.flow.SharedFlow<Unit> = _newOrderAlert

    private val previousQueuedIds = mutableSetOf<String>()
    private var isFirstLoad = true

    fun track(outletId: String) {
        outlet.value = outletId
        // Saat queue berubah, muat item detail untuk setiap order & cek order baru untuk alert
        queue.onEach { orderList ->
            val currentQueued = orderList.filter { it.status == "QUEUED" }.map { it.id }.toSet()
            if (!isFirstLoad) {
                val newIncoming = currentQueued - previousQueuedIds
                if (newIncoming.isNotEmpty()) {
                    _newOrderAlert.tryEmit(Unit)
                }
            } else {
                isFirstLoad = false
            }
            previousQueuedIds.clear()
            previousQueuedIds.addAll(currentQueued)

            val map = mutableMapOf<String, List<OrderItemEntity>>()
            orderList.forEach { order ->
                try {
                    map[order.id] = dao.getOrderItems(order.id)
                } catch (_: Exception) { /* order lama mungkin tidak punya items */ }
            }
            _orderItemsMap.value = map
        }.launchIn(viewModelScope)
    }

    fun setStatus(orderId: String, status: String) {
        viewModelScope.launch {
            try {
                orders.updateKitchenStatus(orderId, status)
                _msg.value = null
            } catch (e: Exception) {
                _msg.value = "Gagal update status: ${e.message}"
            }
        }
    }
}
