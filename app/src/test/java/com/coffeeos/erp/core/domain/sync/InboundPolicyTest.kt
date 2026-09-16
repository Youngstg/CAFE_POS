package com.coffeeos.erp.core.domain.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InboundPolicyTest {

    @Test fun `lokal pending menang atas server`() {
        assertFalse(InboundPolicy.shouldApplyDoc(localPendingSync = true))
        assertTrue(InboundPolicy.shouldApplyDoc(localPendingSync = false))
    }

    @Test fun `ingredient last write wins`() {
        assertTrue(InboundPolicy.shouldApplyVersioned(serverUpdatedAt = 200, localUpdatedAt = 100))
        assertFalse(InboundPolicy.shouldApplyVersioned(serverUpdatedAt = 100, localUpdatedAt = 200))
        assertFalse(InboundPolicy.shouldApplyVersioned(serverUpdatedAt = 200, localUpdatedAt = 200))
        assertTrue(InboundPolicy.shouldApplyVersioned(serverUpdatedAt = 200, localUpdatedAt = null))
    }

    @Test fun `removed diabaikan`() {
        assertFalse(InboundPolicy.shouldApplyRemoved())
    }
}
