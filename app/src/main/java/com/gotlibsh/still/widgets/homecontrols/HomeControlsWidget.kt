package com.gotlibsh.still.widgets.homecontrols

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.home.PermissionsState
import com.gotlibsh.still.stillApp
import com.gotlibsh.still.ui.StillColors
import com.gotlibsh.still.ui.quietClickable
import com.gotlibsh.still.widgets.HomeWidget
import com.gotlibsh.still.widgets.WidgetHost
import kotlinx.coroutines.launch

/**
 * A few Google Home devices as small switches, up to four per row: a line icon plus a short
 * word, e.g. "❄ living  ❄ bedroom  🔥 boiler" (drawn as theme-colored icons, not emoji).
 * Bright = on, faint = off. Pick devices, words and icons in Settings > Google Home.
 */
object HomeControlsWidget : HomeWidget {
    override val id = "home_controls"
    override val title = "Google Home switches"
    override val enabledByDefault = false

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content(host: WidgetHost) {
        val ids = host.settings.homeDeviceIds
        val context = LocalContext.current
        val repo = context.stillApp().googleHome
        val permission by remember(repo) { repo.permissionState }
            .collectAsState(initial = PermissionsState.PERMISSIONS_STATE_UNINITIALIZED)

        when {
            ids.isEmpty() -> {
                Dim("Pick Google Home devices in settings", Modifier.quietClickable(onClick = host::openSettings))
                return
            }
            permission == PermissionsState.NOT_GRANTED || permission == PermissionsState.PERMISSIONS_STATE_UNAVAILABLE -> {
                Dim("Google Home isn't connected. Tap to open settings.", Modifier.quietClickable(onClick = host::openSettings))
                return
            }
        }

        val switches by remember(repo) { repo.switches() }.collectAsState(initial = null)
        val pending = remember { mutableStateMapOf<String, Boolean>() }
        val scope = rememberCoroutineScope()

        // Icon + short word per switch, up to four per row. Bright = on, faint = off,
        // crossed out = offline. Tap one to flip it.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = PER_ROW,
        ) {
            ids.forEach { id ->
                val device = switches?.firstOrNull { it.id == id }
                val wanted = pending[id]
                val online = device?.online == true
                val isOn = online && (wanted ?: device?.isOn) == true
                val color = when {
                    wanted != null -> StillColors.dim
                    isOn -> StillColors.text
                    else -> StillColors.faint
                }
                val name = device?.name.orEmpty()
                val icon = iconFor(id, device, host.settings.homeDeviceIcons)
                val word = host.settings.homeDeviceLabels[id]
                    ?: if (device == null) (if (switches == null) "…" else "missing") else SwitchLabels.word(name)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.quietClickable(onLongClick = host::openSettings) {
                        if (device == null || !device.online || wanted != null) return@quietClickable
                        val target = device.isOn != true
                        pending[id] = target
                        scope.launch {
                            val ok = repo.setOn(id, target)
                            pending.remove(id)
                            if (!ok) Toast.makeText(context, "Couldn't switch ${device.name}", Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Icon(SwitchIcons.vector(icon), contentDescription = name, tint = color, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        word,
                        fontSize = 17.sp,
                        color = color,
                        textDecoration = if (device != null && !online) TextDecoration.LineThrough else null,
                    )
                }
            }
        }
    }

    /** Hand-picked icon if set, otherwise guessed from the device's name and type. */
    fun iconFor(id: String, device: HomeSwitch?, overrides: Map<String, String>): SwitchIcon =
        SwitchIcon.of(overrides[id]) ?: SwitchLabels.icon(device?.name.orEmpty(), device?.typeNames.orEmpty())

    private const val PER_ROW = 4

    @Composable
    private fun Dim(text: String, modifier: Modifier = Modifier) =
        Text(text, fontSize = 14.sp, color = StillColors.faint, modifier = modifier)
}
