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
    val weatherPlace: WeatherPlace? = null,
    val fahrenheit: Boolean = false,
    val noteText: String = "",
    /** A link that opens your real notes, e.g. https://..., obsidian://open?vault=..., notion://... */
    val notesUrl: String = "",
    /** Or an app to open for notes (an [AppInfo.key]). Takes priority over [notesUrl]. */
    val notesAppKey: String = "",
) {
    companion object {
        const val MAX_FAVORITES = 8
    }
}

data class WeatherPlace(val name: String, val latitude: Double, val longitude: Double)

object SettingsCodec {
    private const val SEP = "\n"

    fun encodeList(items: Collection<String>): String = items.filter { it.isNotBlank() }.joinToString(SEP)

    fun decodeList(raw: String?): List<String> =
        raw.orEmpty().split(SEP).map { it.trim() }.filter { it.isNotEmpty() }

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
