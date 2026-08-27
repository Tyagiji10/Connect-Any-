package com.example.connectany.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.connectany.domain.model.DeviceType
import com.example.connectany.domain.model.SystemBluetoothDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun getBondedDevices(): List<SystemBluetoothDevice> {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return emptyList()
        
        val bonded = try {
            bluetoothAdapter.bondedDevices ?: return emptyList()
        } catch (e: SecurityException) {
            return emptyList()
        }
        
        // Use reflection to determine actual connection state, bypassing Profile limits
        fun isDeviceConnected(device: BluetoothDevice): Boolean {
            return try {
                val method = device.javaClass.getMethod("isConnected")
                method.invoke(device) as? Boolean ?: false
            } catch (e: Exception) {
                false
            }
        }

        return bonded.map { device ->
            SystemBluetoothDevice(
                stableKey = device.address,
                name = device.name ?: "Unknown Device",
                deviceType = mapDeviceClassToType(device.bluetoothClass?.deviceClass),
                isBonded = true,
                isConnected = isDeviceConnected(device)
            )
        }
    }

    fun observeBluetoothState(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        try {
            androidx.core.content.ContextCompat.registerReceiver(
                context, 
                receiver, 
                filter, 
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        trySend(Unit) // Initial emission

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Already unregistered
            }
        }
    }

    private fun mapDeviceClassToType(deviceClass: Int?): DeviceType {
        if (deviceClass == null) return DeviceType.OTHER
        // 0x400 = Audio/Video
        // 0x418 = Headphones
        // 0x41C = Portable Audio (Earbuds)
        // 0x414 = Loudspeaker
        // 0x420 = Car audio
        // 0x704 = Wearable (Watch)
        return when (deviceClass) {
            0x418 -> DeviceType.HEADPHONES
            0x41C -> DeviceType.EARBUDS
            0x414 -> DeviceType.SPEAKER
            0x420 -> DeviceType.CAR_AUDIO
            0x704 -> DeviceType.WATCH
            0x540 -> DeviceType.KEYBOARD // Peripheral Keyboard
            0x508 -> DeviceType.CONTROLLER // Peripheral Gamepad
            0x10C -> DeviceType.LAPTOP // Computer Laptop
            else -> DeviceType.OTHER
        }
    }
}
