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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.coffeeos.erp.core.domain.order.ReceiptFormatter
import java.util.UUID

import com.coffeeos.erp.core.data.repo.CustomerDisplayBridge
import com.coffeeos.erp.core.data.repo.CustomerRepository
import com.coffeeos.erp.core.domain.menu.SelectedModifiers
import com.coffeeos.erp.core.domain.menu.DEFAULT_ADD_ONS

import com.coffeeos.erp.core.sync.FirebaseHealthService
import com.coffeeos.erp.core.sync.FirebaseHealthState

data class HeldCart(
    val id: String,
    val note: String,
    val items: List<CartLine>,
    val timestamp: Long = System.currentTimeMillis(),
    val orderType: String = "DINE_IN",
)

@HiltViewModel
class CashierViewModel @Inject constructor(
    private val orders: OrderRepository,
    private val shifts: ShiftRepository,
    private val printer: PrinterRepository,
    private val sync: SyncTrigger,
    private val catalog: MenuRepository,
    private val customerRepo: CustomerRepository,
    private val cfdBridge: CustomerDisplayBridge,
    private val firebaseHealth: FirebaseHealthService,
    private val authRepo: com.coffeeos.erp.core.data.auth.AuthRepository,
) : ViewModel() {

    init {
        viewModelScope.launch {
            authRepo.ensureCloudAuth()
        }
    }

    val firebaseHealthState: StateFlow<FirebaseHealthState> = firebaseHealth.health

    fun checkFirebaseHealth() {
        viewModelScope.launch {
            firebaseHealth.checkHealth()
        }
    }

    suspend fun verifySupervisorPin(pin: String): Boolean =
        authRepo.verifySupervisorPin(pin)

    data class UiState(
        val cart: List<CartLine> = emptyList(),
        val busy: Boolean = false,
        val message: String? = null,
        val lastReceiptPath: String? = null,
        val lastReceiptText: String? = null,
        val pendingSync: Int = 0,
        /** Tampilkan dialog konfirmasi checkout */
        val showCheckoutDialog: Boolean = false,
        /** Metode bayar yang dipilih di dialog */
        val selectedPayment: String = "TUNAI",
        /** Pilihan tipe pesanan: DINE_IN atau TAKE_AWAY */
        val orderType: String = "DINE_IN",
        /** Nama pelanggan atau nomor akrilik antrean (opsional) */
        val customerName: String = "",
        /** No. WhatsApp pelanggan untuk program stempel loyalitas & digital receipt */
        val customerPhone: String = "",
        /** Pajak Restoran PB1 10% */
        val applyTax: Boolean = false,
        /** Nomor order terakhir yang berhasil */
        val lastOrderId: String? = null,
        /** Daftar keranjang yang ditahan (Hold Bill) */
        val heldCarts: List<HeldCart> = emptyList(),
        /** Tampilkan dialog daftar pesanan ditahan */
        val showHeldDialog: Boolean = false,
        /** Menu yang sedang dikustomisasi modifier oleh kasir */
        val customizingMenu: MenuEntity? = null,
        /** Hasil stempel loyalitas transaksi terakhir */
        val lastStampResult: CustomerRepository.StampResult? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    /** Null = belum dicek; false = shift belum dibuka (kasir terkunci lembut). */
    private val _hasShift = MutableStateFlow<Boolean?>(null)
    val hasShift: StateFlow<Boolean?> = _hasShift

    /** ID shift aktif — dipakai untuk nomor urut order. */
    private var activeShiftId: String = ""

    fun checkShift(outletId: String) {
        viewModelScope.launch {
            val shift = shifts.activeShift(outletId)
            activeShiftId = shift?.id ?: ""
            _hasShift.value = shift != null
        }
    }

    private val _promo = MutableStateFlow<PromoEntity?>(null)
    val selectedPromo: StateFlow<PromoEntity?> = _promo

    /** Query pencarian menu (filter nama, murni UI-state). */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    fun setQuery(q: String) { _query.value = q }

    /** Strip KPI kasir: omzet shift + tiket terbuka + bahan STOP. */
    data class CashierKpi(val revenue: Long, val openTickets: Int, val critical: Int)
    private val _kpi = MutableStateFlow<CashierKpi?>(null)
    val kpi: StateFlow<CashierKpi?> = _kpi

    private var kpiPollingJob: Job? = null

    /** Mulai polling KPI setiap 30 detik agar data selalu fresh. */
    fun startKpiPolling(outletId: String) {
        kpiPollingJob?.cancel()
        kpiPollingJob = viewModelScope.launch {
            while (isActive) {
                loadKpiOnce(outletId)
                delay(30_000L)
            }
        }
    }

    fun loadKpi(outletId: String) {
        viewModelScope.launch { loadKpiOnce(outletId) }
    }

    private suspend fun loadKpiOnce(outletId: String) {
        try {
            val revenue = shifts.salesTotal(outletId)
            val open = shifts.openTickets(outletId)
            val critical = catalog.observeIngredients(outletId).first().count { it.isStopped }
            _kpi.value = CashierKpi(revenue, open, critical)
        } catch (_: Exception) { /* KPI gagal = strip disembunyikan, kasir tetap jalan */ }
    }

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
        checkFirebaseHealth()
    }

    private fun syncCfd(cart: List<CartLine> = _ui.value.cart) {
        val promo = _promo.value?.toPromo()
        val totals = OrderTotals.compute(
            items = cart.map { ReceiptItem(it.name, it.variant, it.qty, it.unitPrice) },
            promo = promo,
            applyTax = _ui.value.applyTax
        )
        cfdBridge.updateCart(
            items = cart,
            discount = totals.discount,
            tax = totals.tax,
            total = totals.total,
            orderType = _ui.value.orderType,
            customerName = _ui.value.customerName
        )
    }

    fun startCustomizing(menu: MenuEntity) {
        _ui.value = _ui.value.copy(customizingMenu = menu)
    }

    fun dismissCustomizing() {
        _ui.value = _ui.value.copy(customizingMenu = null)
    }

    fun addCustomizedToCart(menu: MenuEntity, modifiers: SelectedModifiers, quantity: Int = 1) {
        if (!menu.isAvailable) {
            _ui.value = _ui.value.copy(message = "${menu.name} habis (stok ≤2%)", customizingMenu = null)
            return
        }
        val cart = _ui.value.cart.toMutableList()
        val variantLabel = modifiers.toSummary()
        val extraPrice = modifiers.totalExtraPrice
        val finalUnitPrice = menu.price + extraPrice

        val idx = cart.indexOfFirst { it.menuId == menu.id && it.variant == variantLabel }
        if (idx >= 0) {
            cart[idx] = cart[idx].copy(qty = cart[idx].qty + quantity)
        } else {
            cart += CartLine(
                menuId = menu.id,
                name = menu.name,
                variant = variantLabel.ifBlank { null },
                qty = quantity,
                unitPrice = finalUnitPrice
            )
        }
        _ui.value = _ui.value.copy(cart = cart, message = null, customizingMenu = null)
        syncCfd(cart)
    }

    fun addToCart(menu: MenuEntity) {
        if (!menu.isAvailable) {
            _ui.value = _ui.value.copy(message = "${menu.name} habis (stok ≤2%)")
            return
        }
        val cart = _ui.value.cart.toMutableList()
        val idx = cart.indexOfFirst { it.menuId == menu.id && it.variant == null }
        if (idx >= 0) cart[idx] = cart[idx].copy(qty = cart[idx].qty + 1)
        else cart += CartLine(menu.id, menu.name, null, 1, menu.price)
        _ui.value = _ui.value.copy(cart = cart, message = null)
        syncCfd(cart)
    }

    /** Tambah qty dari cart panel (tombol +). */
    fun increaseInCart(menuId: String) {
        val cart = _ui.value.cart.toMutableList()
        val idx = cart.indexOfFirst { it.menuId == menuId }
        if (idx < 0) return
        cart[idx] = cart[idx].copy(qty = cart[idx].qty + 1)
        _ui.value = _ui.value.copy(cart = cart)
        syncCfd(cart)
    }

    fun clearCart() {
        _ui.value = _ui.value.copy(cart = emptyList(), message = null, selectedPayment = "TUNAI")
        cfdBridge.reset()
    }

    /** Stepper keranjang: kurang 1 (hapus baris jika qty jadi 0). */
    fun decreaseFromCart(menuId: String) {
        val cart = _ui.value.cart.toMutableList()
        val idx = cart.indexOfFirst { it.menuId == menuId }
        if (idx < 0) return
        val line = cart[idx]
        if (line.qty <= 1) cart.removeAt(idx) else cart[idx] = line.copy(qty = line.qty - 1)
        _ui.value = _ui.value.copy(cart = cart, message = null)
        syncCfd(cart)
    }

    fun setQtyInCart(menuId: String, qty: Int) {
        if (qty <= 0) { decreaseFromCart(menuId); return }
        val cart = _ui.value.cart.toMutableList()
        val idx = cart.indexOfFirst { it.menuId == menuId }
        if (idx < 0) return
        cart[idx] = cart[idx].copy(qty = qty)
        _ui.value = _ui.value.copy(cart = cart)
        syncCfd(cart)
    }

    fun selectPaymentMethod(method: String) {
        _ui.value = _ui.value.copy(selectedPayment = method)
        val cart = _ui.value.cart
        val totals = OrderTotals.compute(
            items = cart.map { ReceiptItem(it.name, it.variant, it.qty, it.unitPrice) },
            promo = _promo.value?.toPromo(),
            applyTax = _ui.value.applyTax
        )
        if (method == "QRIS") {
            cfdBridge.showPayment("QRIS", totals.total, _ui.value.lastOrderId ?: "NEW")
        }
    }

    fun setOrderType(type: String) {
        _ui.value = _ui.value.copy(orderType = type)
        syncCfd()
    }

    fun setCustomerName(name: String) {
        _ui.value = _ui.value.copy(customerName = name)
        syncCfd()
    }

    fun setCustomerPhone(phone: String) {
        _ui.value = _ui.value.copy(customerPhone = phone)
    }

    fun setApplyTax(apply: Boolean) {
        _ui.value = _ui.value.copy(applyTax = apply)
        syncCfd()
    }

    fun setShowHeldDialog(show: Boolean) {
        _ui.value = _ui.value.copy(showHeldDialog = show)
    }

    /** Tahan keranjang belanja saat ini (Hold Bill) agar bisa layani antrean lain. */
    fun holdCurrentCart(note: String = "") {
        val currentCart = _ui.value.cart
        if (currentCart.isEmpty()) return
        val label = note.ifBlank { "Antrean #${_ui.value.heldCarts.size + 1}" }
        val held = HeldCart(
            id = UUID.randomUUID().toString(),
            note = label,
            items = currentCart,
            timestamp = System.currentTimeMillis(),
            orderType = _ui.value.orderType
        )
        _ui.value = _ui.value.copy(
            cart = emptyList(),
            heldCarts = listOf(held) + _ui.value.heldCarts,
            message = "Pesanan \"$label\" disimpan sementara",
            customerName = "",
            customerPhone = "",
            orderType = "DINE_IN"
        )
        cfdBridge.reset()
    }

    /** Buka kembali keranjang yang ditahan (Resume Bill). */
    fun resumeHeldCart(id: String) {
        val found = _ui.value.heldCarts.find { it.id == id } ?: return
        val updatedHeld = _ui.value.heldCarts.filterNot { it.id == id }
        _ui.value = _ui.value.copy(
            cart = found.items,
            orderType = found.orderType,
            heldCarts = updatedHeld,
            showHeldDialog = false,
            message = "Pesanan \"${found.note}\" dikembalikan ke keranjang"
        )
        syncCfd(found.items)
    }

    /** Hapus keranjang yang ditahan. */
    fun deleteHeldCart(id: String) {
        _ui.value = _ui.value.copy(
            heldCarts = _ui.value.heldCarts.filterNot { it.id == id }
        )
    }

    /** Tampilkan dialog konfirmasi checkout. */
    fun requestCheckout() {
        if (_ui.value.cart.isEmpty()) return
        _ui.value = _ui.value.copy(showCheckoutDialog = true)
        val totals = OrderTotals.compute(
            items = _ui.value.cart.map { ReceiptItem(it.name, it.variant, it.qty, it.unitPrice) },
            promo = _promo.value?.toPromo(),
            applyTax = _ui.value.applyTax
        )
        if (_ui.value.selectedPayment == "QRIS") {
            cfdBridge.showPayment("QRIS", totals.total, _ui.value.lastOrderId ?: "NEW")
        }
    }

    /** Batal dari dialog checkout. */
    fun dismissCheckout() {
        _ui.value = _ui.value.copy(showCheckoutDialog = false)
        syncCfd()
    }

    /** Bayar → checkout deduct Room → cetak Fake PDF/Text → catat stempel → tandai PAID. */
    fun pay(outletId: String, cashierName: String, cafeName: String, outletName: String) {
        val cart = _ui.value.cart
        if (cart.isEmpty()) return
        val paymentMethod = _ui.value.selectedPayment
        val customerName = _ui.value.customerName.trim()
        val customerPhone = _ui.value.customerPhone.trim()
        val orderType = _ui.value.orderType
        val applyTax = _ui.value.applyTax
        val heldList = _ui.value.heldCarts

        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, message = null, showCheckoutDialog = false)
            try {
                val shiftId = shifts.activeShift(outletId)?.id ?: run {
                    _ui.value = _ui.value.copy(busy = false, message = "Buka shift dulu sebelum jualan")
                    return@launch
                }
                val orderId = orders.checkout(
                    outletId = outletId,
                    lines = cart,
                    promo = _promo.value?.toPromo(),
                    paymentMethod = paymentMethod,
                    shiftId = shiftId,
                    customerName = customerName,
                    orderType = orderType,
                    applyTax = applyTax
                )
                val totals = OrderTotals.compute(
                    items = cart.map { ReceiptItem(it.name, it.variant, it.qty, it.unitPrice) },
                    promo = _promo.value?.toPromo(),
                    applyTax = applyTax
                )

                // Catat stempel loyalitas jika no HP diisi
                var stampRes: CustomerRepository.StampResult? = null
                if (customerPhone.isNotBlank()) {
                    val totalCups = cart.sumOf { it.qty }
                    stampRes = customerRepo.recordOrderAndAddStamps(
                        phone = customerPhone,
                        name = customerName.ifBlank { "Pelanggan" },
                        cups = totalCups,
                        amountSpent = totals.total
                    )
                }

                val receipt = Receipt(
                    tenantName = cafeName,
                    outletName = outletName,
                    orderId = orderId,
                    cashierName = cashierName,
                    items = cart.map { ReceiptItem(it.name, it.variant, it.qty, it.unitPrice) },
                    discount = totals.discount,
                    paymentRef = paymentMethod,
                    tax = totals.tax,
                    customerName = customerName,
                    orderType = orderType
                )

                // Susun teks struk lengkap dengan info loyalty stempel & Google Maps Review
                var receiptText = ReceiptFormatter.format(receipt)
                if (stampRes != null) {
                    val stampSection = buildString {
                        appendLine("-".repeat(32))
                        appendLine("   PROGRAM STEMPEL DIGITAL")
                        appendLine("Pelanggan : ${stampRes.customer.name}")
                        appendLine("Stempel   : ${stampRes.progressBar}")
                        appendLine("Terkumpul : ${stampRes.customer.stamps}/10 Stempel")
                        if (stampRes.earnedFreeDrink) {
                            appendLine("🎉 SELAMAT! DAPAT 1 MINUMAN GRATIS!")
                        }
                        appendLine("-".repeat(32))
                        appendLine("⭐ Suka kopi kami? Beri ulasan di:")
                        appendLine("https://maps.app.goo.gl/coffeeos")
                    }
                    receiptText = receiptText.replace("=".repeat(32) + "\n   Terima kasih!", "$stampSection\n" + "=".repeat(32) + "\n   Terima kasih!")
                }

                val printed = printer.printReceipt(receipt)
                orders.markPaid(orderId)

                // Update CFD layar pelanggan
                cfdBridge.markPaymentSuccess(orderId, totals.total)

                // Refresh KPI setelah transaksi
                loadKpiOnce(outletId)

                _ui.value = UiState(
                    message = "✓ Order $orderId LUNAS — ${totals.total.toRupiahSimple()}",
                    lastOrderId = orderId,
                    lastReceiptPath = (printed as? com.coffeeos.erp.printing.PrintResult.Success)?.location,
                    lastReceiptText = receiptText,
                    pendingSync = orders.pendingCount(),
                    heldCarts = heldList,
                    lastStampResult = stampRes
                )
            } catch (e: Exception) {
                val userMsg = when {
                    e.message?.contains("Stok tidak cukup") == true -> e.message ?: "Stok tidak cukup"
                    e.message?.contains("Keranjang kosong") == true -> "Keranjang masih kosong"
                    else -> "Gagal checkout — coba lagi"
                }
                _ui.value = _ui.value.copy(busy = false, message = userMsg)
            }
        }
    }

    /** Helper format Rupiah lokal (tanpa import eksternal). */
    private fun Long.toRupiahSimple(): String {
        val s = this.toString().reversed()
        return "Rp" + s.chunked(3).joinToString(".").reversed()
    }

    override fun onCleared() {
        super.onCleared()
        kpiPollingJob?.cancel()
    }
}

