package com.coffeeos.erp.core.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

private val IDR = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
    currency = Currency.getInstance("IDR")
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}

/** Format Long ke "Rp1.500.000" */
fun Long.toRupiah(): String = IDR.format(this)

/** Format Double ke "Rp1.500" */
fun Double.toRupiah(): String = IDR.format(this.toLong())

/** Format waktu millis ke "HH:mm" */
fun Long.toTimeString(): String =
    SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(this))

/** Format waktu millis ke "dd MMM, HH:mm" */
fun Long.toDateTimeString(): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(this))

/** Format nama hari ("Sen", "Sel", "Rab", dll) */
fun Long.toDayName(): String =
    SimpleDateFormat("EEE", Locale("id", "ID")).format(Date(this))

/** Format tanggal singkat ("dd/MM") */
fun Long.toDateShort(): String =
    SimpleDateFormat("dd/MM", Locale("id", "ID")).format(Date(this))
