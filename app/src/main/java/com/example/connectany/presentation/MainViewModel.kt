package com.example.connectany.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connectany.data.local.dao.DeviceDao
import com.example.connectany.data.local.dao.BatteryLogDao
import com.example.connectany.data.local.entity.DeviceEntity
import com.example.connectany.data.local.entity.BatteryLogEntity
import com.example.connectany.data.settings.AppSettings
import com.example.connectany.data.settings.SettingsRepository
import com.example.connectany.data.settings.ThemeOption
import com.example.connectany.domain.ConnectionStateMachine
import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.domain.model.DeviceIdentity
import com.example.connectany.domain.model.DeviceType
import com.example.connectany.domain.model.ConnectAnyDeviceUiModel
import com.example.connectany.domain.model.NormalizedConnectionEvent
import com.example.connectany.domain.model.ProfileType
import com.example.connectany.domain.model.BluetoothListState
import com.example.connectany.domain.usecase.GetBondedDevicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val deviceDao: DeviceDao,
    private val batteryLogDao: BatteryLogDao,
    private val getBondedDevicesUseCase: GetBondedDevicesUseCase,
    private val connectionStateMachine: ConnectionStateMachine
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val refreshTrigger = MutableStateFlow(0)

    val devices: StateFlow<BluetoothListState> = refreshTrigger
        .flatMapLatest { getBondedDevicesUseCase() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BluetoothListState.Loading)

    fun refreshBluetoothDevices() {
        refreshTrigger.value++
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setHasSeenOnboarding(true) }
    }

    fun setTheme(theme: ThemeOption) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setListening(isListening: Boolean) {
        viewModelScope.launch { settingsRepository.setListening(isListening) }
    }

    fun setDefaultPopupStyle(style: String) {
        viewModelScope.launch { settingsRepository.setDefaultPopupStyle(style) }
    }

    fun saveDevice(device: DeviceEntity) {
        viewModelScope.launch { deviceDao.insertDevice(device) }
    }

    suspend fun getDevice(macAddress: String): DeviceEntity? {
        return deviceDao.getDeviceByMac(macAddress)
    }

    suspend fun getBatteryLogs(macAddress: String): List<BatteryLogEntity> {
        return batteryLogDao.getLogsForDevice(macAddress)
    }

    fun toggleDevice(device: ConnectAnyDeviceUiModel, isEnabled: Boolean) {
        viewModelScope.launch {
            val existing = deviceDao.getDeviceByMac(device.deviceKey)
            if (existing != null) {
                deviceDao.insertDevice(existing.copy(isEnabled = isEnabled))
            } else {
                deviceDao.insertDevice(
                    DeviceEntity(
                        macAddress = device.deviceKey,
                        name = device.deviceName,
                        deviceType = device.systemDeviceType.name,
                        imageUri = null,
                        popupStyle = "DROP", // Default
                        isEnabled = isEnabled,
                        durationMs = 5000L
                    )
                )
            }
        }
    }

    fun simulateConnection(device: ConnectAnyDeviceUiModel) {
        viewModelScope.launch {
            connectionStateMachine.forcePreview(
                NormalizedConnectionEvent(
                    deviceIdentity = DeviceIdentity(
                        localId = device.deviceKey,
                        platformAddress = device.deviceKey,
                        normalizedName = device.deviceName,
                        deviceType = device.systemDeviceType,
                        profileHints = emptySet()
                    ),
                    profileType = ProfileType.A2DP,
                    state = ConnectionState.CONNECTED,
                    timestampMs = System.currentTimeMillis(),
                    rawAddress = device.deviceKey,
                    batteryLevel = 82
                )
            )
        }
    }
}
