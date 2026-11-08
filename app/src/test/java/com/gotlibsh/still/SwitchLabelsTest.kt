package com.gotlibsh.still

import com.gotlibsh.still.widgets.homecontrols.SwitchIcon
import com.gotlibsh.still.widgets.homecontrols.SwitchLabels
import org.junit.Assert.assertEquals
import org.junit.Test

class SwitchLabelsTest {
    @Test fun iconsFromNames() {
        assertEquals(SwitchIcon.COOL, SwitchLabels.icon("AC Living Room"))
        assertEquals(SwitchIcon.HEAT, SwitchLabels.icon("Boiler", listOf("OnOffPluginUnitDevice")))
        assertEquals(SwitchIcon.LIGHT, SwitchLabels.icon("Desk lamp"))
        assertEquals(SwitchIcon.AIR, SwitchLabels.icon("Air purifier"))
        assertEquals(SwitchIcon.TV, SwitchLabels.icon("Samsung TV"))
    }

    @Test fun iconFallsBackToDeviceTypeThenPower() {
        assertEquals(SwitchIcon.COOL, SwitchLabels.icon("Bedroom unit", listOf("RoomAirConditionerDevice")))
        assertEquals(SwitchIcon.PLUG, SwitchLabels.icon("Thing", listOf("OnOffPluginUnitDevice")))
        assertEquals(SwitchIcon.POWER, SwitchLabels.icon("Thing"))
    }

    @Test fun shortWordDropsTheDeviceKind() {
        assertEquals("living", SwitchLabels.word("AC Living Room"))
        assertEquals("bedroom", SwitchLabels.word("AC Bedroom"))
        assertEquals("boiler", SwitchLabels.word("Boiler"))
        assertEquals("kitchen", SwitchLabels.word("Kitchen light"))
    }
}
