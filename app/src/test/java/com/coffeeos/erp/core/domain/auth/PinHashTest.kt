package com.coffeeos.erp.core.domain.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHashTest {
    @Test fun `hash stabil dan verify benar`() {
        val h = PinHash.hash("demo-kasir", "123456")
        assertTrue(PinHash.verify("demo-kasir", "123456", h))
        assertTrue(PinHash.verify("demo-kasir", "123456", h.uppercase()))
    }

    @Test fun `pin salah atau uid beda ditolak`() {
        val h = PinHash.hash("demo-kasir", "123456")
        assertFalse(PinHash.verify("demo-kasir", "000000", h))
        assertFalse(PinHash.verify("demo-lain", "123456", h))
    }
}
