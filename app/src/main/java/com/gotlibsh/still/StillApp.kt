package com.gotlibsh.still

import android.app.Application
import com.gotlibsh.still.data.AppRepository
import com.gotlibsh.still.data.SettingsRepository
import com.gotlibsh.still.widgets.homecontrols.GoogleHomeRepository
import com.gotlibsh.still.widgets.weather.WeatherRepository

/**
 * Tiny service locator. Everything the launcher needs lives here as a single instance,
 * so widgets can reach shared services with `LocalContext.current.stillApp()`.
 */
class StillApp : Application() {
    val settings by lazy { SettingsRepository(this) }
    val apps by lazy { AppRepository(this) }
    val weather by lazy { WeatherRepository() }
    val googleHome by lazy { GoogleHomeRepository(this) }
}

fun android.content.Context.stillApp(): StillApp = applicationContext as StillApp
