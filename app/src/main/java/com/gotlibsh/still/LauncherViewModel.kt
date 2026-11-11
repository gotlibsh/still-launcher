package com.gotlibsh.still

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gotlibsh.still.data.AppInfo
import com.gotlibsh.still.data.LauncherSettings
import com.gotlibsh.still.data.SettingsRepository
import com.gotlibsh.still.data.withCustomNames
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val still = application as StillApp
    val appRepository = still.apps

    /** null until settings have loaded once, so the home screen doesn't flash empty. */
    val settings: StateFlow<LauncherSettings?> =
        still.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Installed apps with your custom names applied. */
    val apps: StateFlow<List<AppInfo>> =
        combine(still.apps.apps, settings) { list, s -> list.withCustomNames(s?.appNames.orEmpty()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        still.apps.watchPackageChanges { refreshApps() }
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch { still.apps.refresh() }
    }

    /** "" when idle or connected; otherwise progress or the reason connecting failed. */
    val googleHomeStatus = MutableStateFlow("")

    /** True while Google's permission screen is on top, so the launcher keeps showing Settings. */
    @Volatile var awayForGoogleHome = false
        private set

    /** True while a system picker (e.g. contacts) opened from Settings is on top. */
    @Volatile var awayForPicker = false

    /** Runs Google's permission flow. Lives in viewModelScope so leaving Settings can't cancel it. */
    fun connectGoogleHome() {
        if (awayForGoogleHome) return
        awayForGoogleHome = true
        googleHomeStatus.value = "waiting for Google…"
        viewModelScope.launch {
            try {
                googleHomeStatus.value = still.googleHome.requestAccess().orEmpty()
            } finally {
                awayForGoogleHome = false
            }
        }
    }

    /** Writes all settings to the file the user picked. [done] gets true on success. */
    fun backupTo(uri: Uri, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                val json = still.settings.exportBackup()
                withContext(Dispatchers.IO) {
                    still.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
                }
            }.isSuccess
            done(ok)
        }
    }

    /** Replaces all settings with the picked backup file. [done] gets true on success. */
    fun restoreFrom(uri: Uri, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                val json = withContext(Dispatchers.IO) {
                    still.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }
                still.settings.importBackup(json)
            }.getOrDefault(false)
            done(ok)
        }
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

    fun rename(app: AppInfo, name: String) {
        // Typing the original name back counts as clearing it.
        val value = if (name.trim() == app.originalLabel) "" else name
        edit { setAppName(app.key, value) }
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
