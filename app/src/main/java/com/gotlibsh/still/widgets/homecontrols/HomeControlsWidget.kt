package com.gotlibsh.still.widgets.homecontrols

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
 * A few Google Home devices as plain text switches, e.g. "Living room AC   off".
 * Tap a line to flip it. Pick the devices in Settings > Google Home.
 */
object HomeControlsWidget : HomeWidget {
    override val id = "home_controls"
    override val title = "Google Home switches"
    override val enabledByDefault = false

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

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ids.forEach { id ->
                val device = switches?.firstOrNull { it.id == id }
                val wanted = pending[id]
                val state = when {
                    switches == null -> "…"
                    device == null -> "missing"
                    !device.online -> "offline"
                    wanted != null -> if (wanted) "on…" else "off…"
                    device.isOn == true -> "on"
                    device.isOn == false -> "off"
                    else -> "?"
                }
                val isOn = (wanted ?: device?.isOn) == true
                Row(
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
                    Text(device?.name ?: "Device", fontSize = 18.sp, color = StillColors.text)
                    Spacer(Modifier.width(12.dp))
                    Text(state, fontSize = 18.sp, color = if (isOn) StillColors.text else StillColors.faint)
                }
            }
        }
    }

    @Composable
    private fun Dim(text: String, modifier: Modifier = Modifier) =
        Text(text, fontSize = 14.sp, color = StillColors.faint, modifier = modifier)
}
