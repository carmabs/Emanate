package com.carmabs.app

import android.app.Application
import com.carmabs.app.di.environmentModule
import com.carmabs.app.di.uiModule
import com.carmabs.app.di.useCaseModule
import com.carmabs.emanate.android.configuration.Android
import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.sample.injection.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 *  *<p>
 * Copyright (c) 2020, Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 *
 * Created by: Carlos Mateo Benito on 21/1/19.
 */
class EmaSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Emanate.init(EmaConfiguration.Android(debug = BuildConfig.DEBUG))
        startKoin {
            androidContext(this@EmaSampleApplication)
            modules(environmentModule, uiModule, useCaseModule)
        }
    }
}
