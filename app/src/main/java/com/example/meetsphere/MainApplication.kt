package com.example.meetsphere

import android.app.Application
import com.example.meetsphere.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }
}
