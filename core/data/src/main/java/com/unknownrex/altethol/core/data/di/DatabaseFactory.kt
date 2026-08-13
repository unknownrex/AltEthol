package com.unknownrex.altethol.core.data.di

import android.content.Context
import androidx.room.Room
import com.unknownrex.altethol.core.data.local.db.AltEtholDatabase

fun createAltEtholDatabase(context: Context): AltEtholDatabase =
    Room.databaseBuilder(
        context,
        AltEtholDatabase::class.java,
        "altethol.db",
    ).build()
