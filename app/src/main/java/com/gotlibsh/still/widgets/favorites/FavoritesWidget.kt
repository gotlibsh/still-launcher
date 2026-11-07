package com.gotlibsh.still.widgets.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (favorites.isEmpty()) {
                Text(
                    "Swipe up for all apps.\nLong-press one to put it here.",
                    fontSize = 16.sp,
                    color = StillColors.faint,
                )
            }
            favorites.forEach { app -> FavoriteRow(app, host) }
        }
    }

    @Composable
    private fun FavoriteRow(app: AppInfo, host: WidgetHost) {
        var menu by remember { mutableStateOf(false) }
        Box {
            Text(
                text = app.label,
                fontSize = 26.sp,
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
