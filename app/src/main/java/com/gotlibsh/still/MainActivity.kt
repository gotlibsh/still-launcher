package com.gotlibsh.still

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotlibsh.still.data.AppInfo
import com.gotlibsh.still.data.LauncherSettings
import com.gotlibsh.still.data.SettingsRepository
import com.gotlibsh.still.ui.AppDrawer
import com.gotlibsh.still.ui.HomeScreen
import com.gotlibsh.still.ui.SettingsScreen
import com.gotlibsh.still.ui.StillColors
import com.gotlibsh.still.ui.StillTheme
import com.gotlibsh.still.widgets.HomeWidget
import com.gotlibsh.still.widgets.WidgetHost
import com.gotlibsh.still.widgets.WidgetRegistry

private enum class Screen { Home, Drawer, Settings, Detail }

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()
    private var screen by mutableStateOf(Screen.Home)
    private var detailWidget by mutableStateOf<HomeWidget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Google Home's permission screen needs to be registered before the activity starts.
        stillApp().googleHome.registerForPermissions(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { StillTheme { Root() } }
    }

    /** Pressing the home button while in the drawer or settings goes back to home. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        screen = Screen.Home
    }

    override fun onResume() {
        super.onResume()
        vm.refreshApps()
    }

    override fun onStop() {
        super.onStop()
        // Come back to a clean home screen after using another app,
        // but not while Google Home's permission screen is covering Settings.
        if (!vm.awayForGoogleHome) screen = Screen.Home
    }

    @Composable
    private fun Root() {
        val settings by vm.settings.collectAsStateWithLifecycle()
        val apps by vm.apps.collectAsStateWithLifecycle()

        Box(Modifier.fillMaxSize().background(StillColors.background)) {
            val s = settings
            if (s != null) LauncherContent(s, apps)
        }
    }

    @Composable
    private fun LauncherContent(s: LauncherSettings, apps: List<AppInfo>) {
        val host = remember(s, apps) { ActivityWidgetHost(s, apps) }

        // A launcher never "exits" on back.
        BackHandler(enabled = screen == Screen.Home) {}

        Crossfade(targetState = screen, animationSpec = tween(150), label = "screen") { current ->
            when (current) {
                Screen.Home -> HomeScreen(
                    host = host,
                    widgets = WidgetRegistry.visible(s),
                    onOpenDrawer = { screen = Screen.Drawer },
                    onSwipeLeft = { launchSwipeApp(s.swipeLeftAppKey, apps, "left") },
                    onSwipeRight = { launchSwipeApp(s.swipeRightAppKey, apps, "right") },
                )
                Screen.Drawer -> AppDrawer(
                    apps = apps,
                    settings = s,
                    onLaunch = { app -> vm.appRepository.launch(app); screen = Screen.Home },
                    onClose = { screen = Screen.Home },
                    onToggleFavorite = vm::toggleFavorite,
                    onHide = vm::hide,
                    onRename = vm::rename,
                    onAppInfo = vm.appRepository::openAppInfo,
                    onUninstall = vm.appRepository::uninstall,
                )
                Screen.Settings -> SettingsScreen(
                    settings = s,
                    apps = apps,
                    vm = vm,
                    onClose = { screen = Screen.Home },
                )
                Screen.Detail -> {
                    val close = { screen = Screen.Home }
                    BackHandler(onBack = close)
                    detailWidget?.Detail(host, close)
                }
            }
        }
    }

    private fun launchSwipeApp(key: String, apps: List<AppInfo>, direction: String) {
        val app = apps.firstOrNull { it.key == key }
        if (app != null) {
            vm.appRepository.launch(app)
        } else {
            Toast.makeText(this, "Choose an app for swipe $direction in settings", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class ActivityWidgetHost(
        override val settings: LauncherSettings,
        override val apps: List<AppInfo>,
    ) : WidgetHost {
        override fun launch(app: AppInfo) = vm.appRepository.launch(app)
        override fun openAppInfo(app: AppInfo) = vm.appRepository.openAppInfo(app)
        override fun startIntent(intent: Intent) = vm.appRepository.startSafely(intent)
        override fun openSettings() { screen = Screen.Settings }
        override fun openDetail(widget: HomeWidget) {
            detailWidget = widget
            screen = Screen.Detail
        }
        override fun editSettings(block: suspend SettingsRepository.() -> Unit) = vm.edit(block)
    }
}
