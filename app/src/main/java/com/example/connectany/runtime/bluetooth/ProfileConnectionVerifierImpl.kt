package com.example.connectany.runtime.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.example.connectany.domain.ProfileConnectionVerifier
import com.example.connectany.domain.model.ProfileType
import kotlinx.coroutines.suspendCancellableCoroutine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume

class ProfileConnectionVerifierImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ProfileConnectionVerifier {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    @SuppressLint("MissingPermission") // Permissions handled at UI level
    override suspend fun verifyConnection(address: String, profile: ProfileType): Boolean {
        val adapter = bluetoothManager?.adapter ?: return false
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            return false
        }

        val profileId = when (profile) {
            ProfileType.A2DP -> BluetoothProfile.A2DP
            ProfileType.HEADSET -> BluetoothProfile.HEADSET
            ProfileType.HID -> 19 // BluetoothProfile.HID_DEVICE (API 28+)
            else -> return false
        }

        return suspendCancellableCoroutine { continuation ->
            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val isConnected = proxy.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED
                    adapter.closeProfileProxy(profile, proxy)
                    if (continuation.isActive) {
                        continuation.resume(isConnected)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }
            val success = adapter.getProfileProxy(context, listener, profileId)
            if (!success && continuation.isActive) {
                continuation.resume(false)
            }
        }
    }
}
