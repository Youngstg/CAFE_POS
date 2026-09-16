package com.coffeeos.erp.core.notify

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM skeleton MVP-4: order baru (KDS bunyi) + stok kritis (gudang/owner).
 * Notifikasi lokal saat offline ditangani ViewModel via evaluateIngredient (tanpa FCM).
 * TODO: buat channel + tampilkan NotificationCompat + data sync trigger.
 */
class CoffeeosMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val kind = message.data["kind"] ?: return
        when (kind) {
            "ORDER_NEW" -> { /* TODO: bunyikan KDS + refresh orders */ }
            "STOCK_CRITICAL" -> { /* TODO: tampilkan daftar bahan STOP */ }
            "PO_APPROVED" -> { /* TODO: notif ke gudang */ }
        }
    }

    override fun onNewToken(token: String) {
        // TODO: simpan token ke Firestore users/{uid}/fcmTokens untuk topic per outlet.
    }
}
