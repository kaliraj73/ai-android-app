package com.yourapp

import android.app.Application

/**
 * Application class for AI App
 */
class AIAppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AIAppApplication
            private set
    }
}
