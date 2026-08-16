package com.unknownrex.altethol.feature.di

import com.unknownrex.altethol.feature.auth.login.LoginViewModel
import com.unknownrex.altethol.feature.auth.sessioncheck.SessionCheckViewModel
import com.unknownrex.altethol.feature.history.HistoryViewModel
import com.unknownrex.altethol.feature.home.HomeViewModel
import com.unknownrex.altethol.feature.home.engine.AndroidEngineController
import com.unknownrex.altethol.feature.home.engine.AttendanceFlowRunner
import com.unknownrex.altethol.feature.home.engine.AttendanceSyncEngine
import com.unknownrex.altethol.feature.home.engine.DefaultPresensiContextResolver
import com.unknownrex.altethol.feature.home.engine.EngineController
import com.unknownrex.altethol.feature.home.engine.NotifDiffCoordinator
import com.unknownrex.altethol.feature.home.engine.PresensiContextResolver
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.factory
import org.koin.dsl.module
import org.koin.dsl.single

val featureModule: Module = module {
    singleOf(::AndroidEngineController) { bind<EngineController>() }
    viewModelOf(::SessionCheckViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::HistoryViewModel)

    factoryOf(::NotifDiffCoordinator)
    factoryOf(::DefaultPresensiContextResolver) { bind<PresensiContextResolver>() }
    factory {
        AttendanceFlowRunner(
            repository = get(),
            contextResolver = get(),
        )
    }
    single {
        AttendanceSyncEngine(
            repository = get(),
            cacheDao = get(),
            historyDao = get(),
            coordinator = get(),
            flowRunner = get(),
        )
    }
}
