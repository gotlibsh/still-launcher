package com.gotlibsh.still

import com.gotlibsh.still.widgets.favorites.FavoritesLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesLayoutTest {
    @Test fun fullSizeWhenThereIsRoom() {
        assertEquals(1f, FavoritesLayout.scale(count = 4, availableDp = 600f, fontScale = 1f))
        assertEquals(1f, FavoritesLayout.scale(count = 0, availableDp = 10f, fontScale = 1f))
    }

    @Test fun shrinksToFitButNotBelowTheMinimum() {
        val s = FavoritesLayout.scale(count = 8, availableDp = 300f, fontScale = 1f)
        assertTrue(s < 1f && s > 0.6f)
        assertEquals(0.6f, FavoritesLayout.scale(count = 8, availableDp = 50f, fontScale = 1f))
    }
}
