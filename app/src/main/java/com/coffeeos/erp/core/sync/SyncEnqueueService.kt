package com.coffeeos.erp.core.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Placeholder service agar manifest valid; sync asli via WorkManager (SyncWorker). */
class SyncEnqueueService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
