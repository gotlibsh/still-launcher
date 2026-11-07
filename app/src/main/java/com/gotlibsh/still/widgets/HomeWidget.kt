package com.gotlibsh.still.widgets

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.gotlibsh.still.data.AppInfo
import com.gotlibsh.still.data.LauncherSettings
import com.gotlibsh.still.data.SettingsRepository

/**
 * A block on the home screen. To add a new one:
 *  1. Create an `object MyWidget : HomeWidget` with a unique [id].
 *  2. Add it to [WidgetRegistry.all].
 * It then shows up in Settings > Home screen, where it can be switched on/off and reordered.
 */
interface HomeWidget {
    /** Stable id, saved in settings. Never change it once released. */
    val id: String

    /** Name shown in settings. */
    val title: String

    /** If true, the widget takes the leftover vertical space (used by the app list). */
    val fillsSpace: Boolean get() = false

    /** Whether the widget is on for new installs. */
    val enabledByDefault: Boolean get() = true

    @Composable
    fun Content(host: WidgetHost)
}

/** What a widget can see and do. Passed to every [HomeWidget.Content]. */
@Stable
interface WidgetHost {
    val settings: LauncherSettings
    val apps: List<AppInfo>

    fun findApp(key: String): AppInfo? = apps.firstOrNull { it.key == key }
    fun launch(app: AppInfo)
    fun openAppInfo(app: AppInfo)

    /** Opens any intent; shows a short message instead of crashing if nothing handles it. */
    fun startIntent(intent: Intent): Boolean
    fun openSettings()

    /** Changes settings in the background, e.g. `host.editSettings { setNoteText("hi") }`. */
    fun editSettings(block: suspend SettingsRepository.() -> Unit)
}
