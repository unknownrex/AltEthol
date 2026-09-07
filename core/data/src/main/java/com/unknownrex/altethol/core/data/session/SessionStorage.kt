package com.unknownrex.altethol.core.data.session

import kotlinx.coroutines.flow.Flow

interface SessionStorage {
    val session: Flow<SessionState>

    suspend fun saveSession(token: String, refreshToken: String? = null)

    suspend fun saveToken(token: String)

    suspend fun saveMahasiswaId(id: Int)

    suspend fun clear()
}