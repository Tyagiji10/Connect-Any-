package com.example.connectany.runtime.overlay

import android.content.Intent
import android.content.Context
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Recomposer
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
import com.example.connectany.presentation.popup.MagneticPopup
import com.example.connectany.presentation.popup.GamingPopup
import com.example.connectany.presentation.popup.MinimalPopup
import com.example.connectany.theme.ConnectAnyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
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
    private var hideJob: kotlinx.coroutines.Job? = null

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()

        coroutineScope.launch {
            stateMachine.currentState.collectLatest { state ->
                if (state == ConnectionState.TRIGGERED) {
                    try {
                        val event = stateMachine.latestEvent.value
                        if (event != null) {
                            // Read from Room DeviceDao
                            val deviceEntity = try { deviceDao.getDeviceByMac(event.rawAddress) } catch (e: Exception) { null }

                            if (deviceEntity?.isEnabled == false) return@collectLatest

                            val isConnected = event.state == ConnectionState.CONNECTED
                            if (isConnected && deviceEntity?.showOnConnect == false) return@collectLatest
                            if (!isConnected && deviceEntity?.showOnDisconnect == false) return@collectLatest

                            if (isConnected) {
                                // Smart Volume
                                deviceEntity?.smartVolumeLevel?.let { targetVolumePercent ->
                                    try {
                                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                                        audioManager?.let { am ->
                                            val maxVolume = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                            if (maxVolume > 0) {
                                                val targetIndex = (targetVolumePercent * maxVolume / 100.0).toInt()
                                                am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetIndex, 0)
                                            }
                                        }
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                                
                                // Auto-Launch App
                                deviceEntity?.autoLaunchPackage?.let { pkg ->
                                    try {
                                        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                                        if (launchIntent != null) {
                                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            startActivity(launchIntent)
                                        }
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            }

                            // Vibration
                            if (deviceEntity?.vibration == true) {
                                try {
                                    @Suppress("DEPRECATION")
                                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                                } catch (e: Exception) { /* vibration not available */ }
                            }
                            
                            // Sound
                            if (deviceEntity?.playSound == true) {
                                try {
                                    val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                                    val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, uri)
                                    ringtone?.play()
                                } catch (e: Exception) { /* sound not available */ }
                            }

                            val duration = deviceEntity?.durationMs ?: 5000L
                            val battery = if (deviceEntity?.showBattery != false) event.batteryLevel else null

                            showOverlay(
                                deviceName = deviceEntity?.name ?: event.deviceIdentity?.normalizedName ?: "Unknown Device",
                                deviceType = deviceEntity?.deviceType ?: event.deviceIdentity?.deviceType?.name ?: "Device",
                                style = deviceEntity?.popupStyle ?: "Drop",
                                imageUri = deviceEntity?.imageUri,
                                batteryLevel = battery,
                                isConnected = isConnected,
                                duration = duration,
                                colorIndex = deviceEntity?.popupColor ?: android.graphics.Color.DKGRAY
                            )
                            
                            // Trigger Widget Update
                            val widgetIntent = Intent(this@ConnectAnyOverlayService, com.example.connectany.runtime.widget.ConnectedDevicesWidgetReceiver::class.java).apply {
                                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                val ids = android.appwidget.AppWidgetManager.getInstance(applicationContext)
                                    .getAppWidgetIds(android.content.ComponentName(applicationContext, com.example.connectany.runtime.widget.ConnectedDevicesWidgetReceiver::class.java))
                                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                            }
                            sendBroadcast(widgetIntent)
                        }
                    } catch (e: Exception) {
                        // Prevent service crash from any individual event processing error
                        e.printStackTrace()
                    }
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

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Connect Any")
            .setContentText("Listening for device connections...")
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Fallback icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
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

    private fun showOverlay(deviceName: String, deviceType: String, style: String, imageUri: String?, batteryLevel: Int?, isConnected: Boolean, duration: Long, colorIndex: Int) {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ConnectAnyOverlayService)
            setViewTreeViewModelStoreOwner(this@ConnectAnyOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ConnectAnyOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            
            setContent {
                val theme = com.example.connectany.presentation.popup.getPopupThemeByColor(colorIndex)
                ConnectAnyTheme {
                    when {
                        style.equals("Gaming", ignoreCase = true) -> {
                            GamingPopup(deviceName = deviceName, deviceType = deviceType, batteryLevel = batteryLevel, theme = theme, imageUri = imageUri, isConnected = isConnected, displayDurationMs = duration, onAnimationComplete = {})
                        }
                        style.equals("Minimal", ignoreCase = true) -> {
                            MinimalPopup(deviceName = deviceName, batteryLevel = batteryLevel, theme = theme, isConnected = isConnected, displayDurationMs = duration, onAnimationComplete = {})
                        }
                        style.equals("Magnetic", ignoreCase = true) -> {
                            MagneticPopup(deviceName = deviceName, deviceType = deviceType, batteryLevel = batteryLevel, theme = theme, imageUri = imageUri, isConnected = isConnected, displayDurationMs = duration, onAnimationComplete = {})
                        }
                        style.equals("Glass", ignoreCase = true) -> {
                            GlassPopup(deviceName = deviceName, deviceType = deviceType, batteryLevel = batteryLevel, theme = theme, imageUri = imageUri, isConnected = isConnected, displayDurationMs = duration, onAnimationComplete = {})
                        }
                        else -> {
                            DropPopup(deviceName = deviceName, deviceType = deviceType, batteryLevel = batteryLevel, theme = theme, imageUri = imageUri, isConnected = isConnected, displayDurationMs = duration, onAnimationComplete = {})
                        }
                    }
                }
            }
        }

        // Attach Recomposer
        val recomposer = Recomposer(coroutineScope.coroutineContext)
        composeView.compositionContext = recomposer
        coroutineScope.launch { recomposer.runRecomposeAndApplyChanges() }

        overlayGateway.setContent(composeView)
        overlayGateway.showOverlay()

        // Hide overlay after duration, cancelling any previous hide job
        hideJob?.cancel()
        hideJob = coroutineScope.launch {
            delay(duration)
            overlayGateway.hideOverlay()
        }
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
