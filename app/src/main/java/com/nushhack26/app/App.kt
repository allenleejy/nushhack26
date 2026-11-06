package com.nushhack26.app

import android.app.Application
import com.nushhack26.app.alarm.OverdueAlarm

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
        OverdueAlarm.start(this, container)
    }
}
