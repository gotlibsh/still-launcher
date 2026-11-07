package com.gotlibsh.still.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gotlibsh.still.LauncherViewModel
import com.gotlibsh.still.data.AppInfo
import com.gotlibsh.still.data.LauncherSettings
import com.gotlibsh.still.data.SettingsCodec
import com.gotlibsh.still.data.WeatherPlace
import com.gotlibsh.still.stillApp
import com.gotlibsh.still.widgets.WidgetRegistry
import com.gotlibsh.still.widgets.homecontrols.HomeControlsWidget
import com.gotlibsh.still.widgets.homecontrols.HomeSwitch
import com.google.home.PermissionsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

private enum class Picker { Favorite, NotesApp, SwipeLeft, SwipeRight }

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    apps: List<AppInfo>,
    vm: LauncherViewModel,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var picker by remember { mutableStateOf<Picker?>(null) }
    val appsByKey = remember(apps) { apps.associateBy { it.key } }
    val widgets = WidgetRegistry.ordered(settings)
    val widgetIds = widgets.map { it.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 28.dp),
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Text("‹ back", color = StillColors.dim, fontSize = 16.sp, modifier = Modifier.quietClickable(onClick = onClose))
            Spacer(Modifier.height(12.dp))
            Text("Settings", color = StillColors.text, fontSize = 32.sp)
        }

        // Home screen widgets
        item { Section("Home screen") }
        items(widgets, key = { "w-" + it.id }) { w ->
            OrderRow(
                label = w.title,
                trailing = if (WidgetRegistry.isEnabled(w, settings)) "on" else "off",
                onClick = { vm.edit { setDisabledWidgets(WidgetRegistry.toggled(w, settings)) } },
                onUp = { vm.edit { setWidgetOrder(SettingsCodec.move(widgetIds, w.id, -1)) } },
                onDown = { vm.edit { setWidgetOrder(SettingsCodec.move(widgetIds, w.id, +1)) } },
            )
        }

        // Favorites
        item { Section("Favorite apps (${settings.favorites.size}/${LauncherSettings.MAX_FAVORITES})") }
        items(settings.favorites, key = { "f-$it" }) { key ->
            OrderRow(
                label = appsByKey[key]?.label ?: "(not installed)",
                trailing = "remove",
                onClick = { vm.edit { setFavorites(settings.favorites - key) } },
                onUp = { vm.edit { setFavorites(SettingsCodec.move(settings.favorites, key, -1)) } },
                onDown = { vm.edit { setFavorites(SettingsCodec.move(settings.favorites, key, +1)) } },
            )
        }
        if (settings.favorites.size < LauncherSettings.MAX_FAVORITES) {
            item { SettingRow("+ add app") { picker = Picker.Favorite } }
        }

        // Weather
        item { Section("Weather") }
        item { WeatherSettings(settings, vm) }

        // Notes
        item { Section("Notes link") }
        item { NotesSettings(settings, appsByKey, vm, onPickApp = { picker = Picker.NotesApp }) }

        // Google Home
        item { Section("Google Home") }
        item { GoogleHomeSettings(settings, vm) }

        // Gestures
        item { Section("Swipe gestures") }
        item { SwipeSetting("Swipe left opens", settings.swipeLeftAppKey, appsByKey, onPick = { picker = Picker.SwipeLeft }, onClear = { vm.edit { setSwipeLeftApp("") } }) }
        item { SwipeSetting("Swipe right opens", settings.swipeRightAppKey, appsByKey, onPick = { picker = Picker.SwipeRight }, onClear = { vm.edit { setSwipeRightApp("") } }) }

        // Clock & drawer
        item { Section("Behaviour") }
        item {
            val label = when (settings.use24Hour) { null -> "phone default"; true -> "24-hour"; false -> "12-hour" }
            SettingRow("Clock format", label) {
                val next = when (settings.use24Hour) { null -> true; true -> false; false -> null }
                vm.edit { setUse24Hour(next) }
            }
        }
        item {
            SettingRow("Open app when search has one match", if (settings.autoLaunchSingleMatch) "on" else "off") {
                vm.edit { setAutoLaunch(!settings.autoLaunchSingleMatch) }
            }
        }

        // Hidden apps
        if (settings.hiddenApps.isNotEmpty()) {
            item { Section("Hidden apps (tap to show again)") }
            items(settings.hiddenApps.toList(), key = { "h-$it" }) { key ->
                SettingRow(appsByKey[key]?.label ?: "(not installed)", "show") { vm.unhide(key) }
            }
        }

        // System
        item { Section("Phone") }
        item { SettingRow("Set Still as the home app") { vm.appRepository.startSafely(Intent(Settings.ACTION_HOME_SETTINGS)) } }
        item { SettingRow("Phone settings") { vm.appRepository.startSafely(Intent(Settings.ACTION_SETTINGS)) } }
        item { Spacer(Modifier.height(48.dp)) }
    }

    when (picker) {
        Picker.Favorite -> AppPickerDialog(
            title = "Add to home",
            apps = apps.filter { it.key !in settings.favorites },
            onPick = { app -> picker = null; vm.edit { setFavorites(settings.favorites + app.key) } },
            onDismiss = { picker = null },
        )
        Picker.NotesApp -> AppPickerDialog(
            title = "Open this app for notes",
            apps = apps,
            onPick = { app -> picker = null; vm.edit { setNotesApp(app.key) } },
            onDismiss = { picker = null },
        )
        Picker.SwipeLeft -> AppPickerDialog(
            title = "Swipe left opens",
            apps = apps,
            onPick = { app -> picker = null; vm.edit { setSwipeLeftApp(app.key) } },
            onDismiss = { picker = null },
        )
        Picker.SwipeRight -> AppPickerDialog(
            title = "Swipe right opens",
            apps = apps,
            onPick = { app -> picker = null; vm.edit { setSwipeRightApp(app.key) } },
            onDismiss = { picker = null },
        )
        null -> Unit
    }
}

