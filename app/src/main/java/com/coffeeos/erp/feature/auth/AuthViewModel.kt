package com.coffeeos.erp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.auth.AuthRepository
import com.coffeeos.erp.core.data.seed.DemoSeeder
import com.coffeeos.erp.core.data.session.SessionManager
import com.coffeeos.erp.core.sync.RealtimeSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val session: SessionManager,
    private val seeder: DemoSeeder,
    private val realtime: RealtimeSync,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val error: String? = null,
        val session: SessionManager.Session? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch {
            val s = session.session.first()
            if (s != null) {
                seeder.seedIfEmpty(s.outletId)
                realtime.start(s.tenantId, s.outletId)
                _ui.value = UiState(session = s)
            }
        }
    }

    fun login(username: String, pin: String) {
        viewModelScope.launch {
            _ui.value = UiState(loading = true)
            try {
                val result = auth.loginPin(username, pin)
                seeder.seedIfEmpty(result.session.outletId)
                realtime.start(result.session.tenantId, result.session.outletId)
                _ui.value = UiState(session = result.session)
            } catch (e: Exception) {
                _ui.value = UiState(error = e.message ?: "Login gagal")
            }
        }
    }

    fun logout() {
        viewModelScope.launch { realtime.stop(); auth.logout(); _ui.value = UiState() }
    }
}
