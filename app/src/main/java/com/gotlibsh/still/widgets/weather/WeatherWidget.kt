package com.gotlibsh.still.widgets.weather

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gotlibsh.still.stillApp
import com.gotlibsh.still.ui.StillColors
import com.gotlibsh.still.ui.quietClickable
import com.gotlibsh.still.widgets.HomeWidget
import com.gotlibsh.still.widgets.WidgetHost
import kotlinx.coroutines.launch

/**
 * One quiet line of weather: "18°C  Partly cloudy", then "H 21° · L 12° · Tel Aviv".
 * Tap to refresh, long-press to change the city. Refreshes at most every 30 minutes.
 */
object WeatherWidget : HomeWidget {
    override val id = "weather"
    override val title = "Weather"

    @Composable
    override fun Content(host: WidgetHost) {
        val repo = LocalContext.current.stillApp().weather
        val state by repo.state.collectAsState()
        val scope = rememberCoroutineScope()
        val place = host.settings.weatherPlace
        val fahrenheit = host.settings.fahrenheit

        LaunchedEffect(place, fahrenheit) { repo.refresh(place, fahrenheit) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { repo.refresh(place, fahrenheit) } }

        val modifier = Modifier.quietClickable(onLongClick = host::openSettings) {
            if (place == null) host.openSettings()
            else scope.launch { repo.refresh(place, fahrenheit, force = true) }
        }

        Column(modifier) {
            when (val s = state) {
                WeatherState.NotConfigured -> Dim("Tap to set your city for weather")
                WeatherState.Loading -> Dim("…")
                is WeatherState.Ready -> WeatherLines(s.weather, stale = false)
                is WeatherState.Failed ->
                    if (s.lastKnown != null) WeatherLines(s.lastKnown, stale = true) else Dim(s.message)
            }
        }
    }

    @Composable
    private fun WeatherLines(w: Weather, stale: Boolean) {
        Text(
            "${w.temperature}${w.unit}  ${w.description}",
            fontSize = 18.sp,
            color = if (stale) StillColors.dim else StillColors.text,
        )
        Text(
            "H ${w.high}° · L ${w.low}° · ${w.place.name.substringBefore(",")}",
            fontSize = 14.sp,
            color = StillColors.dim,
        )
    }

    @Composable
    private fun Dim(text: String) = Text(text, fontSize = 14.sp, color = StillColors.dim)
}
