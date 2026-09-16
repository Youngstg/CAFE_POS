package com.coffeeos.erp.core.domain.sync

/**
 * Kebijakan inbound murni (Firestore -> Room), cermin tools/verify_inbound.py.
 *
 * Anti-loop & anti-timpa:
 * 1. Snapshot dengan pendingWrites lokal diabaikan (gema tulisan sendiri).
 * 2. Baris lokal yang masih pendingSync=true MENANG atas server (tulisan lokal
 *    belum diakui server; timpa nanti setelah mutasinya terkirim).
 * 3. Ingredient/menu: last-write-wins berdasarkan updatedAt (jam server).
 */
object InboundPolicy {

    /** Order/shift/PO: terapkan server hanya jika lokal sudah tersinkron. */
    fun shouldApplyDoc(localPendingSync: Boolean): Boolean = !localPendingSync

    /** Ingredient: terapkan jika server lebih baru (atau lokal belum ada). */
    fun shouldApplyVersioned(serverUpdatedAt: Long?, localUpdatedAt: Long?): Boolean {
        if (localUpdatedAt == null) return true
        if (serverUpdatedAt == null) return false
        return serverUpdatedAt > localUpdatedAt
    }

    /** Perubahan REMOVED dari server diabaikan (arsip lokal dipertahankan). */
    fun shouldApplyRemoved(): Boolean = false
}
