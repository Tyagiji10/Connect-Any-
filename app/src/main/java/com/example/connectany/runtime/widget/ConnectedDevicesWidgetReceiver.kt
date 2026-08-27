package com.example.connectany.runtime.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.example.connectany.data.bluetooth.BluetoothDataSource
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
    lateinit var bluetoothDataSource: BluetoothDataSource
    @Inject
    lateinit var deviceDao: DeviceDao

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidget(context)
    }

    fun updateWidget(context: Context) {
        scope.launch {
            try {
                // Manually fetch connected devices
                val systemDevices = bluetoothDataSource.getBondedDevices()
                val connectedSystemDevices = systemDevices.filter { it.isConnected }
                
                // Get display names
                val savedDevices = deviceDao.getAllDevicesSync()
                val profileMap = savedDevices.associateBy { it.macAddress }
                
                val widgetData = connectedSystemDevices.map { sysDevice ->
                    val profile = profileMap[sysDevice.stableKey]
                    WidgetDeviceData(
                        name = profile?.name ?: sysDevice.name ?: "Unknown Device",
                        batteryLevel = null // SysDevice doesn't expose battery level yet in the direct list, we might need a workaround or just show Connected.
                    )
                }
                
                ConnectedDevicesWidget(widgetData).updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
