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
import kotlinx.coroutines.Dispatchers
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
                coroutineContext = Dispatchers.IO,
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

    /** Shows Google's permission screen. Returns true if access was granted. */
    suspend fun requestAccess(): Boolean = try {
        client.requestPermissions(ForcePermissionFlow.FORCE_LAUNCH).status == PermissionsResultStatus.SUCCESS
    } catch (e: Exception) {
        false
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
        )
    }

    private companion object {
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
