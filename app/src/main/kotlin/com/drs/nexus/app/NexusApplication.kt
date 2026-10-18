package com.drs.nexus.app

import android.app.Application
import com.drs.nexus.app.data.AppContainer

/** Application entry point with manual dependency container (no framework). */
class NexusApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
