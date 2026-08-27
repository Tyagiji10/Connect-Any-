package com.example.connectany

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.Settings
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.connectany.presentation.navigation.ConnectAnyNavGraph
import com.example.connectany.theme.ConnectAnyTheme
import com.example.connectany.runtime.overlay.ConnectAnyOverlayService
import com.example.connectany.data.settings.SettingsRepository
import com.example.connectany.data.settings.ThemeOption
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject lateinit var settingsRepository: SettingsRepository
    
    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(this)) {
            // Service will be started by the LaunchedEffect if isListening is true
        }
    }
    
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()


    setContent {
      val settings by settingsRepository.settingsFlow.collectAsState(initial = null)

      // Request overlay permission once settings are loaded (not during startup)
      androidx.compose.runtime.LaunchedEffect(settings) {
          if (settings != null && !Settings.canDrawOverlays(this@MainActivity)) {
              val intent = Intent(
                  Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                  android.net.Uri.parse("package:$packageName")
              )
              try { overlayPermissionLauncher.launch(intent) } catch (e: Exception) { }
          }
      }
      
      // Manage Foreground Service based on isListening state
      androidx.compose.runtime.LaunchedEffect(settings?.isListening) {
          val hasBluetoothPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
              androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
          } else true

          if (settings?.isListening == true && Settings.canDrawOverlays(this@MainActivity) && hasBluetoothPermission) {
              try {
                  val serviceIntent = Intent(this@MainActivity, ConnectAnyOverlayService::class.java)
                  if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                      startForegroundService(serviceIntent)
                  } else {
                      startService(serviceIntent)
                  }
              } catch (e: Exception) { e.printStackTrace() }
          } else if (settings?.isListening == false || !hasBluetoothPermission) {
              try {
                  val serviceIntent = Intent(this@MainActivity, ConnectAnyOverlayService::class.java)
                  stopService(serviceIntent)
              } catch (e: Exception) { }
          }
      }
      
      val darkTheme = when (settings?.theme) {
          ThemeOption.DARK -> true
          ThemeOption.LIGHT -> false
          else -> isSystemInDarkTheme()
      }
      
      ConnectAnyTheme(darkTheme = darkTheme) { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { ConnectAnyNavGraph() } }
    }
  }
}
