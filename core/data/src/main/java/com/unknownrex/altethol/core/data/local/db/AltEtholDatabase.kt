package com.unknownrex.altethol.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.unknownrex.altethol.core.data.local.db.dao.AbsensiHistoriDao
import com.unknownrex.altethol.core.data.local.db.dao.NotifCacheDao
import com.unknownrex.altethol.core.data.local.db.entity.AbsensiHistoriEntity
import com.unknownrex.altethol.core.data.local.db.entity.NotifCacheEntity

@Database(
    entities = [
        AbsensiHistoriEntity::class,
        NotifCacheEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AltEtholDatabase : RoomDatabase() {
    abstract fun absensiHistoriDao(): AbsensiHistoriDao
    abstract fun notifCacheDao(): NotifCacheDao
}
