package com.unknownrex.altethol.di

import com.unknownrex.altethol.BuildConfig
import com.unknownrex.altethol.core.data.di.DiQualifiers
import com.unknownrex.altethol.core.data.network.HttpClientFactory
import com.unknownrex.altethol.core.data.session.SessionPreferences
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.flow.first
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule: Module = module {
    single(named(DiQualifiers.BASE_URL)) { BuildConfig.BASE_URL }

    single {
        HttpClientFactory.create(
            baseUrl = get(named(DiQualifiers.BASE_URL)),
            tokenProvider = { get<SessionPreferences>().token.first() },
            cookieProvider = { get<SessionPreferences>().cookiePhpSessId.first() },
            loggingLevel = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE,
        )
    }
}
