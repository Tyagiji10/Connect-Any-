package com.example.connectany.runtime.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import com.example.connectany.core.time.TimeProvider
import com.example.connectany.domain.ConnectionStateMachine
import com.example.connectany.domain.DeviceIdentityResolver
import com.example.connectany.domain.ProfileConnectionVerifier
import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.domain.model.NormalizedConnectionEvent
import com.example.connectany.domain.model.ProfileType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothEventIngress : BroadcastReceiver() {

    @Inject lateinit var stateMachine: ConnectionStateMachine
    @Inject lateinit var identityResolver: DeviceIdentityResolver
    @Inject lateinit var profileVerifier: ProfileConnectionVerifier
    @Inject lateinit var timeProvider: TimeProvider
    @Inject lateinit var batteryLogDao: com.example.connectany.data.local.dao.BatteryLogDao

    // A proper scope that is not GlobalScope — avoids coroutine leaks.
    // The HandlerThread survives multiple onReceive() calls as a static companion.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        val address = device.address ?: return
        
        val hasPermission = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
        val name = if (hasPermission) {
            try { device.name } catch (e: SecurityException) { null }
        } else null

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val identity = identityResolver.resolve(address, name) ?: return@launch

                        // ── Step 1: Try battery immediately ──────────────────────────
                        var batteryLevel = fetchBatteryLevel(device)

                        if (batteryLevel != null) {
                            // Battery available right away → fire popup instantly
                            fireConnected(identity, address, batteryLevel)
                        } else {
                            // Battery not yet negotiated.
                            // Fire popup immediately (without battery), then retry in 2s
                            // to see if the device supports battery reporting.
                            fireConnected(identity, address, batteryLevel = null)

                            // ── Step 2: Async battery retry (silent update) ──────────
                            delay(2000)
                            val retried = fetchBatteryLevel(device)
                            if (retried != null) {
                                // Update the event's battery level in-place — no new popup
                                stateMachine.updateBatteryLevel(address, retried)
                                triggerWidgetUpdate(context)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val identity = identityResolver.resolve(address, name) ?: return@launch
                        val disconnectEvent = NormalizedConnectionEvent(
                            deviceIdentity = identity,
                            profileType = ProfileType.UNKNOWN,
                            state = ConnectionState.DISCONNECTED,
                            timestampMs = timeProvider.elapsedRealtime(),
                            rawAddress = address
                        )
                        stateMachine.processEvent(disconnectEvent)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val level = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                        if (level in 0..100) {
                            stateMachine.updateBatteryLevel(address, level)
                            
                            val now = System.currentTimeMillis()
                            val latestLog = batteryLogDao.getLatestLogForDevice(address)
                            if (latestLog == null || latestLog.batteryLevel != level || (now - latestLog.timestampMs > 60_000)) {
                                batteryLogDao.insert(com.example.connectany.data.local.entity.BatteryLogEntity(macAddress = address, timestampMs = now, batteryLevel = level))
                            }
                            
                            if (level <= 15) {
                                val alertedRecently = latestLog != null && latestLog.batteryLevel <= 15 && (now - latestLog.timestampMs < 30 * 60_000)
                                if (!alertedRecently) {
                                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                    val name = try { device?.name ?: "Device" } catch (e: SecurityException) { "Device" }
                                    showLowBatteryNotification(context, address, name, level)
                                }
                            }
                            
                            triggerWidgetUpdate(context)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun triggerWidgetUpdate(context: Context) {
        val widgetIntent = Intent(context, com.example.connectany.runtime.widget.ConnectedDevicesWidgetReceiver::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = android.appwidget.AppWidgetManager.getInstance(context)
                .getAppWidgetIds(android.content.ComponentName(context, com.example.connectany.runtime.widget.ConnectedDevicesWidgetReceiver::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(widgetIntent)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────


    private fun fetchBatteryLevel(device: BluetoothDevice): Int? {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            if (level in 0..100) level else null
        } catch (e: Throwable) {
            null
        }
    }

    private fun fireConnected(
        identity: com.example.connectany.domain.model.DeviceIdentity,
        address: String,
        batteryLevel: Int?
    ) {
        val event = NormalizedConnectionEvent(
            deviceIdentity = identity,
            profileType = ProfileType.UNKNOWN,
            state = ConnectionState.CONNECTED,
            timestampMs = timeProvider.elapsedRealtime(),
            rawAddress = address,
            batteryLevel = batteryLevel
        )
        stateMachine.processEvent(event)
    }

    private fun showLowBatteryNotification(context: Context, address: String, deviceName: String, batteryLevel: Int) {
        val channelId = "battery_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Battery Alerts", android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts when a connected device has low battery"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(context, com.example.connectany.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, address.hashCode(), intent, android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Low Battery: $deviceName")
            .setContentText("Battery is at $batteryLevel%. Please charge soon.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(address.hashCode(), notification)
            }
        } else {
            notificationManager.notify(address.hashCode(), notification)
        }
    }
}

