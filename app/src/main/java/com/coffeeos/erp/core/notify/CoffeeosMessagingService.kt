package com.coffeeos.erp.core.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.coffeeos.erp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM tampil (bukan skeleton): order baru untuk KDS, stok kritis untuk
 * gudang/owner, PO disetujui untuk gudang. Berfungsi saat online;
 * saat offline notifikasi lokal ditangani ViewModel masing-masing.
 */
class CoffeeosMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val (channel, title, body) = when (data["kind"]) {
            "ORDER_NEW" -> Triple(
                CH_ORDERS, "Order baru ${data["orderId"] ?: ""}",
                "Total Rp${data["total"] ?: 0} — cek KDS"
            )
            "STOCK_CRITICAL" -> Triple(
                CH_STOCK, "Stok kritis: ${data["ingredient"] ?: ""}",
                "Sisa ${data["percent"] ?: "?"}% — menu terkait mati, segera buat PO"
            )
            "PO_APPROVED" -> Triple(
                CH_STOCK, "PO ${data["poId"] ?: ""} disetujui",
                "Gudang bisa terima barang"
            )
            else -> Triple(CH_GENERAL, message.notification?.title ?: "CoffeeOS", message.notification?.body ?: "")
        }
        if (title.isBlank() && body.isBlank()) return
        show(channel, title, body)
    }

    private fun show(channelId: String, title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, channelName(channelId), NotificationManager.IMPORTANCE_HIGH)
            )
            manager.createNotificationChannel(
                NotificationChannel(CH_GENERAL, "Umum", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
    }

    private fun channelName(id: String): String = when (id) {
        CH_ORDERS -> "Order masuk (KDS)"
        CH_STOCK -> "Stok & PO"
        else -> "Umum"
    }

    override fun onNewToken(token: String) {
        // Langkah lanjutan: simpan ke Firestore users/{uid}/fcmTokens + topic per outlet
        // agar Cloud Function bisa target order/stok ke device yang tepat.
    }

    companion object {
        const val CH_ORDERS = "coffeeos_orders"
        const val CH_STOCK = "coffeeos_stock"
        const val CH_GENERAL = "coffeeos_general"
    }
}
