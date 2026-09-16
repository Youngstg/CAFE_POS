package com.coffeeos.erp.core.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ConflictPolicyTest {
    @Test fun `stok cukup diterima`() {
        assertEquals(
            ConflictPolicy.Resolution.ACCEPT,
            ConflictPolicy.resolveOrder(1L, 1000.0, 150.0, emptySet(), "O-1")
        )
    }

    @Test fun `stok kurang masuk review`() {
        assertEquals(
            ConflictPolicy.Resolution.CONFLICT_REVIEW,
            ConflictPolicy.resolveOrder(2L, 100.0, 150.0, emptySet(), "O-2")
        )
    }

    @Test fun `duplikat diabaikan`() {
        assertEquals(
            ConflictPolicy.Resolution.DUPLICATE_IGNORED,
            ConflictPolicy.resolveOrder(1L, 1000.0, 150.0, setOf("O-1"), "O-1")
        )
    }
}
