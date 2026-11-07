package com.gotlibsh.still

import com.gotlibsh.still.data.WeatherPlace
import com.gotlibsh.still.widgets.weather.OpenMeteoForecastParser
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoForecastParserTest {

    private val place = WeatherPlace("Tel Aviv", 32.08, 34.78)

    private val hourTimes = (0..23).joinToString(",") { "\"2026-11-07T%02d:00\"".format(it) }
    private val hourTemps = (0..23).joinToString(",") { "${10 + it}.4" }
    private val json = """
        {
          "current": {"time": "2026-11-07T15:15", "temperature_2m": 21.6, "apparent_temperature": 20.2,
                      "weather_code": 2, "relative_humidity_2m": 64, "wind_speed_10m": 12.7},
          "hourly": {"time": [$hourTimes], "temperature_2m": [$hourTemps],
                     "weather_code": [${(0..23).joinToString(",") { "3" }}],
                     "precipitation_probability": [${(0..23).joinToString(",") { "$it" }}]},
          "daily": {"time": ["2026-11-07", "2026-11-08"], "weather_code": [2, 61],
                    "temperature_2m_max": [23.5, 19.1], "temperature_2m_min": [14.4, 12.6],
                    "precipitation_probability_max": [10, 80]}
        }
    """.trimIndent()

    @Test
    fun parsesCurrentConditions() {
        val f = OpenMeteoForecastParser.parse(json, place, fahrenheit = false)
        assertEquals(22, f.temperature)
        assertEquals(20, f.feelsLike)
        assertEquals(64, f.humidity)
        assertEquals(13, f.windKmh)
        assertEquals(place, f.place)
    }

    @Test
    fun hoursStartAtTheCurrentHourAndStopAfterTwelve() {
        val f = OpenMeteoForecastParser.parse(json, place, fahrenheit = false)
        assertEquals(9, f.hours.size) // 15:00..23:00, only 9 left in the data
        assertEquals("15:00", f.hours.first().time)
        assertEquals(25, f.hours.first().temperature)
        assertEquals(15, f.hours.first().rainChance)
    }

    @Test
    fun parsesDays() {
        val f = OpenMeteoForecastParser.parse(json, place, fahrenheit = false)
        assertEquals(2, f.days.size)
        assertEquals("2026-11-08", f.days[1].date)
        assertEquals(19, f.days[1].high)
        assertEquals(13, f.days[1].low)
        assertEquals(80, f.days[1].rainChance)
    }
}