@Composable
private fun GoogleHomeSettings(settings: LauncherSettings, vm: LauncherViewModel) {
    val repo = LocalContext.current.stillApp().googleHome
    val scope = rememberCoroutineScope()
    val permission by remember(repo) { repo.permissionState }
        .collectAsState(initial = PermissionsState.PERMISSIONS_STATE_UNINITIALIZED)
    val connected = permission == PermissionsState.GRANTED
    var picking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    Column {
        Text(
            "Show a few Google Home devices (AC, boiler, lights…) as on/off switches on the home screen.",
            color = StillColors.faint,
            fontSize = 14.sp,
        )
        when (permission) {
            PermissionsState.GRANTED -> SettingRow("Google Home", "connected · change access") {
                scope.launch { repo.requestAccess() }
            }
            PermissionsState.PERMISSIONS_STATE_UNINITIALIZED -> SettingRow("Google Home", "checking…")
            else -> SettingRow("Connect Google Home") {
                status = "waiting for Google…"
                scope.launch { status = if (repo.requestAccess()) "" else "Not connected. See the README for the one-time Google setup." }
            }
        }
        if (status.isNotEmpty()) Text(status, color = StillColors.faint, fontSize = 14.sp)

        if (connected) {
            val switches by remember(repo) { repo.switches() }.collectAsState(initial = emptyList())
            val ids = settings.homeDeviceIds
            ids.forEach { id ->
                OrderRow(
                    label = switches.firstOrNull { it.id == id }?.name ?: "(device not found)",
                    trailing = "remove",
                    onClick = { vm.edit { setHomeDevices(ids - id) } },
                    onUp = { vm.edit { setHomeDevices(SettingsCodec.move(ids, id, -1)) } },
                    onDown = { vm.edit { setHomeDevices(SettingsCodec.move(ids, id, +1)) } },
                )
            }
            SettingRow("+ add device") { picking = true }
            if (picking) {
                HomeDevicePickerDialog(
                    devices = switches.filter { it.id !in ids },
                    onPick = { device ->
                        picking = false
                        vm.edit {
                            setHomeDevices(ids + device.id)
                            // Adding the first device switches the widget on.
                            if (!WidgetRegistry.isEnabled(HomeControlsWidget, settings)) {
                                setDisabledWidgets(WidgetRegistry.toggled(HomeControlsWidget, settings))
                            }
                        }
                    },
                    onDismiss = { picking = false },
                )
            }
        }
    }
}

