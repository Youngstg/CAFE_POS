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
import java.util.Calendar
import com.coffeeos.erp.core.sync.FirebaseHealthService
import com.coffeeos.erp.core.sync.FirebaseHealthState
import javax.inject.Inject

enum class PeriodFilter(val label: String) {
    TODAY("Hari ini"), WEEK("7 hari"), MONTH("30 hari"), ALL("Semua")
}

@HiltViewModel
class OwnerViewModel @Inject constructor(
    private val repo: OwnerRepository,
    private val sync: SyncTrigger,
    val firebaseHealth: FirebaseHealthService,
) : ViewModel() {

    val healthState: StateFlow<FirebaseHealthState> = firebaseHealth.health

    fun checkFirebaseHealth() {
        viewModelScope.launch {
            firebaseHealth.checkHealth()
        }
    }

    private val _dash = MutableStateFlow<OwnerRepository.Dashboard?>(null)
    val dashboard: StateFlow<OwnerRepository.Dashboard?> = _dash

    private val _period = MutableStateFlow(PeriodFilter.ALL)
    val period: StateFlow<PeriodFilter> = _period

    private val _msg = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _msg

    fun conflicts(outletId: String): StateFlow<List<OrderEntity>> =
        repo.observeConflicts(outletId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setPeriod(outletId: String, p: PeriodFilter) {
        _period.value = p
        load(outletId)
    }

    fun load(outletId: String) {
        cachedOutletId = outletId
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val sinceEpoch = when (_period.value) {
                    PeriodFilter.TODAY -> {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        cal.timeInMillis
                    }
                    PeriodFilter.WEEK -> now - 7L * 24 * 3600 * 1000
                    PeriodFilter.MONTH -> now - 30L * 24 * 3600 * 1000
                    PeriodFilter.ALL -> 0L
                }
                _dash.value = repo.dashboard(outletId, sinceEpoch)
            } catch (e: Exception) {
                _msg.value = "Gagal memuat dashboard — coba refresh."
            }
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
                _msg.value = if (refund)
                    "✓ Refund untuk order $orderId berhasil dicatat"
                else
                    "✓ Order $orderId dipaksa lunas"
                if (cachedOutletId.isNotEmpty()) load(cachedOutletId)
            } catch (e: Exception) {
                _msg.value = "Gagal menyelesaikan konflik — coba lagi"
            }
        }
    }

    fun getShareReportText(outletName: String = "Outlet"): String? {
        val d = _dash.value ?: return null
        return repo.formatReportShare(outletName, _period.value.label, d)
    }

    private var cachedOutletId = ""

    /** Override load untuk menyimpan outletId terakhir. */
    fun loadWithCache(outletId: String) {
        load(outletId)
    }
}

