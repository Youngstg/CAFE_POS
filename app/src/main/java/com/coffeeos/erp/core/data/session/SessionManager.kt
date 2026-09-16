package com.coffeeos.erp.core.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.coffeeos.erp.core.domain.auth.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionStore by preferencesDataStore("session")

/** Sesi login tersimpan lokal (tetap login saat offline). PIN cepat untuk kasir. */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val UID = stringPreferencesKey("uid")
    private val ROLE = stringPreferencesKey("role")
    private val TENANT = stringPreferencesKey("tenantId")
    private val OUTLET = stringPreferencesKey("outletId")
    private val NAME = stringPreferencesKey("displayName")
    private val PIN_HASH = stringPreferencesKey("pinHash")

    data class Session(
        val uid: String,
        val role: UserRole,
        val tenantId: String,
        val outletId: String,
        val displayName: String,
    )

    val session: Flow<Session?> = context.sessionStore.data.map { p ->
        val uid = p[UID] ?: return@map null
        val role = p[ROLE]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: return@map null
        Session(uid, role, p[TENANT] ?: "", p[OUTLET] ?: "", p[NAME] ?: "")
    }

    suspend fun save(s: Session) {
        context.sessionStore.edit { e ->
            e[UID] = s.uid; e[ROLE] = s.role.name
            e[TENANT] = s.tenantId; e[OUTLET] = s.outletId; e[NAME] = s.displayName
        }
    }

    /** Hash PIN cepat (lihat PinHash). Disimpan terpisah agar ganti sesi tidak bocor. */
    suspend fun savePinHash(hash: String) {
        context.sessionStore.edit { it[PIN_HASH] = hash }
    }

    suspend fun readPinHash(): String? =
        context.sessionStore.data.map { it[PIN_HASH] }.first()

    suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }
}
