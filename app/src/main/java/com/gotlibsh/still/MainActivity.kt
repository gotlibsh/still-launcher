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
import com.gotlibsh.still.data.WhatsApp
import com.gotlibsh.still.ui.AppDrawer
import com.gotlibsh.still.ui.HomeScreen
import com.gotlibsh.still.ui.SettingsScreen
import com.gotlibsh.still.ui.StillColors
import com.gotlibsh.still.ui.StillTheme
import com.gotlibsh.still.data.Background
import androidx.compose.ui.graphics.Color as ComposeColor
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
        // but not while Google Home's permission screen or a picker (contacts) is covering
        // Settings and will hand a result back to it.
        if (vm.awayForGoogleHome || vm.awayForPicker) {
            vm.awayForPicker = false
            return
        }
        screen = Screen.Home
    }

    @Composable
    private fun Root() {
        val settings by vm.settings.collectAsStateWithLifecycle()
        val apps by vm.apps.collectAsStateWithLifecycle()

        val s = settings
        // Until settings load, stay black rather than flashing the wallpaper.
        val fill = if (s == null) StillColors.background else backgroundFill(s, onHome = screen == Screen.Home)
        Box(Modifier.fillMaxSize().background(fill)) {
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
                    onSwipeLeft = { launchSwipeApp(s.swipeLeftAppKey, apps) },
                    onSwipeRight = { launchSwipeApp(s.swipeRightAppKey, apps) },
                    onTripleTap = { openTripleTapChat(s) },
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

    /**
     * Solid color, or a see-through dark layer over the phone's wallpaper. Off the home screen
     * (drawer, settings, forecast) the wallpaper is dimmed at least 70% so lists stay readable.
     */
    private fun backgroundFill(s: LauncherSettings, onHome: Boolean): ComposeColor {
        val bg = Background.of(s.background)
        bg.argb?.let { return ComposeColor(it) }
        val dim = if (onHome) s.wallpaperDim else maxOf(s.wallpaperDim, 70)
        return ComposeColor.Black.copy(alpha = dim / 100f)
    }

    private fun openTripleTapChat(s: LauncherSettings) {
        // Nothing chosen: stay quiet, an accidental triple tap shouldn't nag.
        if (s.tripleTapPhone.isBlank()) return
        if (!WhatsApp.openChat(this, s.tripleTapPhone)) {
            Toast.makeText(this, "WhatsApp isn't installed", Toast.LENGTH_SHORT).show()
        }
    }

    /** Opens the app chosen for a swipe. No app chosen (or it was uninstalled): do nothing. */
    private fun launchSwipeApp(key: String, apps: List<AppInfo>) {
        apps.firstOrNull { it.key == key }?.let(vm.appRepository::launch)
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
