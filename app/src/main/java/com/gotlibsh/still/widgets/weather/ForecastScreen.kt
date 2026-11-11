package com.gotlibsh.still.widgets.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gotlibsh.still.data.WeatherPlace
import com.gotlibsh.still.stillApp
import com.gotlibsh.still.ui.StillColors
import com.gotlibsh.still.ui.quietClickable
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Full forecast for each saved city. Swipe left/right to move between cities; the city
 * you land on becomes the one shown on the home screen.
 */
@Composable
fun ForecastScreen(
    places: List<WeatherPlace>,
    fahrenheit: Boolean,
    initialIndex: Int,
    onPageChange: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val pager = rememberPagerState(initialPage = initialIndex.coerceIn(0, (places.size - 1).coerceAtLeast(0))) { places.size }
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect(onPageChange) }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(top = 24.dp)) {
        Row(Modifier.padding(horizontal = 32.dp)) {
            Text("‹ back", color = StillColors.dim, fontSize = 16.sp, modifier = Modifier.quietClickable(onClick = onClose))
            Spacer(Modifier.weight(1f))
            if (places.size > 1) {
                Text(
                    places.indices.joinToString(" ") { if (it == pager.currentPage) "●" else "○" },
                    color = StillColors.dim,
                    fontSize = 12.sp,
                )
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            PlaceForecast(places[page], fahrenheit)
        }
    }
}

@Composable
private fun PlaceForecast(place: WeatherPlace, fahrenheit: Boolean) {
    val repo = LocalContext.current.stillApp().weather
    // first = finished loading, second = the forecast (null if it failed).
    var attempt by remember { mutableIntStateOf(0) }
    val result by produceState<Pair<Boolean, Forecast?>>(false to null, place, fahrenheit, attempt) {
        value = false to null
        value = true to repo.forecast(place, fahrenheit)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        Text(place.name.substringBefore(","), color = StillColors.text, fontSize = 28.sp)
        Spacer(Modifier.height(16.dp))
        val f = result.second
        if (f == null) {
            if (result.first) {
                Text(
                    "Weather unavailable. Tap to try again.",
                    color = StillColors.dim,
                    fontSize = 16.sp,
                    modifier = Modifier.quietClickable { attempt++ },
                )
            } else {
                Text("…", color = StillColors.dim, fontSize = 16.sp)
            }
        } else {
            ForecastBody(f)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ForecastBody(f: Forecast) {
    Column {
        Text("${f.temperature}${f.unit}", color = StillColors.text, fontSize = 56.sp, fontWeight = FontWeight.Light)
        Text(f.description, color = StillColors.text, fontSize = 18.sp)
        Text(
            "feels ${f.feelsLike}° · humidity ${f.humidity}% · wind ${f.windKmh} ${f.windUnit}",
            color = StillColors.dim,
            fontSize = 14.sp,
        )

        Section("Next hours")
        f.hours.forEach { h ->
            ForecastRow(h.time, "${h.temperature}°", WeatherCodes.describe(h.code), rain(h.rainChance))
        }

        Section("Next days")
        f.days.forEachIndexed { i, d ->
            ForecastRow(dayName(d.date, i), "${d.high}° / ${d.low}°", WeatherCodes.describe(d.code), rain(d.rainChance))
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        color = StillColors.dim,
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 28.dp, bottom = 6.dp),
    )
}

@Composable
private fun ForecastRow(first: String, temps: String, description: String, rain: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(first, color = StillColors.dim, fontSize = 15.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(56.dp))
        Text(temps, color = StillColors.text, fontSize = 15.sp, modifier = Modifier.width(88.dp))
        Text(description, color = StillColors.dim, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(rain, color = StillColors.faint, fontSize = 15.sp)
    }
}

private fun rain(chance: Int) = if (chance >= 10) "$chance%" else ""

private fun dayName(date: String, index: Int): String = when (index) {
    0 -> "Today"
    else -> runCatching { LocalDate.parse(date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
        .getOrDefault(date)
}
