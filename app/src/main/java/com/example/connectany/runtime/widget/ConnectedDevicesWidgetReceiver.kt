package com.example.connectany.runtime.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.example.connectany.data.local.dao.DeviceDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConnectedDevicesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ConnectedDevicesWidget(emptyList())

    @Inject
    lateinit var deviceDao: DeviceDao
    
    @Inject
    lateinit var stateMachine: com.example.connectany.domain.ConnectionStateMachine

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidget(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        job.cancel() // Prevent coroutine leaks when widget is removed
    }

    fun updateWidget(context: Context) {
        scope.launch {
            try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter: BluetoothAdapter? = bluetoothManager?.adapter
                if (adapter == null || !adapter.isEnabled) {
                    ConnectedDevicesWidget(emptyList()).updateAll(context)
                    return@launch
                }

                @SuppressLint("MissingPermission")
                val bondedDevices: Set<BluetoothDevice> = try {
                    adapter.bondedDevices ?: emptySet()
                } catch (e: SecurityException) {
                    emptySet()
                }

                // Filter to actually connected devices using the same reflection trick as BluetoothDataSource
                @SuppressLint("MissingPermission")
                val connectedDevices = bondedDevices.filter { device ->
                    try {
                        val method = device.javaClass.getMethod("isConnected")
                        method.invoke(device) as? Boolean ?: false
                    } catch (e: Exception) { false }
                }

                // Get saved profiles for custom names
                val savedDevices = deviceDao.getAllDevicesSync()
                val profileMap = savedDevices.associateBy { it.macAddress }

                val widgetData = connectedDevices.mapNotNull { device ->
                    @SuppressLint("MissingPermission")
                    val address = device.address
                    @SuppressLint("MissingPermission")
                    val deviceName = try { device.name } catch (e: SecurityException) { null }
                    val profile = profileMap[address]

                    // Fetch battery level via the hidden getBatteryLevel() API.
                    // Only devices implementing GATT Battery Service or HFP battery extension
                    // return a value in 0..100; others return -1.
                    var batteryLevel: Int? = try {
                        val method = device.javaClass.getMethod("getBatteryLevel")
                        val level = method.invoke(device) as? Int ?: -1
                        if (level in 0..100) level else null
                    } catch (e: Exception) { null }

                    if (batteryLevel == null) {
                        batteryLevel = stateMachine.getBatteryLevel(address)
                    }

                    // Do not return null if batteryLevel is null; just display without it
                    WidgetDeviceData(
                        name = profile?.name ?: deviceName ?: "Unknown Device",
                        batteryLevel = batteryLevel
                    )
                }

                ConnectedDevicesWidget(widgetData).updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
