package com.gotlibsh.still.widgets.homecontrols

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.home.ConnectivityState
import com.google.home.DeviceType
import com.google.home.DeviceTypeFactory
import com.google.home.FactoryRegistry
import com.google.home.ForcePermissionFlow
import com.google.home.Home
import com.google.home.HomeClient
import com.google.home.HomeConfig
import com.google.home.HomeDevice
import com.google.home.PermissionsResultStatus
import com.google.home.PermissionsState
import com.google.home.Trait
import com.google.home.TraitFactory
import com.google.home.google.GoogleAirCoolerDevice
import com.google.home.google.GoogleBoilerDevice
import com.google.home.google.GoogleFireplaceDevice
import com.google.home.google.GoogleKettleDevice
import com.google.home.google.GooglePowerControlDevice
import com.google.home.google.GoogleRadiatorDevice
import com.google.home.google.GoogleVentilatorDevice
import com.google.home.google.GoogleWaterHeaterDevice
import com.google.home.google.OtherDeviceDevice
import com.google.home.matter.standard.AirPurifierDevice
import com.google.home.matter.standard.ColorTemperatureLightDevice
import com.google.home.matter.standard.DimmableLightDevice
import com.google.home.matter.standard.DimmablePlugInUnitDevice
import com.google.home.matter.standard.ExtendedColorLightDevice
import com.google.home.matter.standard.FanDevice
import com.google.home.matter.standard.GenericSwitchDevice
import com.google.home.matter.standard.HeatingCoolingUnitDevice
import com.google.home.matter.standard.MountedOnOffControlDevice
import com.google.home.matter.standard.OnOff
import com.google.home.matter.standard.OnOffLightDevice
import com.google.home.matter.standard.OnOffLightSwitchDevice
import com.google.home.matter.standard.OnOffPluginUnitDevice
import com.google.home.matter.standard.RoomAirConditionerDevice
import com.google.home.matter.standard.ThermostatDevice
import com.google.home.matter.standard.WaterHeaterDevice
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/** One Google Home device that can be switched on and off. */
data class HomeSwitch(
    val id: String,
    val name: String,
    /** null when Google Home doesn't know the current state. */
    val isOn: Boolean?,
    val online: Boolean,
    /** Google Home device type class names, e.g. "RoomAirConditionerDevice" (for the icon). */
    val typeNames: List<String> = emptyList(),
)

/**
 * Talks to your Google Home through Google's Home APIs. Only devices with an on/off
 * control are exposed. Access is granted once on Google's own permission screen.
 */
class GoogleHomeRepository(private val context: Context) {

    private val client: HomeClient by lazy {
        Home.getClient(
            context,
            HomeConfig(
                // Google Home runs its own background work here. A failure in it must never
                // take the launcher down, so log it instead of crashing.
                // No Job here: the SDK uses this context in flowOn(), which rejects one.
                coroutineContext = Dispatchers.IO +
                    CoroutineExceptionHandler { _, e -> Log.w("StillGoogleHome", "Google Home background error", e) },
                factoryRegistry = FactoryRegistry(traits = TRAITS, types = DEVICE_TYPES),
            ),
        )
    }

    /** Latest on/off control per device id, used to send commands. */
    private val controls = ConcurrentHashMap<String, OnOff>()

    /** Must be called from Activity.onCreate, before the activity is started. */
    fun registerForPermissions(activity: ComponentActivity) {
        runCatching { client.registerActivityResultCallerForPermissions(activity) }
    }

    val permissionState: Flow<PermissionsState>
        get() = runCatching { client.hasPermissions() }
            .getOrElse { flowOf(PermissionsState.PERMISSIONS_STATE_UNAVAILABLE) }
            .catch { emit(PermissionsState.PERMISSIONS_STATE_UNAVAILABLE) }

    /** Shows Google's permission screen. Returns null if access was granted, otherwise why not. */
    suspend fun requestAccess(): String? = try {
        val result = client.requestPermissions(ForcePermissionFlow.FORCE_LAUNCH)
        if (result.status == PermissionsResultStatus.SUCCESS) null
        else "Google said: ${result.status}${result.errorMessage?.let { " ($it)" } ?: ""}"
    } catch (e: Throwable) {
        Log.w("StillGoogleHome", "requestPermissions failed", e)
        "Google Home error: ${e.message ?: e.javaClass.simpleName}"
    }

