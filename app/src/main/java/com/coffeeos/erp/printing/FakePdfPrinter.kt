package com.coffeeos.erp.printing

import android.content.Context
import com.coffeeos.erp.core.domain.order.Receipt
import com.coffeeos.erp.core.domain.order.ReceiptFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Printer FAKE untuk fase porto tanpa hardware.
 * Menulis teks struk ke cache + dibagikan sebagai file (nanti: render PDF beneran
 * via android.graphics.pdf.PdfDocument — format teksnya tetap dari ReceiptFormatter
 * agar kompatibel dengan printer ESC/POS asli).
 */
class FakePdfPrinter @Inject constructor(
    @ApplicationContext private val context: Context,
) : PrinterRepository {

    override suspend fun printReceipt(receipt: Receipt): PrintResult = try {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "struk-${receipt.orderId}.txt")
        file.writeText(ReceiptFormatter.format(receipt))
        PrintResult.Success(file.absolutePath)
    } catch (e: Exception) {
        PrintResult.Failed(e.message ?: "gagal tulis struk fake")
    }
}
