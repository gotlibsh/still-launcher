package com.gotlibsh.still

import com.gotlibsh.still.data.WeatherPlace
import com.gotlibsh.still.widgets.weather.OpenMeteoParser
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoParserTest {

    @Test fun parsesForecast() {
        val json = """
            {"current":{"time":"2026-11-07T14:00","temperature_2m":18.6,"weather_code":2},
             "daily":{"time":["2026-11-07"],"temperature_2m_max":[21.2],"temperature_2m_min":[12.4]}}
        """.trimIndent()
        val place = WeatherPlace("Tel Aviv", 32.08, 34.78)
        val w = OpenMeteoParser.parseForecast(json, place, fahrenheit = false, now = 1L)
        assertEquals(19, w.temperature)
        assertEquals(21, w.high)
        assertEquals(12, w.low)
        assertEquals("Partly cloudy", w.description)
        assertEquals("°C", w.unit)
    }

    @Test fun parsesPlaces() {
        val json = """
            {"results":[{"name":"Paris","admin1":"Île-de-France","country":"France","latitude":48.85,"longitude":2.35},
                        {"name":"Paris","country":"United States","latitude":33.66,"longitude":-95.55}]}
        """.trimIndent()
        val places = OpenMeteoParser.parsePlaces(json)
        assertEquals(2, places.size)
        assertEquals("Paris, Île-de-France, France", places[0].name)
        assertEquals("Paris, United States", places[1].name)
    }

    @Test fun noResultsIsEmpty() = assertEquals(0, OpenMeteoParser.parsePlaces("{}").size)
}
