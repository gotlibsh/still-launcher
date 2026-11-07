package com.gotlibsh.still

import com.gotlibsh.still.data.Background
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundTest {
    @Test fun unknownIdFallsBackToBlack() {
        assertEquals(Background.BLACK, Background.of(null))
        assertEquals(Background.BLACK, Background.of("gone"))
        assertEquals(Background.NAVY, Background.of("navy"))
    }

    @Test fun cyclesWrapAround() {
        assertEquals(Background.BLACK, Background.next(Background.WALLPAPER))
        assertEquals(60, Background.nextDim(40))
        assertEquals(0, Background.nextDim(80))
        assertEquals(20, Background.nextDim(5))
    }
}
