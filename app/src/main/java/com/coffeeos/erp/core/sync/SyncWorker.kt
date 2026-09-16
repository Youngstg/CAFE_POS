package com.coffeeos.erp.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sync worker skeleton: ambil pending_mutations dari Room, kirim ke Firestore
 * via runTransaction (khusus stok/PO/shift), hapus dari antrian jika sukses.
 *
 * Constraints (didaftarkan di modul DI): NetworkType.CONNECTED + backoff exponential.
 * TODO MVP-2: implementasi Firestore transaction + idempotency key (poId/orderId).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // TODO: baca peekQueue() -> kirim -> dequeue()
        return Result.success()
    }
}
