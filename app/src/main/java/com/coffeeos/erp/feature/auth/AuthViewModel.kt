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
        val hasQuickPin: Boolean = false,
        val pinSaved: Boolean = false,
        /** True hanya setelah login email tanpa PIN → AuthScreen tawarkan buat PIN dulu. */
        val promptPinSetup: Boolean = false,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch {
            val s = session.session.first()
            if (s != null) {
                seeder.seedIfEmpty(s.outletId)
                realtime.start(s.tenantId, s.outletId)
                _ui.value = UiState(session = s, hasQuickPin = auth.hasQuickPin())
            }
        }
    }

    private fun attached(s: SessionManager.Session) {
        viewModelScope.launch {
            seeder.seedIfEmpty(s.outletId)
            realtime.start(s.tenantId, s.outletId)
            _ui.value = UiState(session = s, hasQuickPin = auth.hasQuickPin())
        }
    }

    /** Demo offline (evaluator tanpa Firebase). */
    fun loginDemo(username: String, pin: String) {
        viewModelScope.launch {
            _ui.value = UiState(loading = true)
            try {
                attached(auth.loginDemo(username, pin).session)
            } catch (e: Exception) {
                _ui.value = UiState(error = e.message ?: "Login gagal")
            }
        }
    }

    /** Firebase email+password (butuh online sekali; claims role/tenant). */
    fun loginEmail(email: String, password: String) {
        viewModelScope.launch {
            _ui.value = UiState(loading = true)
            try {
                val s = auth.loginEmail(email, password).session
                val hasPin = auth.hasQuickPin()
                seeder.seedIfEmpty(s.outletId)
                realtime.start(s.tenantId, s.outletId)
                _ui.value = UiState(session = s, hasQuickPin = hasPin, promptPinSetup = !hasPin)
            } catch (e: Exception) {
                _ui.value = UiState(error = e.message ?: "Login gagal")
            }
        }
    }

    /** PIN cepat offline (butuh sesi + PIN terdaftar). */
    fun loginQuickPin(pin: String) {
        viewModelScope.launch {
            _ui.value = UiState(loading = true)
            try {
                attached(auth.loginQuickPin(pin).session)
            } catch (e: Exception) {
                _ui.value = UiState(error = e.message ?: "Login gagal")
            }
        }
    }

    fun setupPin(pin: String) {
        viewModelScope.launch {
            try {
                auth.setupPin(pin)
                _ui.value = _ui.value.copy(pinSaved = true, promptPinSetup = false, error = null)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = e.message)
            }
        }
    }

    /** Dilewati dari layar setup PIN (tetap bisa buat nanti — TODO pengaturan). */
    fun skipPinSetup() {
        _ui.value = _ui.value.copy(promptPinSetup = false)
    }

    fun logout() {
        viewModelScope.launch { realtime.stop(); auth.logout(); _ui.value = UiState() }
    }
}