    /** All on/off-capable devices, kept up to date as they change. Empty if not connected. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun switches(): Flow<List<HomeSwitch>> =
        (runCatching { client.devices() }.getOrNull() ?: return flowOf(emptyList()))
            .flatMapLatest { devices ->
                if (devices.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(devices.map { device -> device.types().map { types -> toSwitch(device, types) } }) { all ->
                        all.filterNotNull().sortedBy { it.name.lowercase() }
                    }
                }
            }
            .catch { emit(emptyList()) }

    // --- One shared subscription for the home screen ---------------------------------------
    // It lives here, not in the widget, so moving between home, the drawer and settings keeps
    // it. It closes 2 minutes after the home screen stops needing it. Main thread only.

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _live = MutableStateFlow<List<HomeSwitch>?>(null)

    /** Latest device states for the home screen; null until the first answer arrives. */
    val live: StateFlow<List<HomeSwitch>?> = _live.asStateFlow()
    private var liveJob: Job? = null
    private var pendingStop: Job? = null

    /** The home screen is showing switches: make sure we're listening. */
    fun watch() {
        pendingStop?.cancel()
        pendingStop = null
        if (liveJob?.isActive != true) liveJob = scope.launch { switches().collect { _live.value = it } }
    }

    /** The home screen went away: stop listening if it doesn't come back soon. */
    fun unwatchSoon() {
        if (pendingStop?.isActive == true) return
        pendingStop = scope.launch {
            delay(KEEP_WHILE_AWAY_MS)
            liveJob?.cancel()
            liveJob = null
        }
    }

    /** Reads everything again right now (after a tap). Keeps showing the old states meanwhile. */
    fun reread() {
        liveJob?.cancel()
        liveJob = null
        watch()
    }

    /** Turns a device on or off. Returns false if it failed. */
    suspend fun setOn(id: String, on: Boolean): Boolean {
        val control = controls[id] ?: return false
        return try {
            if (on) control.on() else control.off()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun toSwitch(device: HomeDevice, types: Set<DeviceType>): HomeSwitch? {
        val onOff = types.asSequence()
            .flatMap { it.traits().asSequence() }
            .filterIsInstance<OnOff>()
            .firstOrNull() ?: return null
        val id = device.id.id
        controls[id] = onOff
        return HomeSwitch(
            id = id,
            name = device.name,
            isOn = onOff.onOff,
            online = device.sourceConnectivity.connectivityState != ConnectivityState.OFFLINE,
            typeNames = types.mapNotNull { it::class.simpleName },
        )
    }

    private companion object {
        const val KEEP_WHILE_AWAY_MS = 2 * 60 * 1000L

        val TRAITS: List<TraitFactory<out Trait>> = listOf(OnOff)

        /** Device kinds that commonly have an on/off control (ACs, boilers, plugs, lights…). */
        val DEVICE_TYPES: List<DeviceTypeFactory<out DeviceType>> = listOf(
            RoomAirConditionerDevice,
            HeatingCoolingUnitDevice,
            ThermostatDevice,
            WaterHeaterDevice,
            AirPurifierDevice,
            FanDevice,
            OnOffPluginUnitDevice,
            DimmablePlugInUnitDevice,
            OnOffLightSwitchDevice,
            GenericSwitchDevice,
            MountedOnOffControlDevice,
            OnOffLightDevice,
            DimmableLightDevice,
            ColorTemperatureLightDevice,
            ExtendedColorLightDevice,
            GoogleBoilerDevice,
            GoogleWaterHeaterDevice,
            GooglePowerControlDevice,
            GoogleAirCoolerDevice,
            GoogleRadiatorDevice,
            GoogleVentilatorDevice,
            GoogleKettleDevice,
            GoogleFireplaceDevice,
            OtherDeviceDevice,
        )
    }
}
