package com.gotlibsh.still.widgets.homecontrols

/** The kind of icon shown next to a Google Home switch. [id] is what settings store. */
enum class SwitchIcon(val id: String, val title: String) {
    COOL("cool", "snowflake"),
    HEAT("heat", "flame"),
    LIGHT("light", "bulb"),
    AIR("air", "fan"),
    TV("tv", "tv"),
    PLUG("plug", "plug"),
    POWER("power", "power");

    companion object {
        fun of(id: String?): SwitchIcon? = entries.firstOrNull { it.id == id }
        fun next(current: SwitchIcon): SwitchIcon = entries[(current.ordinal + 1) % entries.size]
    }
}

/**
 * Picks an icon and a short word for a switch from its Google Home name and device types,
 * e.g. "AC Living Room" -> snowflake + "living", "Boiler" -> flame + "boiler".
 * Pure Kotlin so it can be unit tested.
 */
object SwitchLabels {

    /** Words in a device name that say what it is, per icon. Checked in this order. */
    private val KEYWORDS: List<Pair<SwitchIcon, Set<String>>> = listOf(
        SwitchIcon.COOL to setOf("ac", "a/c", "aircon", "conditioner", "conditioning", "cooler", "cooling", "mazgan"),
        SwitchIcon.HEAT to setOf("boiler", "heater", "heating", "radiator", "fireplace", "kettle", "dud"),
        SwitchIcon.LIGHT to setOf("light", "lights", "lamp", "bulb", "led"),
        SwitchIcon.AIR to setOf("fan", "purifier", "ventilator"),
        SwitchIcon.TV to setOf("tv", "television"),
        SwitchIcon.PLUG to setOf("plug", "socket", "outlet"),
    )

    /** Fallback by Google Home device type class name, e.g. "RoomAirConditionerDevice". */
    private val TYPE_HINTS: List<Pair<String, SwitchIcon>> = listOf(
        "AirConditioner" to SwitchIcon.COOL,
        "AirCooler" to SwitchIcon.COOL,
        "HeatingCooling" to SwitchIcon.COOL,
        "Boiler" to SwitchIcon.HEAT,
        "WaterHeater" to SwitchIcon.HEAT,
        "Radiator" to SwitchIcon.HEAT,
        "Fireplace" to SwitchIcon.HEAT,
        "Kettle" to SwitchIcon.HEAT,
        "Thermostat" to SwitchIcon.HEAT,
        "Light" to SwitchIcon.LIGHT,
        "Fan" to SwitchIcon.AIR,
        "Purifier" to SwitchIcon.AIR,
        "Ventilator" to SwitchIcon.AIR,
        "PlugIn" to SwitchIcon.PLUG,
    )

    private fun words(name: String): List<String> =
        name.lowercase().split(Regex("[^\\p{L}\\p{N}/]+")).filter { it.isNotEmpty() }

    fun icon(name: String, typeNames: Collection<String> = emptyList()): SwitchIcon {
        val w = words(name)
        KEYWORDS.firstOrNull { (_, keys) -> w.any { it in keys } }?.let { return it.first }
        TYPE_HINTS.firstOrNull { (hint, _) -> typeNames.any { it.contains(hint) } }?.let { return it.second }
        return SwitchIcon.POWER
    }

    /** The first word that isn't a "what it is" word; the whole name if nothing is left. */
    fun word(name: String): String {
        val w = words(name)
        val allKeys = KEYWORDS.flatMap { it.second }.toSet()
        return w.firstOrNull { it !in allKeys } ?: w.firstOrNull() ?: "?"
    }
}
