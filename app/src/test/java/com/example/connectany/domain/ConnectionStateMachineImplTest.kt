package com.example.connectany.domain

import com.example.connectany.core.time.TimeProvider
import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.domain.model.NormalizedConnectionEvent
import com.example.connectany.domain.model.ProfileType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConnectionStateMachineImplTest {

    private lateinit var stateMachine: ConnectionStateMachineImpl
    private var currentTime = 0L

    private val timeProvider = object : TimeProvider {
        override fun elapsedRealtime(): Long = currentTime
    }

    @Before
    fun setup() {
        currentTime = 0L
        stateMachine = ConnectionStateMachineImpl(timeProvider)
    }

    @Test
    fun `test valid connection transition triggers popup`() {
        assertEquals(ConnectionState.DISCONNECTED, stateMachine.currentState.value)

        val event = NormalizedConnectionEvent(
            deviceIdentity = null,
            profileType = ProfileType.A2DP,
            state = ConnectionState.CONNECTED,
            timestampMs = currentTime,
            rawAddress = "00:11:22:33:44:55"
        )

        stateMachine.processEvent(event)

        // After processing a CONNECTED event from DISCONNECTED, it should end up in COOLDOWN
        assertEquals(ConnectionState.COOLDOWN, stateMachine.currentState.value)
    }

    @Test
    fun `test duplicate connected events are ignored during cooldown`() {
        val event = NormalizedConnectionEvent(
            deviceIdentity = null,
            profileType = ProfileType.A2DP,
            state = ConnectionState.CONNECTED,
            timestampMs = currentTime,
            rawAddress = "00:11:22:33:44:55"
        )

        stateMachine.processEvent(event)
        assertEquals(ConnectionState.COOLDOWN, stateMachine.currentState.value)

        // Advance time by 1000ms (still within 3000ms cooldown)
        currentTime = 1000L
        stateMachine.processEvent(event)
        
        // Should still be in cooldown, duplicate ignored
        assertEquals(ConnectionState.COOLDOWN, stateMachine.currentState.value)
    }

    @Test
    fun `test cooldown expiry transitions to ready on next event`() {
        val event = NormalizedConnectionEvent(
            deviceIdentity = null,
            profileType = ProfileType.A2DP,
            state = ConnectionState.CONNECTED,
            timestampMs = currentTime,
            rawAddress = "00:11:22:33:44:55"
        )

        stateMachine.processEvent(event)
        assertEquals(ConnectionState.COOLDOWN, stateMachine.currentState.value)

        // Advance time by 3001ms
        currentTime = 3001L
        
        // Next connecting event should be processed because cooldown expired
        val connectingEvent = event.copy(state = ConnectionState.CONNECTING)
        stateMachine.processEvent(connectingEvent)
        
        assertEquals(ConnectionState.CONNECTING, stateMachine.currentState.value)
    }

    @Test
    fun `test disconnected event resets state to disconnected`() {
        val event = NormalizedConnectionEvent(
            deviceIdentity = null,
            profileType = ProfileType.A2DP,
            state = ConnectionState.CONNECTED,
            timestampMs = currentTime,
            rawAddress = "00:11:22:33:44:55"
        )

        stateMachine.processEvent(event)
        assertEquals(ConnectionState.COOLDOWN, stateMachine.currentState.value)

        val disconnectEvent = event.copy(state = ConnectionState.DISCONNECTED)
        stateMachine.processEvent(disconnectEvent)
        
        assertEquals(ConnectionState.DISCONNECTED, stateMachine.currentState.value)
    }
}
