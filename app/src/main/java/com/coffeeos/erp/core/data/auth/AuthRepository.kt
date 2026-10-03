package com.coffeeos.erp.core.data.auth

import com.coffeeos.erp.core.data.session.SessionManager
import com.coffeeos.erp.core.domain.auth.PinHash
import com.coffeeos.erp.core.domain.auth.UserRole
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Hasil login (demo, email Firebase, atau PIN cepat — seragam ke Session). */
data class LoginResult(val session: SessionManager.Session)

/** Akun demo 1 APK multi-role (tanpa Firebase, untuk evaluator). PIN semua: 123456. */
object DemoAccounts {
    data class Account(val pin: String, val role: UserRole, val name: String)

    val accounts: Map<String, Account> = mapOf(
        "owner" to Account("123456", UserRole.OWNER, "Owner"),
        "admin" to Account("123456", UserRole.ADMIN_OUTLET, "Admin Cabang"),
        "kasir" to Account("123456", UserRole.CASHIER, "Kasir 1"),
        "dapur" to Account("123456", UserRole.KITCHEN, "Dapur 1"),
        "gudang" to Account("123456", UserRole.WAREHOUSE, "Gudang 1"),
    )
}

/**
 * 3 jalur login:
 * 1. [loginEmail] — online via Firebase Auth; role/tenant/outlet dari Custom Claims
 *    (diset owner via tools/set-claims.js). Wajib online sekali.
 * 2. [setupPin]/[loginQuickPin] — PIN 6-digit lokal (hash SHA-256) untuk masuk
 *    cepat saat offline setelah login email. Kasir tidak hafal password panjang.
 * 3. [loginDemo] — akun demo lokal (tanpa Firebase sama sekali).
 */
@Singleton
class AuthRepository @Inject constructor(
    private val session: SessionManager,
) {
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }

    suspend fun loginEmail(email: String, password: String): LoginResult {
        require(email.isNotBlank() && password.isNotBlank()) { "Email & password wajib diisi" }
        val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("Login gagal")
        // Paksa refresh agar Custom Claims terbaru (role/tenantId) terbaca.
        val claims = user.getIdToken(true).await().claims
        val roleName = claims["role"] as? String
            ?: throw IllegalStateException("Akun belum punya role (set via set-claims.js)")
        val role = runCatching { UserRole.valueOf(roleName) }.getOrNull()
            ?: throw IllegalStateException("Role tidak dikenal: $roleName")
        val s = SessionManager.Session(
            uid = user.uid,
            role = role,
            tenantId = claims["tenantId"] as? String ?: "",
            outletId = claims["outletId"] as? String ?: "",
            displayName = (claims["displayName"] as? String)
                ?: user.displayName ?: user.email ?: "Staff"
        )
        require(s.tenantId.isNotBlank() && s.outletId.isNotBlank()) {
            "Akun belum terikat tenant/outlet (set via set-claims.js)"
        }
        session.save(s)
        return LoginResult(s)
    }

    /** Daftarkan PIN cepat untuk sesi saat ini (butuh sesi aktif). */
    suspend fun setupPin(pin: String) {
        require(pin.length >= 4) { "PIN minimal 4 digit" }
        val s = session.session.first() ?: throw IllegalStateException("Belum login")
        session.savePinHash(PinHash.hash(s.uid, pin))
    }

    /** Masuk offline dengan PIN cepat (sesi + hash harus sudah ada). */
    suspend fun loginQuickPin(pin: String): LoginResult {
        val s = session.session.first() ?: throw IllegalStateException("Belum ada sesi tersimpan")
        val hash = session.readPinHash() ?: throw IllegalStateException("PIN belum diatur")
        if (!PinHash.verify(s.uid, pin, hash)) throw IllegalArgumentException("PIN salah")
        return LoginResult(s)
    }

    suspend fun hasQuickPin(): Boolean = session.readPinHash() != null

    /** Login demo offline (evaluator tanpa Firebase). */
    suspend fun loginDemo(username: String, pin: String): LoginResult {
        val acc = DemoAccounts.accounts[username.trim().lowercase()]
            ?: throw IllegalArgumentException("Akun tidak dikenal")
        if (acc.pin != pin) throw IllegalArgumentException("PIN salah")
        val s = SessionManager.Session(
            uid = "demo-$username", role = acc.role,
            tenantId = "tenant-demo", outletId = "outlet-1", displayName = acc.name
        )
        session.save(s)
        return LoginResult(s)
    }

    /**
     * Verifikasi PIN Supervisor / Manager / Owner untuk aksi berisiko tinggi
     * (Void transaksi, paksa lunas konflik stok, hapus master data).
     */
    suspend fun verifySupervisorPin(pin: String): Boolean {
        if (pin.isBlank()) return false
        // 1. Cek terhadap master PIN supervisor default (123456) atau akun owner
        if (pin == "123456" || pin == DemoAccounts.accounts["owner"]?.pin) return true

        // 2. Jika akun yang login adalah Owner/Admin, verifikasi PIN aktifnya
        val s = session.session.first()
        val savedHash = session.readPinHash()
        if (s != null && (s.role == UserRole.OWNER || s.role == UserRole.ADMIN_OUTLET) && savedHash != null) {
            if (PinHash.verify(s.uid, pin, savedHash)) return true
        }
        return false
    }

    /**
     * Memastikan sesi memiliki token Firebase Auth aktif di perangkat.
     * Jika pengguna belum login online via email, signInAnonymously digunakan
     * agar request Cloud Firestore membawa token valid dan tidak tertolak transport layer.
     */
    suspend fun ensureCloudAuth() {
        if (firebaseAuth.currentUser == null) {
            runCatching {
                firebaseAuth.signInAnonymously().await()
            }
        }
    }

    suspend fun logout() {
        runCatching { firebaseAuth.signOut() }
        session.clear()
    }
}
