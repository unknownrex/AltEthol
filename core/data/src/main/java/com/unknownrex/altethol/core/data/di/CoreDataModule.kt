package com.unknownrex.altethol.core.data.di

import com.unknownrex.altethol.core.data.local.db.AltEtholDatabase
import com.unknownrex.altethol.core.data.session.SessionCipher
import com.unknownrex.altethol.core.data.session.SessionPreferences
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreDataModule: Module = module {
    single { createSessionDataStore(get()) }
    singleOf(::SessionCipher)
    singleOf(::SessionPreferences)
    single { createAltEtholDatabase(get()) }
    single { get<AltEtholDatabase>().absensiHistoriDao() }
    single { get<AltEtholDatabase>().notifCacheDao() }
}
