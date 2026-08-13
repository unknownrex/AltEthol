package com.unknownrex.altethol.core.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notif_cache")
data class NotifCacheEntity(
    @PrimaryKey
    val idNotifikasi: String,
    val status: String,
    val lastSeenAt: Long,
)
