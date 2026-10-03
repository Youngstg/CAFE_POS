package com.coffeeos.erp.feature.queueboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.local.OrderEntity
import com.coffeeos.erp.core.data.local.PosDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class QueueBoardUiState(
    val preparingOrders: List<OrderEntity> = emptyList(),
    val readyOrders: List<OrderEntity> = emptyList(),
    val latestReadyOrder: OrderEntity? = null,
    val cafeName: String = "CoffeeOS",
)

@HiltViewModel
class QueueBoardViewModel @Inject constructor(
    private val dao: PosDao,
) : ViewModel() {

    private val outletIdFlow = MutableStateFlow("")

    private val _uiState = MutableStateFlow(QueueBoardUiState())
    val uiState: StateFlow<QueueBoardUiState> = _uiState

    private val _readyCallEvent = MutableSharedFlow<OrderEntity>(extraBufferCapacity = 1)
    val readyCallEvent: SharedFlow<OrderEntity> = _readyCallEvent

    private val previousReadyIds = mutableSetOf<String>()
    private var isFirstLoad = true

    fun setOutletId(id: String, cafeName: String = "CoffeeOS") {
        outletIdFlow.value = id
        _uiState.value = _uiState.value.copy(cafeName = cafeName)

        dao.observeKitchenQueue(id).onEach { list ->
            val preparing = list.filter { it.status == "QUEUED" || it.status == "COOKING" }
            val ready = list.filter { it.status == "READY" }

            val currentReadyIds = ready.map { it.id }.toSet()
            if (!isFirstLoad) {
                val newlyReady = ready.filter { it.id in (currentReadyIds - previousReadyIds) }
                if (newlyReady.isNotEmpty()) {
                    val latest = newlyReady.last()
                    _uiState.value = _uiState.value.copy(latestReadyOrder = latest)
                    _readyCallEvent.tryEmit(latest)
                }
            } else {
                isFirstLoad = false
            }

            previousReadyIds.clear()
            previousReadyIds.addAll(currentReadyIds)

            _uiState.value = _uiState.value.copy(
                preparingOrders = preparing,
                readyOrders = ready
            )
        }.launchIn(viewModelScope)
    }
}
