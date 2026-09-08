package com.example.connectany.domain.usecase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.connectany.data.bluetooth.BluetoothDataSource
import com.example.connectany.data.local.dao.DeviceDao
import com.example.connectany.domain.model.BluetoothListState
import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.domain.model.ConnectAnyDeviceUiModel
import com.example.connectany.domain.ConnectionStateMachine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetBondedDevicesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothDataSource: BluetoothDataSource,
    private val deviceDao: DeviceDao,
    private val connectionStateMachine: ConnectionStateMachine
) {
    operator fun invoke(): Flow<BluetoothListState> {
        return combine(
            bluetoothDataSource.observeBluetoothState(),
            deviceDao.getAllDevices(),
            connectionStateMachine.latestEvent
        ) { _, savedDevices, latestEvent ->
            
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            }

            if (!hasPermission) {
                return@combine BluetoothListState.MissingPermission
            }

            // 2. Check Bluetooth State
            if (!bluetoothDataSource.isBluetoothEnabled()) {
                return@combine BluetoothListState.BluetoothDisabled
            }

            // 3. Get Bonded Devices
            val systemDevices = bluetoothDataSource.getBondedDevices()
            if (systemDevices.isEmpty()) {
                return@combine BluetoothListState.Empty
            }

            // 4. Merge Profiles (O(n) index build, O(1) lookup)
            val profileMap = savedDevices.associateBy { it.macAddress }

            val uiModels = systemDevices.map { sysDevice ->
                val profile = profileMap[sysDevice.stableKey]
                
                val isConnected = sysDevice.isConnected
                
                ConnectAnyDeviceUiModel(
                    deviceKey = sysDevice.stableKey,
                    deviceName = sysDevice.name ?: "Unknown Device",
                    systemDeviceType = sysDevice.deviceType,
                    paired = sysDevice.isBonded,
                    connectionState = if (isConnected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED,
                    connectanyEnabled = profile?.isEnabled ?: false,
                    customImageUri = profile?.imageUri,
                    popupStyle = profile?.popupStyle,
                    autoLaunchPackage = profile?.autoLaunchPackage,
                    smartVolumeLevel = profile?.smartVolumeLevel
                )
            }

            val sortedModels = uiModels.sortedWith(
                compareByDescending<ConnectAnyDeviceUiModel> { it.connectanyEnabled }
                    .thenByDescending { it.connectionState == ConnectionState.CONNECTED }
            )
            BluetoothListState.Success(sortedModels)
        }
    }
}
