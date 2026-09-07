package com.unknownrex.altethol.di

import android.util.Log
import com.unknownrex.altethol.BuildConfig
import com.unknownrex.altethol.core.data.di.DiQualifiers
import com.unknownrex.altethol.core.data.network.HttpClientFactory
import com.unknownrex.altethol.core.data.network.cookie.AuthCookieBuilder
import com.unknownrex.altethol.core.data.session.SessionStorage
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
            authCookieProvider = {
                val state = get<SessionStorage>().session.first()
                val cookie = AuthCookieBuilder.build(
                    token = state.token,
                    refreshToken = state.refreshToken,
                )
                Log.d("AltEtholAuthCookie", "built cookie token=${state.token?.length ?: -1}chars refreshToken=${state.refreshToken?.length ?: -1}chars -> cookie=$cookie")
                cookie
            },
            loggingLevel = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE,
        )
    }
}
