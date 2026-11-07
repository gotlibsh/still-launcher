package com.gotlibsh.still.data

import android.content.ComponentName
import android.os.UserHandle

/**
 * One launchable activity. [key] is a stable string used to remember favorites and hidden
 * apps across restarts: "package/activity/userSerial".
 */
data class AppInfo(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val userSerial: Long,
    val isWorkProfile: Boolean,
    /** The app's own name; [label] differs from it when you've renamed the app. */
    val originalLabel: String = label,
) {
    val key: String get() = "${component.packageName}/${component.className}/$userSerial"
    val isRenamed: Boolean get() = label != originalLabel

    /** Search finds an app by your name for it or by its own name. */
    val searchLabels: List<String> get() = if (isRenamed) listOf(label, originalLabel) else listOf(label)
}

/** Applies your custom names ([LauncherSettings.appNames]) and re-sorts by the shown name. */
fun List<AppInfo>.withCustomNames(names: Map<String, String>): List<AppInfo> =
    if (names.isEmpty()) this
    else map { app -> names[app.key]?.let { app.copy(label = it, originalLabel = app.originalLabel) } ?: app }
        .sortedBy { it.label.lowercase() }
