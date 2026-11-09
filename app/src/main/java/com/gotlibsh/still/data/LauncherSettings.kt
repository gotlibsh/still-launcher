package com.gotlibsh.still.data

/** Everything the user can configure. Plain Kotlin so it's easy to test and extend. */
data class LauncherSettings(
    val favorites: List<String> = emptyList(),
    val hiddenApps: Set<String> = emptySet(),
    /** Widget ids in display order. Unknown ids are dropped, new widgets are appended. */
    val widgetOrder: List<String> = emptyList(),
    val disabledWidgets: Set<String> = emptySet(),
    /** null = follow the phone's 12/24-hour setting. */
    val use24Hour: Boolean? = null,
    /** Open the app straight away when a drawer search leaves exactly one match. */
    val autoLaunchSingleMatch: Boolean = true,
    /** Main weather city, shown on the home screen first. */
    val weatherPlace: WeatherPlace? = null,
    /** More cities to flip through. */
    val extraWeatherPlaces: List<WeatherPlace> = emptyList(),
    val fahrenheit: Boolean = false,
    val noteText: String = "",
    /** A link that opens your real notes, e.g. https://..., obsidian://open?vault=..., notion://... */
    val notesUrl: String = "",
    /** Or an app to open for notes (an [AppInfo.key]). Takes priority over [notesUrl]. */
    val notesAppKey: String = "",
    /** Apps opened by swiping left / right on the home screen ([AppInfo.key], empty = none). */
    val swipeLeftAppKey: String = "",
    val swipeRightAppKey: String = "",
    /** Google Home device ids shown as switches by the Home switches widget, in order. */
    val homeDeviceIds: List<String> = emptyList(),
    /** Your own names for apps: [AppInfo.key] -> name. */
    val appNames: Map<String, String> = emptyMap(),
    /** Short names for Google Home switches: device id -> e.g. "LR". Missing = initials. */
    val homeDeviceLabels: Map<String, String> = emptyMap(),
    /** Icon picked by hand for a switch: device id -> SwitchIcon id. Missing = automatic. */
    val homeDeviceIcons: Map<String, String> = emptyMap(),
    /** Triple tap on empty home space opens a WhatsApp chat with this number (digits only). */
    val tripleTapPhone: String = "",
    val tripleTapName: String = "",
    /** A [Background] id. */
    val background: String = Background.DEFAULT.id,
    /** How much to darken the phone's wallpaper, 0-80 (percent). */
    val wallpaperDim: Int = 40,
) {
    companion object {
        const val MAX_FAVORITES = 8
    }
}

/**
 * Home background: a calm solid color, or the phone's own wallpaper with a dark layer on top.
 * All choices are dark so the light text always stays readable.
 */
enum class Background(val id: String, val title: String, val argb: Long?) {
    BLACK("black", "black", 0xFF000000),
    CHARCOAL("charcoal", "warm charcoal", 0xFF1C1A18),
    SLATE("slate", "deep slate", 0xFF161B22),
    NAVY("navy", "midnight blue", 0xFF0D1426),
    FOREST("forest", "dark forest", 0xFF0F1A14),
    PLUM("plum", "dusk plum", 0xFF1A1220),
    WALLPAPER("wallpaper", "phone wallpaper", null);

    companion object {
        val DEFAULT = BLACK
        val DIM_STEPS = listOf(0, 20, 40, 60, 80)
        fun of(id: String?): Background = entries.firstOrNull { it.id == id } ?: DEFAULT
        fun next(current: Background): Background = entries[(current.ordinal + 1) % entries.size]
        fun nextDim(current: Int): Int = DIM_STEPS.firstOrNull { it > current } ?: DIM_STEPS.first()
    }
}

data class WeatherPlace(val name: String, val latitude: Double, val longitude: Double)

/** Main city first, then the extra ones. */
val LauncherSettings.allWeatherPlaces: List<WeatherPlace>
    get() = listOfNotNull(weatherPlace) + extraWeatherPlaces

object SettingsCodec {
    private const val SEP = "\n"

    fun encodeList(items: Collection<String>): String = items.filter { it.isNotBlank() }.joinToString(SEP)

    fun decodeList(raw: String?): List<String> =
        raw.orEmpty().split(SEP).map { it.trim() }.filter { it.isNotEmpty() }

    /** Names map as lines of "key<US>name". Names lose any line breaks. */
    fun encodeNames(names: Map<String, String>): String =
        encodeList(names.mapNotNull { (key, name) -> cleanName(name).takeIf { it.isNotEmpty() }?.let { "$key$US$it" } })

    fun decodeNames(raw: String?): Map<String, String> =
        decodeList(raw).mapNotNull { line ->
            val key = line.substringBefore(US, "")
            val name = line.substringAfter(US, "").trim()
            if (key.isEmpty() || name.isEmpty()) null else key to name
        }.toMap()

    fun cleanName(name: String): String = name.replace(Regex("[\\r\\n$US]+"), " ").trim()

    private const val US = "\u001F"

    /** "AC Living Room" -> "ALR", "Boiler" -> "B": first letters of the words, at most 3. */
    fun initials(name: String): String =
        name.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
            .joinToString("") { it.take(1).uppercase() }.take(3).ifEmpty { "?" }

    fun encodePlace(place: WeatherPlace?): String =
        if (place == null) "" else "${place.latitude}|${place.longitude}|${place.name}"

    fun decodePlace(raw: String?): WeatherPlace? {
        val parts = raw.orEmpty().split("|", limit = 3)
        if (parts.size != 3) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lon = parts[1].toDoubleOrNull() ?: return null
        return WeatherPlace(parts[2], lat, lon)
    }

    /** Saved order first (only ids that still exist), then any widgets added since. */
    fun reconcileOrder(saved: List<String>, available: List<String>): List<String> {
        val known = saved.filter { it in available }.distinct()
        return known + available.filter { it !in known }
    }

    /** Moves [id] one step up (delta = -1) or down (delta = +1). */
    fun move(list: List<String>, id: String, delta: Int): List<String> {
        val from = list.indexOf(id)
        if (from < 0) return list
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (to == from) return list
        return list.toMutableList().apply { add(to, removeAt(from)) }
    }
}
