package com.gotlibsh.still.widgets.weather

import com.gotlibsh.still.data.WeatherPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Weather from Open-Meteo (free, no API key, no account). The location is a city you type
 * in settings, so the launcher never needs the location permission.
 */
class WeatherRepository {

    private val _state = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val state: StateFlow<WeatherState> = _state.asStateFlow()
    private val mutex = Mutex()

    suspend fun refresh(place: WeatherPlace?, fahrenheit: Boolean, force: Boolean = false) = mutex.withLock {
        if (place == null) {
            _state.value = WeatherState.NotConfigured
            return@withLock
        }
        val last = when (val s = _state.value) {
            is WeatherState.Ready -> s.weather
            is WeatherState.Failed -> s.lastKnown
            else -> null
        }
        val sameRequest = last != null && last.place == place && last.fahrenheit == fahrenheit
        val fresh = sameRequest && System.currentTimeMillis() - last!!.fetchedAtMillis < MAX_AGE_MILLIS
        if (fresh && !force) return@withLock
        if (!sameRequest) _state.value = WeatherState.Loading

        _state.value = try {
            val unit = if (fahrenheit) "&temperature_unit=fahrenheit" else ""
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${place.latitude}&longitude=${place.longitude}" +
                "&current=temperature_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min" +
                "&timezone=auto&forecast_days=1$unit"
            val body = httpGet(url)
            WeatherState.Ready(OpenMeteoParser.parseForecast(body, place, fahrenheit, System.currentTimeMillis()))
        } catch (e: Exception) {
            WeatherState.Failed("Weather unavailable", if (sameRequest) last else null)
        }
    }

    /** Looks up a city name and returns matching places (empty on failure). */
    suspend fun searchPlaces(query: String): List<WeatherPlace> {
        if (query.isBlank()) return emptyList()
        return try {
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            OpenMeteoParser.parsePlaces(
                httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=5&language=en&format=json")
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("User-Agent", "StillLauncher/0.1")
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val MAX_AGE_MILLIS = 30 * 60 * 1000L
    }
}
