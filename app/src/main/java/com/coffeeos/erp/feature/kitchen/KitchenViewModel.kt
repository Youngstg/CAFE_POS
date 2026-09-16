package com.coffeeos.erp.feature.kitchen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.local.OrderEntity
import com.coffeeos.erp.core.data.local.PosDao
import com.coffeeos.erp.core.data.repo.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/** KDS: antrean QUEUED/COOKING realtime dari Room (Firestore listener menyusul MVP-2 final). */
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

    private val _msg = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _msg

    fun track(outletId: String) { outlet.value = outletId }

    fun setStatus(orderId: String, status: String) {
        viewModelScope.launch {
            try { orders.updateKitchenStatus(orderId, status) }
            catch (e: Exception) { _msg.value = e.message }
        }
    }
}
