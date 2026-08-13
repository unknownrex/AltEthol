package com.unknownrex.altethol.core.data.di

import com.unknownrex.altethol.core.data.local.db.AltEtholDatabase
import com.unknownrex.altethol.core.data.remote.AuthRepository
import com.unknownrex.altethol.core.data.remote.DefaultAuthRepository
import com.unknownrex.altethol.core.data.session.SessionCipher
import com.unknownrex.altethol.core.data.session.SessionPreferences
import com.unknownrex.altethol.core.data.session.SessionStorage
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreDataModule: Module = module {
    single { createSessionDataStore(get()) }
    singleOf(::SessionCipher)
    singleOf(::SessionPreferences) { bind<SessionStorage>() }
    single { createAltEtholDatabase(get()) }
    single { get<AltEtholDatabase>().absensiHistoriDao() }
    single { get<AltEtholDatabase>().notifCacheDao() }
    singleOf(::DefaultAuthRepository) { bind<AuthRepository>() }
}
