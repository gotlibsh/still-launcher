package com.gotlibsh.still.widgets.weather

import com.gotlibsh.still.data.WeatherPlace
import kotlinx.coroutines.CancellationException
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

    private val _selectedIndex = MutableStateFlow(0)
    /** Which saved city the home screen shows (index into LauncherSettings.allWeatherPlaces). */
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()
    fun select(index: Int) { _selectedIndex.value = index }

    suspend fun refresh(place: WeatherPlace?, fahrenheit: Boolean, force: Boolean = false) = mutex.withLock {
        if (place == null) {
            _state.value = WeatherState.NotConfigured
            return@withLock
        }
        // Remember every city's last reading so swiping between cities is instant.
        val last = nowCache[place to fahrenheit]
        val sameRequest = last != null
        val fresh = last != null && System.currentTimeMillis() - last.fetchedAtMillis < MAX_AGE_MILLIS
        if (fresh && !force) {
            _state.value = WeatherState.Ready(last!!)
            return@withLock
        }
        _state.value = if (last != null) WeatherState.Ready(last) else WeatherState.Loading

        _state.value = try {
            val unit = if (fahrenheit) "&temperature_unit=fahrenheit" else ""
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${place.latitude}&longitude=${place.longitude}" +
                "&current=temperature_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min" +
                "&timezone=auto&forecast_days=1$unit"
            val body = httpGet(url)
            val weather = OpenMeteoParser.parseForecast(body, place, fahrenheit, System.currentTimeMillis())
            nowCache[place to fahrenheit] = weather
            WeatherState.Ready(weather)
        } catch (e: CancellationException) {
            throw e // switched city mid-request: not a failure
        } catch (e: Exception) {
            WeatherState.Failed("Weather unavailable", if (sameRequest) last else null)
        }
    }

    private val nowCache = HashMap<Pair<WeatherPlace, Boolean>, Weather>()
    private val forecastCache = HashMap<Pair<WeatherPlace, Boolean>, Pair<Long, Forecast>>()

    /** Full forecast for [place] (current, 12 hours, 7 days). Cached for 10 minutes. Null on failure. */
    suspend fun forecast(place: WeatherPlace, fahrenheit: Boolean): Forecast? {
        val key = place to fahrenheit
        synchronized(forecastCache) {
            forecastCache[key]?.let { (at, f) -> if (System.currentTimeMillis() - at < FORECAST_MAX_AGE_MILLIS) return f }
        }
        return try {
            val unit = if (fahrenheit) "&temperature_unit=fahrenheit&wind_speed_unit=mph" else ""
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${place.latitude}&longitude=${place.longitude}" +
                "&current=temperature_2m,apparent_temperature,weather_code,relative_humidity_2m,wind_speed_10m" +
                "&hourly=temperature_2m,weather_code,precipitation_probability" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto&forecast_days=7$unit"
            val f = OpenMeteoForecastParser.parse(httpGet(url), place, fahrenheit)
            synchronized(forecastCache) { forecastCache[key] = System.currentTimeMillis() to f }
            f
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
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
        const val FORECAST_MAX_AGE_MILLIS = 10 * 60 * 1000L
    }
}
