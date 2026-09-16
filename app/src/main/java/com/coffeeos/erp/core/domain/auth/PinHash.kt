package com.coffeeos.erp.core.domain.auth

import java.security.MessageDigest

/**
 * Hash PIN cepat offline (SHA-256 dari "uid:pin").
 * Murni JVM — ter-test dan dicerminkan tools/verify_auth.py.
 * BUKAN pengganti password server; hanya kunci lokal agar kasir bisa
 * masuk cepat saat offline setelah 1x login email Firebase.
 */
object PinHash {
    fun hash(uid: String, pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$uid:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(uid: String, pin: String, expectedHash: String): Boolean =
        hash(uid, pin) == expectedHash.lowercase()
}
