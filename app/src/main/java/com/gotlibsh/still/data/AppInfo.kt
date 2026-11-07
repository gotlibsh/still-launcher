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
) {
    val key: String get() = "${component.packageName}/${component.className}/$userSerial"
}
