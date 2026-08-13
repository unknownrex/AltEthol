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
) {

    val session: Flow<SessionState> = combine(
        dataStore.data.map { it[Keys.IS_LOGGED_IN] ?: false },
        dataStore.data.map { decrypt(it, Keys.TOKEN) },
        dataStore.data.map { decrypt(it, Keys.COOKIE) },
        dataStore.data.map { decrypt(it, Keys.MAHASISWA_ID)?.toIntOrNull() },
    ) { isLoggedIn, token, cookie, mahasiswaId ->
        SessionState(
            isLoggedIn = isLoggedIn,
            token = token,
            cookiePhpSessId = cookie,
            mahasiswaId = mahasiswaId,
        )
    }

    val token: Flow<String?> = dataStore.data.map { decrypt(it, Keys.TOKEN) }

    val cookiePhpSessId: Flow<String?> = dataStore.data.map { decrypt(it, Keys.COOKIE) }

    val mahasiswaId: Flow<Int?> = dataStore.data.map {
        decrypt(it, Keys.MAHASISWA_ID)?.toIntOrNull()
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[Keys.IS_LOGGED_IN] ?: false }

    suspend fun saveSession(token: String, cookie: String? = null) {
        dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = cipher.encrypt(token, TOKEN_AD)
            prefs[Keys.IS_LOGGED_IN] = true
            cookie?.let {
                prefs[Keys.COOKIE] = cipher.encrypt(it, COOKIE_AD)
            }
        }
    }

    suspend fun saveMahasiswaId(id: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.MAHASISWA_ID] = cipher.encrypt(id.toString(), MAHASISWA_ID_AD)
        }
    }

    suspend fun saveCookie(cookie: String) {
        dataStore.edit { prefs ->
            prefs[Keys.COOKIE] = cipher.encrypt(cookie, COOKIE_AD)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun decrypt(prefs: Preferences, key: Preferences.Key<String>): String? {
        val encrypted = prefs[key] ?: return null
        return runCatching { cipher.decrypt(encrypted, associatedData(key)) }.getOrNull()
    }

    private fun associatedData(key: Preferences.Key<String>): ByteArray = when (key) {
        Keys.TOKEN -> TOKEN_AD
        Keys.COOKIE -> COOKIE_AD
        Keys.MAHASISWA_ID -> MAHASISWA_ID_AD
        else -> ByteArray(0)
    }

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val COOKIE = stringPreferencesKey("cookie_php_sess_id")
        val MAHASISWA_ID = stringPreferencesKey("mahasiswa_id")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    private companion object {
        val TOKEN_AD = "token".encodeToByteArray()
        val COOKIE_AD = "cookie".encodeToByteArray()
        val MAHASISWA_ID_AD = "mahasiswa_id".encodeToByteArray()
    }
}
