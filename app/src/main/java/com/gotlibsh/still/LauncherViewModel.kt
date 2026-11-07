package com.gotlibsh.still

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gotlibsh.still.data.AppInfo
import com.gotlibsh.still.data.LauncherSettings
import com.gotlibsh.still.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val still = application as StillApp
    val appRepository = still.apps

    /** null until settings have loaded once, so the home screen doesn't flash empty. */
    val settings: StateFlow<LauncherSettings?> =
        still.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val apps: StateFlow<List<AppInfo>> = still.apps.apps

    init {
        still.apps.watchPackageChanges { refreshApps() }
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch { still.apps.refresh() }
    }

    fun edit(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { still.settings.block() }
    }

    private val current get() = settings.value ?: LauncherSettings()

    fun isFavorite(app: AppInfo) = app.key in current.favorites

    fun toggleFavorite(app: AppInfo) {
        val favs = current.favorites
        val updated = if (app.key in favs) favs - app.key else favs + app.key
        edit { setFavorites(updated) }
    }

    fun hide(app: AppInfo) {
        val s = current
        edit {
            setHidden(s.hiddenApps + app.key)
            if (app.key in s.favorites) setFavorites(s.favorites - app.key)
        }
    }

    fun unhide(key: String) {
        val hidden = current.hiddenApps
        edit { setHidden(hidden - key) }
    }
}
