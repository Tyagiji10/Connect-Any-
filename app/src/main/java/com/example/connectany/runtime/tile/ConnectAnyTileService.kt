package com.example.connectany.runtime.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.connectany.data.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class ConnectAnyTileService : TileService() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val currentSettings = settingsRepository.settingsFlow.first()
            val newState = !currentSettings.isListening
            settingsRepository.setListening(newState)
            
            updateTileState(newState)
        }
    }

    private fun updateTileState(isListening: Boolean? = null) {
        val tile = qsTile ?: return
        
        if (isListening != null) {
            applyState(tile, isListening)
        } else {
            scope.launch {
                val settings = settingsRepository.settingsFlow.first()
                applyState(tile, settings.isListening)
            }
        }
    }
    
    private fun applyState(tile: Tile, isListening: Boolean) {
        tile.state = if (isListening) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isListening) "Listening" else "Stopped"
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
