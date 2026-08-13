package com.unknownrex.altethol.feature.di

import com.unknownrex.altethol.feature.auth.login.LoginViewModel
import com.unknownrex.altethol.feature.auth.sessioncheck.SessionCheckViewModel
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
import org.koin.dsl.module

val featureModule: Module = module {
    singleOf(::AndroidEngineController) { bind<EngineController>() }
    viewModelOf(::SessionCheckViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::HomeViewModel)

    factoryOf(::NotifDiffCoordinator)
    factoryOf(::DefaultPresensiContextResolver) { bind<PresensiContextResolver>() }
    factoryOf(::AttendanceFlowRunner)
    singleOf(::AttendanceSyncEngine)
}