@Composable
private fun HomeDevicePickerDialog(
    devices: List<HomeSwitch>,
    onPick: (HomeSwitch) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .background(StillColors.background)
                .padding(24.dp),
        ) {
            Text("Add a Google Home device", fontSize = 14.sp, color = StillColors.dim)
            if (devices.isEmpty()) {
                Text(
                    "No more on/off devices found in your Google Home.",
                    fontSize = 16.sp,
                    color = StillColors.faint,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                items(devices, key = { it.id }) { device ->
                    Text(
                        text = device.name,
                        fontSize = 18.sp,
                        color = StillColors.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .quietClickable { onPick(device) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeSetting(
    label: String,
    appKey: String,
    appsByKey: Map<String, AppInfo>,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val appLabel = appKey.takeIf { it.isNotBlank() }?.let { appsByKey[it]?.label ?: "(not installed)" }
    Column {
        SettingRow(label, appLabel ?: "choose", onClick = onPick)
        if (appLabel != null) {
            Text(
                "clear",
                color = StillColors.faint,
                fontSize = 14.sp,
                modifier = Modifier.quietClickable(onClick = onClear).padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun WeatherSettings(settings: LauncherSettings, vm: LauncherViewModel) {
    val repo = LocalContext.current.stillApp().weather
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherPlace>>(emptyList()) }
    var status by remember { mutableStateOf("") }

    fun search() {
        status = "searching…"
        scope.launch {
            results = repo.searchPlaces(query)
            status = if (results.isEmpty()) "no places found" else ""
        }
    }

    Column {
        SettingRow("City", settings.weatherPlace?.name ?: "not set")
        StillTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "type a city and press enter",
            onImeAction = ::search,
            modifier = Modifier.fillMaxWidth(),
        )
        if (status.isNotEmpty()) Text(status, color = StillColors.faint, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        results.forEach { place ->
            SettingRow(place.name) {
                vm.edit { setWeatherPlace(place) }
                results = emptyList()
                query = ""
            }
        }
        SettingRow("Units", if (settings.fahrenheit) "°F" else "°C") { vm.edit { setFahrenheit(!settings.fahrenheit) } }
        if (settings.weatherPlace != null) SettingRow("Remove city") { vm.edit { setWeatherPlace(null) } }
    }
}

@Composable
private fun NotesSettings(
    settings: LauncherSettings,
    appsByKey: Map<String, AppInfo>,
    vm: LauncherViewModel,
    onPickApp: () -> Unit,
) {
    var url by remember(settings.notesUrl) { mutableStateOf(settings.notesUrl) }
    Column {
        Text(
            "Adds an \"open notes →\" link under your note. Pick an app, or paste a link " +
                "(https://…, obsidian://…, notion://…). The app wins if both are set.",
            color = StillColors.faint,
            fontSize = 14.sp,
        )
        val appLabel = settings.notesAppKey.takeIf { it.isNotBlank() }?.let { appsByKey[it]?.label ?: "(not installed)" }
        SettingRow("Notes app", appLabel ?: "choose", onClick = onPickApp)
        if (appLabel != null) SettingRow("Clear notes app") { vm.edit { setNotesApp("") } }
        StillTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = "or paste a notes link, then press enter",
            onImeAction = { vm.edit { setNotesUrl(url) } },
            modifier = Modifier.fillMaxWidth(),
        )
        if (url != settings.notesUrl) SettingRow("save link") { vm.edit { setNotesUrl(url) } }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        color = StillColors.dim,
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 32.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingRow(label: String, value: String? = null, onClick: (() -> Unit)? = null) {
    val base = Modifier.fillMaxWidth()
    Row(
        modifier = (if (onClick != null) base.quietClickable(onClick = onClick) else base).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = StillColors.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (value != null) Text(value, color = StillColors.dim, fontSize = 15.sp)
    }
}

@Composable
private fun OrderRow(label: String, trailing: String, onClick: () -> Unit, onUp: () -> Unit, onDown: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = StillColors.text, fontSize = 17.sp, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
        Arrow("↑", onUp)
        Arrow("↓", onDown)
        Spacer(Modifier.width(8.dp))
        Text(trailing, color = StillColors.dim, fontSize = 15.sp, modifier = Modifier.quietClickable(onClick = onClick).padding(8.dp))
    }
}

@Composable
private fun Arrow(glyph: String, onClick: () -> Unit) {
    Text(glyph, color = StillColors.dim, fontSize = 18.sp, modifier = Modifier.quietClickable(onClick = onClick).padding(8.dp))
}
