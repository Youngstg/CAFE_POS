package com.coffeeos.erp.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker sync FIFO: Room pending_mutations -> Firestore (lihat FirestoreSync).
 * Dijadwalkan oleh SyncTrigger setiap habis enqueue (hanya jalan saat online).
 * - Sukses semua -> Result.success().
 * - Ada gagal (offline lagi / permission) -> Result.retry() (backoff exponential).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: FirestoreSync,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val r = sync.syncOnce()
            if (r.failed > 0 && r.processed == 0) Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
