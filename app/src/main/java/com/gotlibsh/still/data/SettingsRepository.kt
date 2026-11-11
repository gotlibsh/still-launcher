package com.gotlibsh.still.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "still_settings")

/** Stores [LauncherSettings] in DataStore. Add a key here when you add a setting. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val favorites = stringPreferencesKey("favorites")
        val hidden = stringPreferencesKey("hidden_apps")
        val widgetOrder = stringPreferencesKey("widget_order")
        val disabledWidgets = stringPreferencesKey("disabled_widgets")
        val clockFormat = stringPreferencesKey("clock_format") // "", "12", "24"
        val autoLaunch = booleanPreferencesKey("auto_launch")
        val weatherPlace = stringPreferencesKey("weather_place")
        val extraWeatherPlaces = stringPreferencesKey("weather_places_extra")
        val fahrenheit = booleanPreferencesKey("fahrenheit")
        val noteText = stringPreferencesKey("note_text")
        val notesUrl = stringPreferencesKey("notes_url")
        val notesApp = stringPreferencesKey("notes_app")
        val swipeLeftApp = stringPreferencesKey("swipe_left_app")
        val swipeRightApp = stringPreferencesKey("swipe_right_app")
        val homeDevices = stringPreferencesKey("home_device_ids")
        val appNames = stringPreferencesKey("app_names")
        val homeDeviceLabels = stringPreferencesKey("home_device_labels")
        val homeDeviceIcons = stringPreferencesKey("home_device_icons")
        val tripleTapPhone = stringPreferencesKey("triple_tap_phone")
        val recentApps = stringPreferencesKey("recent_apps")
        val recentCount = intPreferencesKey("recent_count")
        val tripleTapName = stringPreferencesKey("triple_tap_name")
        val background = stringPreferencesKey("background")
        val wallpaperDim = intPreferencesKey("wallpaper_dim")
    }

    val settings: Flow<LauncherSettings> = context.dataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings() = LauncherSettings(
        favorites = SettingsCodec.decodeList(this[Keys.favorites]),
        hiddenApps = SettingsCodec.decodeList(this[Keys.hidden]).toSet(),
        widgetOrder = SettingsCodec.decodeList(this[Keys.widgetOrder]),
        disabledWidgets = SettingsCodec.decodeList(this[Keys.disabledWidgets]).toSet(),
        use24Hour = when (this[Keys.clockFormat]) {
            "24" -> true
            "12" -> false
            else -> null
        },
        autoLaunchSingleMatch = this[Keys.autoLaunch] ?: true,
        weatherPlace = SettingsCodec.decodePlace(this[Keys.weatherPlace]),
        extraWeatherPlaces = SettingsCodec.decodeList(this[Keys.extraWeatherPlaces]).mapNotNull(SettingsCodec::decodePlace),
        fahrenheit = this[Keys.fahrenheit] ?: false,
        noteText = this[Keys.noteText].orEmpty(),
        notesUrl = this[Keys.notesUrl].orEmpty(),
        notesAppKey = this[Keys.notesApp].orEmpty(),
        swipeLeftAppKey = this[Keys.swipeLeftApp].orEmpty(),
        swipeRightAppKey = this[Keys.swipeRightApp].orEmpty(),
        homeDeviceIds = SettingsCodec.decodeList(this[Keys.homeDevices]),
        appNames = SettingsCodec.decodeNames(this[Keys.appNames]),
        homeDeviceLabels = SettingsCodec.decodeNames(this[Keys.homeDeviceLabels]),
        homeDeviceIcons = SettingsCodec.decodeNames(this[Keys.homeDeviceIcons]),
        tripleTapPhone = this[Keys.tripleTapPhone].orEmpty(),
        recentApps = SettingsCodec.decodeList(this[Keys.recentApps]),
        recentCount = (this[Keys.recentCount] ?: 3).coerceIn(0, 5),
        tripleTapName = this[Keys.tripleTapName].orEmpty(),
        background = Background.of(this[Keys.background]).id,
        wallpaperDim = (this[Keys.wallpaperDim] ?: 40).coerceIn(0, 80),
    )

    suspend fun setFavorites(keys: List<String>) = put(Keys.favorites, SettingsCodec.encodeList(keys.take(LauncherSettings.MAX_FAVORITES)))
    suspend fun setHidden(keys: Set<String>) = put(Keys.hidden, SettingsCodec.encodeList(keys))
    suspend fun setWidgetOrder(ids: List<String>) = put(Keys.widgetOrder, SettingsCodec.encodeList(ids))
    suspend fun setDisabledWidgets(ids: Set<String>) = put(Keys.disabledWidgets, SettingsCodec.encodeList(ids))
    suspend fun setUse24Hour(value: Boolean?) = put(Keys.clockFormat, when (value) { true -> "24"; false -> "12"; null -> "" })
    suspend fun setAutoLaunch(value: Boolean) = context.dataStore.edit { it[Keys.autoLaunch] = value }
    suspend fun setWeatherPlace(place: WeatherPlace?) = put(Keys.weatherPlace, SettingsCodec.encodePlace(place))
    suspend fun setExtraWeatherPlaces(places: List<WeatherPlace>) =
        put(Keys.extraWeatherPlaces, SettingsCodec.encodeList(places.map(SettingsCodec::encodePlace)))
    suspend fun setFahrenheit(value: Boolean) = context.dataStore.edit { it[Keys.fahrenheit] = value }
    suspend fun setNoteText(text: String) = put(Keys.noteText, text)
    suspend fun setNotesUrl(url: String) = put(Keys.notesUrl, url.trim())
    suspend fun setNotesApp(key: String) = put(Keys.notesApp, key)
    suspend fun setSwipeLeftApp(key: String) = put(Keys.swipeLeftApp, key)
    suspend fun setSwipeRightApp(key: String) = put(Keys.swipeRightApp, key)
    suspend fun setHomeDevices(ids: List<String>) = put(Keys.homeDevices, SettingsCodec.encodeList(ids))

    suspend fun setBackground(background: Background) = put(Keys.background, background.id)
    suspend fun setWallpaperDim(percent: Int) = context.dataStore.edit { it[Keys.wallpaperDim] = percent.coerceIn(0, 80) }

    /** Remembers that [appKey] was just opened from the drawer. */
    suspend fun noteRecentApp(appKey: String) {
        context.dataStore.edit { prefs ->
            val recent = SettingsCodec.pushRecent(SettingsCodec.decodeList(prefs[Keys.recentApps]), appKey)
            prefs[Keys.recentApps] = SettingsCodec.encodeList(recent)
        }
    }

    suspend fun setRecentCount(count: Int) = context.dataStore.edit { it[Keys.recentCount] = count.coerceIn(0, 5) }

    suspend fun setTripleTapContact(name: String, phoneDigits: String) {
        context.dataStore.edit {
            it[Keys.tripleTapName] = name.trim()
            it[Keys.tripleTapPhone] = phoneDigits
        }
    }

    suspend fun setHomeDeviceIcon(deviceId: String, iconId: String) {
        context.dataStore.edit { prefs ->
            val icons = SettingsCodec.decodeNames(prefs[Keys.homeDeviceIcons]).toMutableMap()
            icons[deviceId] = iconId
            prefs[Keys.homeDeviceIcons] = SettingsCodec.encodeNames(icons)
        }
    }

    /** A blank label goes back to the automatic short word. */
    suspend fun setHomeDeviceLabel(deviceId: String, label: String) {
        context.dataStore.edit { prefs ->
            val labels = SettingsCodec.decodeNames(prefs[Keys.homeDeviceLabels]).toMutableMap()
            val clean = SettingsCodec.cleanName(label)
            if (clean.isEmpty()) labels.remove(deviceId) else labels[deviceId] = clean
            prefs[Keys.homeDeviceLabels] = SettingsCodec.encodeNames(labels)
        }
    }

    /** A blank name goes back to the app's own name. */
    suspend fun setAppName(appKey: String, name: String) {
        context.dataStore.edit { prefs ->
            val names = SettingsCodec.decodeNames(prefs[Keys.appNames]).toMutableMap()
            val clean = SettingsCodec.cleanName(name)
            if (clean.isEmpty()) names.remove(appKey) else names[appKey] = clean
            prefs[Keys.appNames] = SettingsCodec.encodeNames(names)
        }
    }

    /** Every saved setting as a JSON backup file's text. */
    suspend fun exportBackup(): String =
        BackupCodec.encode(context.dataStore.data.first().asMap().mapKeys { it.key.name })

    /** Replaces all settings with the ones in a backup. Returns false if it isn't a Still backup. */
    suspend fun importBackup(json: String): Boolean {
        val values = BackupCodec.decode(json) ?: return false
        context.dataStore.edit { prefs ->
            prefs.clear()
            values.forEach { (key, value) ->
                when (value) {
                    is String -> prefs[stringPreferencesKey(key)] = value
                    is Boolean -> prefs[booleanPreferencesKey(key)] = value
                    is Int -> prefs[intPreferencesKey(key)] = value
                }
            }
        }
        return true
    }

    private suspend fun put(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }
}
