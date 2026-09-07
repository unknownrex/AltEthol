package com.unknownrex.altethol.core.data.session

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

class SessionCipher(context: Context) {

    private val appContext = context.applicationContext

    private val aead: Aead by lazy {
        try {
            buildAead()
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Session keyset cannot be read with the current keystore key, resetting", e)
            resetKeystore()
            buildAead()
        } catch (e: IOException) {
            Log.w(TAG, "Session keyset cannot be read from preferences, resetting", e)
            resetKeystore()
            buildAead()
        }
    }

    fun encrypt(value: String, associatedData: ByteArray): String {
        val cipherText = aead.encrypt(value.encodeToByteArray(), associatedData)
        return Base64.encodeToString(cipherText, Base64.NO_WRAP)
    }

    fun decrypt(value: String, associatedData: ByteArray): String {
        val cipherText = Base64.decode(value, Base64.NO_WRAP)
        return aead.decrypt(cipherText, associatedData).decodeToString()
    }

    private fun buildAead(): Aead {
        val masterKey = MasterKey.Builder(appContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(appContext, KEYSET_NAME, KEYSET_PREF_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://${MasterKey.DEFAULT_MASTER_KEY_ALIAS}")
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(Aead::class.java)
    }

    private fun resetKeystore() {
        runCatching {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }
        appContext.getSharedPreferences(KEYSET_PREF_FILE, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        appContext.getSharedPreferences(LEGACY_KEYSET_PREF_FILE, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private companion object {
        const val KEYSET_NAME = "session_keyset"
        const val KEYSET_PREF_FILE = "session_keyset_prefs"
        const val LEGACY_KEYSET_PREF_FILE = "session_key_alias"
        const val TAG = "AltEtholSessionCipher"
    }
}
