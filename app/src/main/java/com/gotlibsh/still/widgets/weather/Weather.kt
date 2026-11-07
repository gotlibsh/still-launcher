package com.gotlibsh.still.widgets.weather

import com.gotlibsh.still.data.WeatherPlace
import org.json.JSONObject
import kotlin.math.roundToInt

data class Weather(
    val temperature: Int,
    val code: Int,
    val high: Int,
    val low: Int,
    val place: WeatherPlace,
    val fahrenheit: Boolean,
    val fetchedAtMillis: Long,
) {
    val description: String get() = WeatherCodes.describe(code)
    val unit: String get() = if (fahrenheit) "°F" else "°C"
}

sealed interface WeatherState {
    data object NotConfigured : WeatherState
    data object Loading : WeatherState
    data class Ready(val weather: Weather) : WeatherState
    data class Failed(val message: String, val lastKnown: Weather?) : WeatherState
}

/** Plain-language names for WMO weather codes, as used by Open-Meteo. */
object WeatherCodes {
    fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71 -> "Light snow"
        73 -> "Snow"
        75 -> "Heavy snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm, hail"
        else -> "—"
    }
}

/** Parses Open-Meteo JSON. Kept separate from networking so it can be unit tested. */
object OpenMeteoParser {

    fun parseForecast(json: String, place: WeatherPlace, fahrenheit: Boolean, now: Long): Weather {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        return Weather(
            temperature = current.getDouble("temperature_2m").roundToInt(),
            code = current.getInt("weather_code"),
            high = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
            low = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
            place = place,
            fahrenheit = fahrenheit,
            fetchedAtMillis = now,
        )
    }

    fun parsePlaces(json: String): List<WeatherPlace> {
        val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val r = results.getJSONObject(i)
            val label = listOf(r.optString("name"), r.optString("admin1"), r.optString("country"))
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")
            WeatherPlace(label, r.getDouble("latitude"), r.getDouble("longitude"))
        }
    }
}
