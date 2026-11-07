package com.gotlibsh.still

import com.gotlibsh.still.data.AppSearch
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchTest {
    private val apps = listOf("Calendar", "Google Maps", "Camera", "Messages", "Café Notes", "Play Store")
    private fun search(q: String) = AppSearch.filter(apps, q) { it }

    @Test fun emptyQueryReturnsAll() = assertEquals(apps, search("  "))

    @Test fun prefixBeforeWordStartBeforeContains() {
        assertEquals(listOf("Calendar", "Camera"), search("ca").take(2))
        assertEquals(listOf("Google Maps"), search("maps"))
        assertEquals(listOf("Messages"), search("sag"))
    }

    @Test fun initialsAndAccentsMatch() {
        assertEquals("Play Store", search("ps").first())
        assertEquals(listOf("Café Notes"), search("cafe"))
    }
}
