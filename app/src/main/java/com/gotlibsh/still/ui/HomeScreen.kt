package com.gotlibsh.still.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gotlibsh.still.widgets.HomeWidget
import com.gotlibsh.still.widgets.WidgetHost

/**
 * The home screen is just the enabled widgets stacked top to bottom.
 * Swipe up: all apps. Swipe down: notifications. Swipe left / right: the apps chosen in
 * settings. Long-press empty space: settings. Triple tap empty space: a WhatsApp chat.
 */
@Composable
fun HomeScreen(
    host: WidgetHost,
    widgets: List<HomeWidget>,
    onOpenDrawer: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onTripleTap: () -> Unit,
) {
    val context = LocalContext.current
    val openDrawer by rememberUpdatedState(onOpenDrawer)
    val openSettings by rememberUpdatedState(host::openSettings)
    val swipeLeft by rememberUpdatedState(onSwipeLeft)
    val swipeRight by rememberUpdatedState(onSwipeRight)
    val tripleTap by rememberUpdatedState(onTripleTap)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // In dp, so a swipe is the same finger distance on every screen.
                val swipe = SWIPE_DISTANCE.toPx()
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDragEnd = {
                        val (x, y) = total
                        when {
                            abs(y) >= abs(x) && y < -swipe -> openDrawer()
                            abs(y) >= abs(x) && y > swipe -> expandNotifications(context)
                            abs(x) > abs(y) && x < -swipe -> swipeLeft()
                            abs(x) > abs(y) && x > swipe -> swipeRight()
                        }
                    },
                ) { _, dragAmount -> total += dragAmount }
            }
            .pointerInput(Unit) {
                // Taps on widgets are theirs; only taps on empty space count here.
                var taps = 0
                var lastTap = 0L
                detectTapGestures(
                    onLongPress = { openSettings() },
                    onTap = {
                        val now = System.currentTimeMillis()
                        taps = if (now - lastTap < TAP_GAP_MS) taps + 1 else 1
                        lastTap = now
                        if (taps == 3) {
                            taps = 0
                            tripleTap()
                        }
                    },
                )
            }
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 28.dp),
    ) {
        widgets.forEach { widget ->
            key(widget.id) {
                if (widget.fillsSpace) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) { widget.Content(host) }
                } else {
                    widget.Content(host)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        if (widgets.none { it.fillsSpace }) Spacer(Modifier.weight(1f))
    }
}

private val SWIPE_DISTANCE = 44.dp
private const val TAP_GAP_MS = 450L

/** Pulls down the notification shade. Uses a hidden API, so it fails silently if blocked. */
@SuppressLint("WrongConstant")
private fun expandNotifications(context: Context) {
    try {
        val service = context.getSystemService("statusbar") ?: return
        Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
    } catch (_: Exception) {
    }
}
