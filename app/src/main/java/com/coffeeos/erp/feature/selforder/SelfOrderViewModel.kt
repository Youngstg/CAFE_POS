package com.coffeeos.erp.feature.selforder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.data.repo.CartLine
import com.coffeeos.erp.core.data.repo.CustomerRepository
import com.coffeeos.erp.core.data.repo.MenuRepository
import com.coffeeos.erp.core.data.repo.OrderRepository
import com.coffeeos.erp.core.domain.menu.AddOn
import com.coffeeos.erp.core.domain.menu.DEFAULT_ADD_ONS
import com.coffeeos.erp.core.domain.menu.IceLevel
import com.coffeeos.erp.core.domain.menu.SelectedModifiers
import com.coffeeos.erp.core.domain.menu.SugarLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelfOrderViewModel @Inject constructor(
    private val catalog: MenuRepository,
    private val orders: OrderRepository,
    private val customers: CustomerRepository,
) : ViewModel() {

    private val _tableNo = MutableStateFlow("01")
    val tableNo: StateFlow<String> = _tableNo

    fun setTableNo(no: String) {
        _tableNo.value = no.trim()
    }

    private val _cart = MutableStateFlow<List<CartLine>>(emptyList())
    val cart: StateFlow<List<CartLine>> = _cart

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName
    fun setCustomerName(name: String) { _customerName.value = name }

    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone
    fun setCustomerPhone(phone: String) { _customerPhone.value = phone }

    private val _paymentChoice = MutableStateFlow("QRIS") // QRIS atau CASHIER
    val paymentChoice: StateFlow<String> = _paymentChoice
    fun setPaymentChoice(choice: String) { _paymentChoice.value = choice }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    fun setQuery(q: String) { _query.value = q }

    // Dialog kustomisasi minuman yang sedang dipilih
    private val _customizingMenu = MutableStateFlow<MenuEntity?>(null)
    val customizingMenu: StateFlow<MenuEntity?> = _customizingMenu

    private val _selectedModifiers = MutableStateFlow(SelectedModifiers())
    val selectedModifiers: StateFlow<SelectedModifiers> = _selectedModifiers

    private val _submittedOrderId = MutableStateFlow<String?>(null)
    val submittedOrderId: StateFlow<String?> = _submittedOrderId

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _stampResult = MutableStateFlow<CustomerRepository.StampResult?>(null)
    val stampResult: StateFlow<CustomerRepository.StampResult?> = _stampResult

    fun menus(outletId: String): StateFlow<List<MenuEntity>> =
        catalog.observeMenus(outletId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun openCustomizer(menu: MenuEntity) {
        _customizingMenu.value = menu
        _selectedModifiers.value = SelectedModifiers()
    }

    fun closeCustomizer() {
        _customizingMenu.value = null
    }

    fun setIce(ice: IceLevel) {
        _selectedModifiers.value = _selectedModifiers.value.copy(ice = ice)
    }

    fun setSugar(sugar: SugarLevel) {
        _selectedModifiers.value = _selectedModifiers.value.copy(sugar = sugar)
    }

    fun toggleAddOn(addon: AddOn) {
        val current = _selectedModifiers.value.addOns.toMutableList()
        if (current.any { it.id == addon.id }) {
            current.removeAll { it.id == addon.id }
        } else {
            current.add(addon)
        }
        _selectedModifiers.value = _selectedModifiers.value.copy(addOns = current)
    }

    fun setNotes(notes: String) {
        _selectedModifiers.value = _selectedModifiers.value.copy(notes = notes)
    }

    fun addCustomizedToCart() {
        val menu = _customizingMenu.value ?: return
        val mods = _selectedModifiers.value
        val unitPrice = menu.price + mods.totalExtraPrice
        val variantStr = "${mods.ice.label} | ${mods.sugar.label}"
        val notesStr = mods.toSummary()

        val list = _cart.value.toMutableList()
        list.add(
            CartLine(
                menuId = menu.id,
                name = menu.name,
                variant = variantStr,
                qty = 1,
                unitPrice = unitPrice,
                notes = notesStr
            )
        )
        _cart.value = list
        closeCustomizer()
    }

    fun increaseInCart(index: Int) {
        val list = _cart.value.toMutableList()
        if (index in list.indices) {
            val item = list[index]
            list[index] = item.copy(qty = item.qty + 1)
            _cart.value = list
        }
    }

    fun decreaseFromCart(index: Int) {
        val list = _cart.value.toMutableList()
        if (index in list.indices) {
            val item = list[index]
            if (item.qty <= 1) {
                list.removeAt(index)
            } else {
                list[index] = item.copy(qty = item.qty - 1)
            }
            _cart.value = list
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun submitOrder(outletId: String) {
        val items = _cart.value
        if (items.isEmpty()) return
        val table = _tableNo.value.ifBlank { "01" }
        val name = _customerName.value.trim().ifBlank { "Tamu Meja $table" }
        val phone = _customerPhone.value.trim()
        val paymentMethod = if (_paymentChoice.value == "QRIS") "QRIS" else "TUNAI"

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            try {
                val orderId = orders.checkout(
                    outletId = outletId,
                    lines = items,
                    promo = null,
                    paymentMethod = paymentMethod,
                    shiftId = "",
                    customerName = "$name (Meja $table)",
                    orderType = "DINE_IN",
                    applyTax = false
                )
                // Catat loyalty stamp jika nomor WA diisi
                if (phone.isNotBlank()) {
                    val cups = items.sumOf { it.qty }
                    val total = items.sumOf { it.qty * it.unitPrice }
                    _stampResult.value = customers.recordOrderStamps(phone, name, cups, total)
                }

                _submittedOrderId.value = orderId
                _cart.value = emptyList()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Gagal mengirim pesanan. Silakan coba lagi."
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun resetOrder() {
        _submittedOrderId.value = null
        _cart.value = emptyList()
        _stampResult.value = null
        _errorMessage.value = null
    }
}
