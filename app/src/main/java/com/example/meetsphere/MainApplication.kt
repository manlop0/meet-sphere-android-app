package com.example.meetsphere

import android.app.Application
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowCompat.enableEdgeToEdge
import com.example.meetsphere.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = Configuration.getInstance()

        val prefs = getSharedPreferences("osmdroid", MODE_PRIVATE)
        config.load(this, prefs)

        config.userAgentValue = BuildConfig.APPLICATION_ID

        val basePath = File(cacheDir, "osmdroid")
        config.osmdroidBasePath = basePath

        val tileCache = File(basePath, "tiles")
        config.osmdroidTileCache = tileCache

        config.tileDownloadThreads = 4
        config.tileFileSystemThreads = 4

        config.tileDownloadMaxQueueSize = 16
        config.tileFileSystemMaxQueueSize = 16

        config.cacheMapTileCount = 25

        config.isMapViewHardwareAccelerated = true

        config.isDebugMode = false
        config.isDebugTileProviders = false
    }
}
