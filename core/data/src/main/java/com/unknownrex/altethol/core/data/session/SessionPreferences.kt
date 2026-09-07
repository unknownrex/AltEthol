package com.unknownrex.altethol.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SessionPreferences(
    private val dataStore: DataStore<Preferences>,
    private val cipher: SessionCipher,
) : SessionStorage {

    override val session: Flow<SessionState> = combine(
        dataStore.data.map { it[Keys.IS_LOGGED_IN] ?: false },
        dataStore.data.map { decrypt(it, Keys.TOKEN) },
        dataStore.data.map { decrypt(it, Keys.REFRESH_TOKEN) },
        dataStore.data.map { decrypt(it, Keys.MAHASISWA_ID)?.toIntOrNull() },
    ) { isLoggedIn, token, refreshToken, mahasiswaId ->
        SessionState(
            isLoggedIn = isLoggedIn,
            token = token,
            refreshToken = refreshToken,
            mahasiswaId = mahasiswaId,
        )
    }

    val token: Flow<String?> = dataStore.data.map { decrypt(it, Keys.TOKEN) }

    val refreshToken: Flow<String?> = dataStore.data.map { decrypt(it, Keys.REFRESH_TOKEN) }

    val mahasiswaId: Flow<Int?> = dataStore.data.map {
        decrypt(it, Keys.MAHASISWA_ID)?.toIntOrNull()
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[Keys.IS_LOGGED_IN] ?: false }

    override suspend fun saveSession(token: String, refreshToken: String?) {
        dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = cipher.encrypt(token, TOKEN_AD)
            prefs[Keys.IS_LOGGED_IN] = true
            refreshToken?.let {
                prefs[Keys.REFRESH_TOKEN] = cipher.encrypt(it, REFRESH_TOKEN_AD)
            }
        }
    }

    override suspend fun saveToken(token: String) {
        dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = cipher.encrypt(token, TOKEN_AD)
        }
    }

    override suspend fun saveMahasiswaId(id: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.MAHASISWA_ID] = cipher.encrypt(id.toString(), MAHASISWA_ID_AD)
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun decrypt(prefs: Preferences, key: Preferences.Key<String>): String? {
        val encrypted = prefs[key] ?: return null
        return runCatching { cipher.decrypt(encrypted, associatedData(key)) }.getOrNull()
    }

    private fun associatedData(key: Preferences.Key<String>): ByteArray = when (key) {
        Keys.TOKEN -> TOKEN_AD
        Keys.REFRESH_TOKEN -> REFRESH_TOKEN_AD
        Keys.MAHASISWA_ID -> MAHASISWA_ID_AD
        else -> ByteArray(0)
    }

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val MAHASISWA_ID = stringPreferencesKey("mahasiswa_id")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    private companion object {
        val TOKEN_AD = "token".encodeToByteArray()
        val REFRESH_TOKEN_AD = "refresh_token".encodeToByteArray()
        val MAHASISWA_ID_AD = "mahasiswa_id".encodeToByteArray()
    }
}