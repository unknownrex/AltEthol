package com.unknownrex.altethol.core.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unknownrex.altethol.core.data.local.db.entity.NotifCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotifCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notif: NotifCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notifs: List<NotifCacheEntity>)

    @Query("SELECT * FROM notif_cache")
    suspend fun getAll(): List<NotifCacheEntity>

    @Query("SELECT * FROM notif_cache WHERE idNotifikasi = :id")
    suspend fun getById(id: String): NotifCacheEntity?

    @Query("SELECT * FROM notif_cache")
    fun observeAll(): Flow<List<NotifCacheEntity>>

    @Query("DELETE FROM notif_cache")
    suspend fun clear()
}
