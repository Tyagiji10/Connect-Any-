package com.example.connectany.runtime.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.connectany.core.time.TimeProvider
import com.example.connectany.domain.ConnectionStateMachine
import com.example.connectany.domain.DeviceIdentityResolver
import com.example.connectany.domain.ProfileConnectionVerifier
import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.domain.model.NormalizedConnectionEvent
import com.example.connectany.domain.model.ProfileType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothEventIngress : BroadcastReceiver() {

    @Inject lateinit var stateMachine: ConnectionStateMachine
    @Inject lateinit var identityResolver: DeviceIdentityResolver
    @Inject lateinit var profileVerifier: ProfileConnectionVerifier
    @Inject lateinit var timeProvider: TimeProvider

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        val address = device.address ?: return
        val name = try { device.name } catch (e: SecurityException) { null }

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val pendingResult = goAsync()
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val identity = identityResolver.resolve(address, name)
                        if (identity != null) {
                        
                        // Extract battery level via reflection
                        var batteryLevel: Int? = null
                        try {
                            val method = device.javaClass.getMethod("getBatteryLevel")
                            val level = method.invoke(device) as Int
                            if (level in 0..100) {
                                batteryLevel = level
                            }
                        } catch (e: Exception) {
                            // Unsupported
                        }

                        val connectingEvent = NormalizedConnectionEvent(
                            deviceIdentity = identity,
                            profileType = ProfileType.UNKNOWN,
                            state = ConnectionState.CONNECTING,
                            timestampMs = timeProvider.elapsedRealtime(),
                            rawAddress = address,
                            batteryLevel = batteryLevel
                        )
                        stateMachine.processEvent(connectingEvent)
                        
                        if (batteryLevel == null) {
                            // Battery level often takes a moment to be negotiated after ACL_CONNECTED
                            kotlinx.coroutines.delay(2000)
                            try {
                                val method = device.javaClass.getMethod("getBatteryLevel")
                                val level = method.invoke(device) as Int
                                if (level in 0..100) {
                                    batteryLevel = level
                                }
                            } catch (e: Exception) {}
                        }

                        // Assume connected and trigger popup on ACL connect.
                        val connectedEvent = connectingEvent.copy(
                            profileType = ProfileType.UNKNOWN,
                            state = ConnectionState.CONNECTED,
                            timestampMs = timeProvider.elapsedRealtime(),
                            batteryLevel = batteryLevel
                        )
                        stateMachine.processEvent(connectedEvent)
                    }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val pendingResult = goAsync()
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val identity = identityResolver.resolve(address, name)
                        if (identity != null) {
                        val disconnectEvent = NormalizedConnectionEvent(
                            deviceIdentity = identity,
                            profileType = ProfileType.UNKNOWN,
                            state = ConnectionState.DISCONNECTED,
                            timestampMs = timeProvider.elapsedRealtime(),
                            rawAddress = address
                        )
                        stateMachine.processEvent(disconnectEvent)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
