package com.unknownrex.altethol.core.data.session

import kotlinx.coroutines.flow.Flow

interface SessionStorage {
    val session: Flow<SessionState>

    suspend fun saveSession(token: String, cookie: String? = null)

    suspend fun saveMahasiswaId(id: Int)

    suspend fun clear()
}
