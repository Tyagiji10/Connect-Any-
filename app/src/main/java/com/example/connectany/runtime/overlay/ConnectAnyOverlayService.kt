package com.example.connectany.runtime.overlay

import android.content.Intent
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.connectany.domain.ConnectionStateMachine
import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.data.local.dao.DeviceDao
import com.example.connectany.presentation.popup.DropPopup
import com.example.connectany.presentation.popup.GlassPopup
import com.example.connectany.presentation.popup.GamingPopup
import com.example.connectany.presentation.popup.MinimalPopup
import com.example.connectany.theme.ConnectAnyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import javax.inject.Inject

@AndroidEntryPoint
class ConnectAnyOverlayService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    @Inject lateinit var stateMachine: ConnectionStateMachine
    @Inject lateinit var overlayGateway: AndroidOverlayGateway
    @Inject lateinit var deviceDao: DeviceDao

    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val coroutineScope = CoroutineScope(AndroidUiDispatcher.Main)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()

        // The Channel guarantees sequential delivery and processing.
        coroutineScope.launch {
            stateMachine.popupEvents.collect { event ->
                try {
                    val deviceEntity = try { deviceDao.getDeviceByMac(event.rawAddress) } catch (e: Exception) { null }

                    if (deviceEntity?.isEnabled == false) return@collect // skips this event

                    val isConnected = event.state == ConnectionState.CONNECTED
                    if (isConnected && deviceEntity?.showOnConnect == false) return@collect
                    if (!isConnected && deviceEntity?.showOnDisconnect == false) return@collect

                    if (isConnected) {
                        coroutineScope.launch {
                            applySmartVolume(deviceEntity?.smartVolumeLevel, event.rawAddress)
                        }
                        launchAutoApp(deviceEntity?.autoLaunchPackage)
                    }

                    performVibration(deviceEntity?.vibration == true)
                    performSound(deviceEntity?.playSound == true)

                    val duration = deviceEntity?.durationMs ?: 5000L
                    val battery = if (deviceEntity?.showBattery != false) event.batteryLevel else null

                    // Trigger Widget Update
                    val widgetIntent = Intent(this@ConnectAnyOverlayService, com.example.connectany.runtime.widget.ConnectedDevicesWidgetReceiver::class.java).apply {
                        action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        val ids = android.appwidget.AppWidgetManager.getInstance(applicationContext)
                            .getAppWidgetIds(android.content.ComponentName(applicationContext, com.example.connectany.runtime.widget.ConnectedDevicesWidgetReceiver::class.java))
                        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    sendBroadcast(widgetIntent)

                    // Show the popup overlay
                    showOverlaySync(
                        deviceName = deviceEntity?.name ?: event.deviceIdentity?.normalizedName ?: "Unknown Device",
                        deviceType = deviceEntity?.deviceType ?: event.deviceIdentity?.deviceType?.name ?: "Device",
                        style = deviceEntity?.popupStyle ?: "Drop",
                        imageUri = deviceEntity?.imageUri,
                        batteryLevel = battery,
                        isConnected = isConnected,
                        duration = duration,
                        colorIndex = deviceEntity?.popupColor ?: android.graphics.Color.DKGRAY,
                        showGlow = deviceEntity?.showGlow ?: false,
                        rawAddress = event.rawAddress,
                        showBattery = deviceEntity?.showBattery != false
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val channelId = "connect_any_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Connection Listener",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, com.example.connectany.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }, android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Connect Any")
            .setContentText("Listening for device connections...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }

        return START_STICKY
    }

    // Creates a fresh ComposeView for each popup event. Gateway removes previous view via setContent().
    // This function suspends for the duration of the popup + 1s gap.
    private suspend fun showOverlaySync(
        deviceName: String,
        deviceType: String,
        style: String,
        imageUri: String?,
        batteryLevel: Int?,
        isConnected: Boolean,
        duration: Long,
        colorIndex: Int,
        showGlow: Boolean,
        rawAddress: String?,
        showBattery: Boolean
    ) {
        val completion = kotlinx.coroutines.CompletableDeferred<Unit>()
        
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ConnectAnyOverlayService)
            setViewTreeViewModelStoreOwner(this@ConnectAnyOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ConnectAnyOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val currentEvent by stateMachine.latestEvent.collectAsState()
                val currentBattery = if (showBattery) {
                    if (currentEvent?.rawAddress == rawAddress) currentEvent?.batteryLevel ?: batteryLevel else batteryLevel
                } else null
                
                val theme = com.example.connectany.presentation.popup.getPopupThemeByColor(colorIndex)
                val onComplete = { completion.complete(Unit); Unit }
                ConnectAnyTheme {
                    when {
                        style.equals("Gaming", ignoreCase = true) ->
                            GamingPopup(deviceName = deviceName, deviceType = deviceType, batteryLevel = currentBattery, theme = theme, imageUri = imageUri, isConnected = isConnected, displayDurationMs = duration, showGlow = showGlow, onAnimationComplete = onComplete)
                        style.equals("Minimal", ignoreCase = true) ->
                            MinimalPopup(deviceName = deviceName, batteryLevel = currentBattery, theme = theme, isConnected = isConnected, displayDurationMs = duration, showGlow = showGlow, onAnimationComplete = onComplete)
                        style.equals("Glass", ignoreCase = true) ->
                            GlassPopup(deviceName = deviceName, deviceType = deviceType, batteryLevel = currentBattery, theme = theme, imageUri = imageUri, isConnected = isConnected, displayDurationMs = duration, showGlow = showGlow, onAnimationComplete = onComplete)
                        else ->
                            DropPopup(deviceName = deviceName, deviceType = deviceType, batteryLevel = currentBattery, theme = theme, imageUri = imageUri, isConnected = isConnected, displayDurationMs = duration, showGlow = showGlow, onAnimationComplete = onComplete)
                    }
                }
            }
        }

        val recomposer = Recomposer(coroutineScope.coroutineContext)
        composeView.compositionContext = recomposer
        coroutineScope.launch { recomposer.runRecomposeAndApplyChanges() }

        overlayGateway.setContent(composeView)
        overlayGateway.showOverlay()

        // Wait for the popup to finish its display duration OR dismiss early
        kotlinx.coroutines.withTimeoutOrNull(duration + 1000L) {
            completion.await()
        }
        
        // Hide the popup
        overlayGateway.hideOverlay()
        
        // Wait 1 second before allowing the next popup to show
        delay(1000)
    }

    private suspend fun applySmartVolume(targetVolumePercent: Int?, address: String) {
        targetVolumePercent ?: return
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            
            var isActiveOutput = false
            for (i in 0 until 15) { // Poll for up to 7.5 seconds
                delay(500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    val a2dpOutputs = outputs.filter { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
                    
                    val isConnectedDeviceActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        a2dpOutputs.any { it.address.equals(address, ignoreCase = true) }
                    } else {
                        a2dpOutputs.isNotEmpty()
                    }
                    
                    if (isConnectedDeviceActive) {
                        isActiveOutput = true
                        break
                    }
                } else {
                    isActiveOutput = true
                    break
                }
            }

            if (isActiveOutput) {
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (maxVolume > 0) {
                    val targetIndex = (targetVolumePercent * maxVolume / 100.0).toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun launchAutoApp(pkg: String?) {
        pkg ?: return
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun performVibration(enabled: Boolean) {
        if (!enabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) { }
    }

    private fun performSound(enabled: Boolean) {
        if (!enabled) return
        try {
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        store.clear()
        coroutineScope.cancel()
        overlayGateway.hideOverlay()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
