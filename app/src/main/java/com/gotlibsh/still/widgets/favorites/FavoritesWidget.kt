package com.gotlibsh.still.widgets.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gotlibsh.still.data.AppInfo
import com.gotlibsh.still.ui.StillColors
import com.gotlibsh.still.ui.quietClickable
import com.gotlibsh.still.widgets.HomeWidget
import com.gotlibsh.still.widgets.WidgetHost

/** Your few chosen apps as plain words. Everything else is one swipe up away. */
object FavoritesWidget : HomeWidget {
    override val id = "favorites"
    override val title = "Favorite apps"
    override val fillsSpace = true

    @Composable
    override fun Content(host: WidgetHost) {
        val favorites = host.settings.favorites.mapNotNull(host::findApp)
        // Shrink the words (never below 60%) when the list is taller than the room left on
        // the home screen, so the last favorites aren't cut off.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale
            val scale = FavoritesLayout.scale(
                count = favorites.size,
                availableDp = if (constraints.hasBoundedHeight) maxHeight.value else Float.MAX_VALUE,
                fontScale = fontScale,
            )
            Column(verticalArrangement = Arrangement.spacedBy((FavoritesLayout.GAP_DP * scale).dp)) {
                if (favorites.isEmpty()) {
                    Text(
                        "Swipe up for all apps.\nLong-press one to put it here.",
                        fontSize = 16.sp,
                        color = StillColors.faint,
                    )
                }
                favorites.forEach { app -> FavoriteRow(app, host, (FavoritesLayout.FONT_SP * scale).sp) }
            }
        }
    }

    @Composable
    private fun FavoriteRow(app: AppInfo, host: WidgetHost, fontSize: TextUnit) {
        var menu by remember { mutableStateOf(false) }
        Box {
            Text(
                text = app.label,
                fontSize = fontSize,
                maxLines = 1,
                color = StillColors.text,
                modifier = Modifier.quietClickable(onLongClick = { menu = true }) { host.launch(app) },
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Remove from home") }, onClick = {
                    menu = false
                    val updated = host.settings.favorites - app.key
                    host.editSettings { setFavorites(updated) }
                })
                DropdownMenuItem(text = { Text("App info") }, onClick = {
                    menu = false
                    host.openAppInfo(app)
                })
            }
        }
    }
}

/** Sizing for the favorites list. Pure numbers so it can be unit tested. */
object FavoritesLayout {
    const val FONT_SP = 26f
    const val GAP_DP = 14f
    private const val LINE_HEIGHT = 1.35f // a text line is about this many times its font size
    private const val MIN_SCALE = 0.6f

    /** 1 when [count] favorites fit in [availableDp]; otherwise how much to shrink them. */
    fun scale(count: Int, availableDp: Float, fontScale: Float): Float {
        if (count <= 0) return 1f
        val needed = count * FONT_SP * fontScale * LINE_HEIGHT + (count - 1) * GAP_DP
        return if (needed <= availableDp) 1f else (availableDp / needed).coerceIn(MIN_SCALE, 1f)
    }
}
