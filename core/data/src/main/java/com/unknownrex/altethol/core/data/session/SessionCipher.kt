package com.unknownrex.altethol.core.data.session

import android.content.Context
import android.util.Base64
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

class SessionCipher(context: Context) {

    private val appContext = context.applicationContext

    private val aead: Aead by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(appContext, SESSION_KEYSET_PREF, SESSION_KEYSET_ALIAS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(masterKey.toString())
            .build()
            .keysetHandle
        keysetHandle.getPrimitive(Aead::class.java)
    }

    fun encrypt(value: String, associatedData: ByteArray): String {
        val cipherText = aead.encrypt(value.encodeToByteArray(), associatedData)
        return Base64.encodeToString(cipherText, Base64.NO_WRAP)
    }

    fun decrypt(value: String, associatedData: ByteArray): String {
        val cipherText = Base64.decode(value, Base64.NO_WRAP)
        return aead.decrypt(cipherText, associatedData).decodeToString()
    }

    private companion object {
        const val SESSION_KEYSET_PREF = "session_keyset"
        const val SESSION_KEYSET_ALIAS = "session_key_alias"
    }
}
