package com.gotlibsh.still.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Lists installed apps (including work-profile apps) and launches them. */
class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private var callbackRegistered = false

    /** Re-reads the app list whenever something is installed, removed or updated. */
    fun watchPackageChanges(onChange: () -> Unit) {
        if (callbackRegistered) return
        callbackRegistered = true
        launcherApps.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackagesAvailable(p: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChange()
            override fun onPackagesUnavailable(p: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChange()
        })
    }

    suspend fun refresh() {
        val list = withContext(Dispatchers.IO) {
            val me = Process.myUserHandle()
            launcherApps.profiles.flatMap { user ->
                val serial = userManager.getSerialNumberForUser(user)
                launcherApps.getActivityList(null, user)
                    .filter { it.componentName.packageName != context.packageName }
                    .map { info ->
                        AppInfo(
                            label = info.label.toString().trim(),
                            component = info.componentName,
                            user = user,
                            userSerial = serial,
                            isWorkProfile = user != me,
                        )
                    }
            }.sortedBy { it.label.lowercase() }
        }
        _apps.value = list
    }

    fun launch(app: AppInfo) {
        try {
            launcherApps.startMainActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            Toast.makeText(context, "Couldn't open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppInfo(app: AppInfo) {
        try {
            launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            startSafely(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.component.packageName}"))
            )
        }
    }

    fun uninstall(app: AppInfo) {
        startSafely(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.component.packageName}")))
    }

    /** Starts any intent from the launcher without crashing if nothing can handle it. */
    fun startSafely(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found for that", Toast.LENGTH_SHORT).show()
        false
    } catch (e: SecurityException) {
        Toast.makeText(context, "Not allowed to open that", Toast.LENGTH_SHORT).show()
        false
    }
}
