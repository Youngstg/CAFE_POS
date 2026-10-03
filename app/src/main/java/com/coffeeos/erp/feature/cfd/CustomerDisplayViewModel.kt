package com.coffeeos.erp.feature.cfd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.repo.CustomerDisplayBridge
import com.coffeeos.erp.core.data.repo.CustomerDisplayState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerDisplayViewModel @Inject constructor(
    private val bridge: CustomerDisplayBridge,
) : ViewModel() {

    val state: StateFlow<CustomerDisplayState> = bridge.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomerDisplayState())

    fun dismissSuccess() {
        viewModelScope.launch {
            bridge.reset()
        }
    }
}
