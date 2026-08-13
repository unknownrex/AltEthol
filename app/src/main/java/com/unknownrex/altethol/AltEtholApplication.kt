package com.unknownrex.altethol

import android.app.Application
import com.unknownrex.altethol.core.data.di.coreDataModule
import com.unknownrex.altethol.di.appModule
import com.unknownrex.altethol.feature.di.featureModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AltEtholApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AltEtholApplication)
            modules(appModule, coreDataModule, featureModule)
        }
    }
}
