package com.unknownrex.altethol.core.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unknownrex.altethol.core.data.local.db.entity.AbsensiHistoriEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AbsensiHistoriDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(histori: AbsensiHistoriEntity): Long

    @Query("SELECT * FROM absensi_histori ORDER BY waktuDeteksi DESC")
    fun observeAll(): Flow<List<AbsensiHistoriEntity>>

    @Query("DELETE FROM absensi_histori")
    suspend fun clear()
}
