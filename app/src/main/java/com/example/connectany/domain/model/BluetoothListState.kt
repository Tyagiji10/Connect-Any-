package com.example.connectany.domain.model

sealed class BluetoothListState {
    object Loading : BluetoothListState()
    object MissingPermission : BluetoothListState()
    object BluetoothDisabled : BluetoothListState()
    object Empty : BluetoothListState()
    data class Success(val devices: List<ConnectAnyDeviceUiModel>) : BluetoothListState()
}
