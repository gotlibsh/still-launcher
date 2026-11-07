package com.gotlibsh.still.widgets.clock

import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gotlibsh.still.ui.StillColors
import com.gotlibsh.still.ui.quietClickable
import com.gotlibsh.still.widgets.HomeWidget
import com.gotlibsh.still.widgets.WidgetHost
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Big time, small date. Tap the time for alarms, the date for your calendar. */
object ClockWidget : HomeWidget {
    override val id = "clock"
    override val title = "Clock & date"

    @Composable
    override fun Content(host: WidgetHost) {
        val context = LocalContext.current
        val now by produceState(LocalDateTime.now()) {
            while (true) {
                value = LocalDateTime.now()
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
        val is24 = host.settings.use24Hour ?: DateFormat.is24HourFormat(context)
        val timeFormat = remember(is24) { DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm") }
        val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }

        Column {
            Text(
                text = now.format(timeFormat),
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                color = StillColors.text,
                modifier = Modifier.quietClickable { host.startIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
            )
            Text(
                text = now.format(dateFormat),
                fontSize = 18.sp,
                color = StillColors.dim,
                modifier = Modifier.quietClickable {
                    host.startIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR))
                },
            )
        }
    }
}
