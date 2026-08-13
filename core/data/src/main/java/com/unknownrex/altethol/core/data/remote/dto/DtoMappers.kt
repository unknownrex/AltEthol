package com.unknownrex.altethol.core.data.remote.dto

import com.unknownrex.altethol.core.data.local.db.entity.NotifCacheEntity

fun NotifikasiDto.toNotifCacheEntity(now: Long = System.currentTimeMillis()): NotifCacheEntity =
    NotifCacheEntity(
        idNotifikasi = idNotifikasi,
        status = status,
        lastSeenAt = now,
    )
