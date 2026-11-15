package com.nonen.Bookkeeping

import android.app.Application
import com.nonen.Bookkeeping.debug.CaptureDebug

class BookkeepingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CaptureDebug.init(this)
    }
}
