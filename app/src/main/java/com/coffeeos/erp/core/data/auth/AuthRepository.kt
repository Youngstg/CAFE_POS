package com.coffeeos.erp.core.data.auth

import com.coffeeos.erp.core.data.session.SessionManager
import com.coffeeos.erp.core.domain.auth.UserRole
import javax.inject.Inject

/** Hasil login demo. MVP-1: PIN lokal. Berikutnya: Firebase Auth + Custom Claims. */
data class LoginResult(val session: SessionManager.Session)

/** Akun demo 1 APK multi-role (ganti Firebase di MVP-1 final). PIN semua: 123456. */
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

class AuthRepository @Inject constructor(
    private val session: SessionManager,
) {
    /** Login PIN offline (demo). Firebase Auth menyusul tanpa ubah ViewModel. */
    suspend fun loginPin(username: String, pin: String): LoginResult {
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

    suspend fun logout() = session.clear()
}
