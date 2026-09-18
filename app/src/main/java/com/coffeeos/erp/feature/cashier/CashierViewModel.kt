package com.coffeeos.erp.feature.cashier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.data.local.PromoEntity
import com.coffeeos.erp.core.data.repo.CartLine
import com.coffeeos.erp.core.data.repo.MenuRepository
import com.coffeeos.erp.core.data.repo.OrderRepository
import com.coffeeos.erp.core.data.repo.ShiftRepository
import com.coffeeos.erp.core.domain.order.OrderTotals
import com.coffeeos.erp.core.domain.order.Receipt
import com.coffeeos.erp.core.domain.order.ReceiptItem
import com.coffeeos.erp.core.sync.SyncTrigger
import com.coffeeos.erp.printing.PrinterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CashierViewModel @Inject constructor(
    private val orders: OrderRepository,
    private val shifts: ShiftRepository,
    private val printer: PrinterRepository,
    private val sync: SyncTrigger,
    private val catalog: MenuRepository,
) : ViewModel() {

    data class UiState(
        val cart: List<CartLine> = emptyList(),
        val busy: Boolean = false,
        val message: String? = null,
        val lastReceiptPath: String? = null,
        val pendingSync: Int = 0,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    /** Null = belum dicek; false = shift belum dibuka (kasir terkunci lembut). */
    private val _hasShift = MutableStateFlow<Boolean?>(null)
    val hasShift: StateFlow<Boolean?> = _hasShift

    fun checkShift(outletId: String) {
        viewModelScope.launch { _hasShift.value = shifts.activeShift(outletId) != null }
    }

    private val _promo = MutableStateFlow<PromoEntity?>(null)
    val selectedPromo: StateFlow<PromoEntity?> = _promo

    fun promos(outletId: String): StateFlow<List<PromoEntity>> =
        catalog.observePromos(outletId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectPromo(promo: PromoEntity?) { _promo.value = promo }

    fun menus(outletId: String): StateFlow<List<MenuEntity>> =
        orders.observeMenus(outletId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshPending() {
        viewModelScope.launch { _ui.value = _ui.value.copy(pendingSync = orders.pendingCount()) }
    }

    /** Tombol "Sync sekarang" — picu worker di luar jadwal otomatis. */
    fun syncNow() {
        sync.request()
        refreshPending()
    }

    fun addToCart(menu: MenuEntity) {
        if (!menu.isAvailable) {
            _ui.value = _ui.value.copy(message = "${menu.name} habis (stok ≤2%)")
            return
        }
        val cart = _ui.value.cart.toMutableList()
        val idx = cart.indexOfFirst { it.menuId == menu.id }
        if (idx >= 0) cart[idx] = cart[idx].copy(qty = cart[idx].qty + 1)
        else cart += CartLine(menu.id, menu.name, null, 1, menu.price)
        _ui.value = _ui.value.copy(cart = cart, message = null)
    }

    fun clearCart() { _ui.value = _ui.value.copy(cart = emptyList(), message = null) }

    /** Stepper keranjang: kurang 1 (hapus baris jika qty jadi 0). */
    fun decreaseFromCart(menuId: String) {
        val cart = _ui.value.cart.toMutableList()
        val idx = cart.indexOfFirst { it.menuId == menuId }
        if (idx < 0) return
        val line = cart[idx]
        if (line.qty <= 1) cart.removeAt(idx) else cart[idx] = line.copy(qty = line.qty - 1)
        _ui.value = _ui.value.copy(cart = cart, message = null)
    }

    /** Bayar -> checkout deduct Room -> cetak Fake PDF -> tandai PAID. */
    fun pay(outletId: String, cashierName: String, paymentRef: String) {
        val cart = _ui.value.cart
        if (cart.isEmpty()) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, message = null)
            try {
                if (shifts.activeShift(outletId) == null) {
                    _ui.value = _ui.value.copy(busy = false, message = "Buka shift dulu sebelum jualan")
                    return@launch
                }
                val orderId = orders.checkout(outletId, cart, _promo.value?.toPromo())
                val totals = OrderTotals.compute(
                    cart.map { ReceiptItem(it.name, it.variant, it.qty, it.unitPrice) },
                    _promo.value?.toPromo()
                )
                val receipt = Receipt(
                    "CoffeeOS Demo", "Outlet 1", orderId, cashierName,
                    cart.map { ReceiptItem(it.name, it.variant, it.qty, it.unitPrice) },
                    totals.discount, paymentRef
                )
                val printed = printer.printReceipt(receipt)
                orders.markPaid(orderId)
                _ui.value = UiState(
                    message = "Order $orderId LUNAS Rp${totals.total}",
                    lastReceiptPath = (printed as? com.coffeeos.erp.printing.PrintResult.Success)?.location,
                    pendingSync = orders.pendingCount()
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(busy = false, message = e.message)
            }
        }
    }
}
