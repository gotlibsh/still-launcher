package com.gotlibsh.still.widgets.weather

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gotlibsh.still.data.allWeatherPlaces
import com.gotlibsh.still.stillApp
import com.gotlibsh.still.ui.StillColors
import com.gotlibsh.still.ui.quietClickable
import com.gotlibsh.still.widgets.HomeWidget
import com.gotlibsh.still.widgets.WidgetHost
import kotlinx.coroutines.launch

/**
 * One quiet line of weather: "18°C  Partly cloudy", then "H 21° · L 12° · Tel Aviv".
 * Tap for the full forecast, long-press for settings. Refreshes at most every 30 minutes.
 */
object WeatherWidget : HomeWidget {
    override val id = "weather"
    override val title = "Weather"

    @Composable
    override fun Content(host: WidgetHost) {
        val repo = LocalContext.current.stillApp().weather
        val state by repo.state.collectAsState()
        val selected by repo.selectedIndex.collectAsState()
        val scope = rememberCoroutineScope()
        val places = host.settings.allWeatherPlaces
        val index = if (places.isEmpty()) 0 else selected.coerceIn(0, places.size - 1)
        val place = places.getOrNull(index)
        val fahrenheit = host.settings.fahrenheit

        LaunchedEffect(place, fahrenheit) { repo.refresh(place, fahrenheit) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { repo.refresh(place, fahrenheit) } }

        // Tap: full forecast. Long-press: settings.
        // Swipe left / right: next / previous saved city. Consuming the drag keeps the home
        // screen's own left/right app swipes from firing when you swipe on the weather line.
        val modifier = Modifier
            .pointerInput(places.size) {
                if (places.size < 2) return@pointerInput
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        val step = when {
                            total < -SWIPE_PX -> 1
                            total > SWIPE_PX -> -1
                            else -> 0
                        }
                        if (step != 0) repo.select(Math.floorMod(repo.selectedIndex.value + step, places.size))
                    },
                ) { change, amount ->
                    change.consume()
                    total += amount
                }
            }
            .quietClickable(onLongClick = host::openSettings) {
                if (place == null) host.openSettings() else host.openDetail(this)
            }

        Column(modifier.fillMaxWidth()) {
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
    override fun Detail(host: WidgetHost, onClose: () -> Unit) {
        val repo = LocalContext.current.stillApp().weather
        val places = host.settings.allWeatherPlaces
        ForecastScreen(
            places = places,
            fahrenheit = host.settings.fahrenheit,
            initialIndex = repo.selectedIndex.value.coerceIn(0, (places.size - 1).coerceAtLeast(0)),
            onPageChange = repo::select,
            onClose = onClose,
        )
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

    private const val SWIPE_PX = 80f
}
