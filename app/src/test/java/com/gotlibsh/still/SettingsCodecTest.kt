package com.gotlibsh.still

import com.gotlibsh.still.data.SettingsCodec
import com.gotlibsh.still.data.WeatherPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsCodecTest {

    @Test fun listRoundTrip() {
        val items = listOf("a/b/0", "c/d/10")
        assertEquals(items, SettingsCodec.decodeList(SettingsCodec.encodeList(items)))
        assertEquals(emptyList<String>(), SettingsCodec.decodeList(null))
        assertEquals(emptyList<String>(), SettingsCodec.decodeList(""))
    }

    @Test fun placeRoundTripKeepsCommasAndPipesInName() {
        val place = WeatherPlace("Tel Aviv, Israel | home", 32.08, 34.78)
        assertEquals(place, SettingsCodec.decodePlace(SettingsCodec.encodePlace(place)))
        assertNull(SettingsCodec.decodePlace(""))
        assertNull(SettingsCodec.decodePlace("x|y|z"))
    }

    @Test fun reconcileDropsUnknownAndAppendsNew() {
        val result = SettingsCodec.reconcileOrder(listOf("notes", "gone", "clock"), listOf("weather", "clock", "notes"))
        assertEquals(listOf("notes", "clock", "weather"), result)
    }

    @Test fun moveStaysInBounds() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), SettingsCodec.move(list, "b", -1))
        assertEquals(listOf("a", "c", "b"), SettingsCodec.move(list, "b", +1))
        assertEquals(list, SettingsCodec.move(list, "a", -1))
        assertEquals(list, SettingsCodec.move(list, "c", +1))
        assertEquals(list, SettingsCodec.move(list, "zzz", +1))
    }

    @Test fun appNamesRoundTrip() {
        val names = mapOf("com.a/com.a.Main/0" to "Mail", "com.b/com.b.Main/10" to "Cal = nice")
        assertEquals(names, SettingsCodec.decodeNames(SettingsCodec.encodeNames(names)))
    }

    @Test fun appNamesDropLineBreaksAndBlanks() {
        val encoded = SettingsCodec.encodeNames(mapOf("k1" to "two\nlines", "k2" to "   "))
        assertEquals(mapOf("k1" to "two lines"), SettingsCodec.decodeNames(encoded))
        assertEquals(emptyMap<String, String>(), SettingsCodec.decodeNames(null))
    }

    @Test fun initialsForShortSwitchNames() {
        assertEquals("ALR", SettingsCodec.initials("AC Living Room"))
        assertEquals("AB", SettingsCodec.initials("AC Bedroom"))
        assertEquals("B", SettingsCodec.initials("Boiler"))
        assertEquals("KLD", SettingsCodec.initials("kitchen-light, desk lamp"))
        assertEquals("?", SettingsCodec.initials("  "))
    }

    @Test fun recentAppsNewestFirstWithoutDuplicates() {
        assertEquals(listOf("b", "a", "c"), SettingsCodec.pushRecent(listOf("a", "b", "c"), "b"))
        assertEquals(listOf("z", "a", "b"), SettingsCodec.pushRecent(listOf("a", "b", "c"), "z", max = 3))
    }

    @Test fun recentToShowSkipsHiddenAndUninstalled() {
        val apps = mapOf("a" to "A", "b" to "B", "c" to "C", "d" to "D")
        val recent = listOf("gone", "b", "c", "a", "d")
        assertEquals(listOf("B", "A"), SettingsCodec.recentToShow(recent, apps, hidden = setOf("c"), count = 2))
        assertEquals(emptyList<String>(), SettingsCodec.recentToShow(recent, apps, hidden = emptySet(), count = 0))
    }
}
