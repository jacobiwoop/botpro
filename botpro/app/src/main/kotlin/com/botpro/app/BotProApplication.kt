package com.botpro.app

import android.app.Application

class BotProApplication : Application() {

    companion object {
        @Volatile
        lateinit var instance: BotProApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
