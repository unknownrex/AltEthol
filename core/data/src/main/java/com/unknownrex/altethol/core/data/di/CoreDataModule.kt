package com.unknownrex.altethol.core.data.di

import com.unknownrex.altethol.core.data.local.db.AltEtholDatabase
import com.unknownrex.altethol.core.data.remote.AttendanceRepository
import com.unknownrex.altethol.core.data.remote.AuthRepository
import com.unknownrex.altethol.core.data.remote.DefaultAttendanceRepository
import com.unknownrex.altethol.core.data.remote.DefaultAuthRepository
import com.unknownrex.altethol.core.data.session.SessionCipher
import com.unknownrex.altethol.core.data.session.SessionPreferences
import com.unknownrex.altethol.core.data.session.SessionStorage
import com.unknownrex.altethol.core.data.session.TokenRefresher
import com.unknownrex.altethol.core.data.settings.SettingsPreferences
import com.unknownrex.altethol.core.data.settings.SettingsStorage
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreDataModule: Module = module {
    single(named(DiQualifiers.SESSION_DATASTORE)) { createSessionDataStore(get()) }
    singleOf(::SessionCipher)
    single(named(DiQualifiers.SETTINGS_DATASTORE)) { createSettingsDataStore(get()) }

    single<SessionStorage> { SessionPreferences(get(named(DiQualifiers.SESSION_DATASTORE)), get()) }
    single<SettingsStorage> { SettingsPreferences(get(named(DiQualifiers.SETTINGS_DATASTORE))) }

    single {
        TokenRefresher(
            sessionStorage = get(),
            authRepository = get(),
        )
    }

    single { createAltEtholDatabase(get()) }
    single { get<AltEtholDatabase>().absensiHistoriDao() }
    single { get<AltEtholDatabase>().notifCacheDao() }
    singleOf(::DefaultAuthRepository) { bind<AuthRepository>() }
    singleOf(::DefaultAttendanceRepository) { bind<AttendanceRepository>() }
}
