package com.coffeeos.erp.core.data.repo

import com.coffeeos.erp.core.data.repo.CartLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State untuk Customer-Facing Display (CFD) yang ditaruh menghadap pelanggan di kasir.
 */
data class CustomerDisplayState(
    val cafeName: String = "CoffeeOS",
    val outletName: String = "Outlet Utama",
    val items: List<CartLine> = emptyList(),
    val subtotal: Long = 0L,
    val discount: Long = 0L,
    val tax: Long = 0L,
    val total: Long = 0L,
    val orderType: String = "DINE_IN",
    val customerName: String = "",
    val paymentMethod: String = "TUNAI",
    val isShowingPayment: Boolean = false,
    val isPaymentSuccess: Boolean = false,
    val lastOrderId: String? = null,
    val qrisPayload: String = "",
)

/**
 * Singleton Bridge penghubung real-time antara terminal Kasir dan Layar Hadap Pelanggan (CFD).
 */
@Singleton
class CustomerDisplayBridge @Inject constructor() {

    private val _state = MutableStateFlow(CustomerDisplayState())
    val state: StateFlow<CustomerDisplayState> = _state.asStateFlow()

    fun updateCart(
        items: List<CartLine>,
        discount: Long,
        tax: Long,
        total: Long,
        orderType: String,
        customerName: String,
        cafeName: String = "CoffeeOS",
        outletName: String = "Outlet Utama"
    ) {
        val sub = items.sumOf { it.qty.toLong() * it.unitPrice }
        _state.value = _state.value.copy(
            cafeName = cafeName,
            outletName = outletName,
            items = items,
            subtotal = sub,
            discount = discount,
            tax = tax,
            total = total,
            orderType = orderType,
            customerName = customerName,
            isPaymentSuccess = false
        )
    }

    fun showPayment(method: String, total: Long, orderId: String) {
        val qris = "00020101021226600016ID.COFFEEOS.POS0118936000000000000000520458125303360540${total}5802ID5912COFFEEOS CAFE6007JAKARTA62070703A016304ABCD"
        _state.value = _state.value.copy(
            paymentMethod = method,
            isShowingPayment = true,
            isPaymentSuccess = false,
            lastOrderId = orderId,
            qrisPayload = qris
        )
    }

    fun markPaymentSuccess(orderId: String, total: Long) {
        _state.value = _state.value.copy(
            isShowingPayment = false,
            isPaymentSuccess = true,
            lastOrderId = orderId,
            total = total
        )
    }

    fun reset() {
        _state.value = CustomerDisplayState(
            cafeName = _state.value.cafeName,
            outletName = _state.value.outletName
        )
    }
}
