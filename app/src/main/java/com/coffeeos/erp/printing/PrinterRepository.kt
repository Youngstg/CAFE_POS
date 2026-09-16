package com.coffeeos.erp.printing

import com.coffeeos.erp.core.domain.order.Receipt

/** Abstraksi printer. MVP = FakePdfPrinter. Nanti tambah BluetoothEscPosPrinter. */
interface PrinterRepository {
    /** Mencetak struk, mengembalikan lokasi file/log untuk ditampilkan ke kasir. */
    suspend fun printReceipt(receipt: Receipt): PrintResult
    /** Dipakai laci uang via printer (RJ11). Fake = no-op tercatat. */
    suspend fun kickDrawer(): Boolean = true
}

sealed interface PrintResult {
    data class Success(val location: String) : PrintResult
    data class Failed(val reason: String) : PrintResult
}
