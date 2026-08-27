package com.example.connectany.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.connectany.presentation.MainViewModel
import com.example.connectany.presentation.device.DeviceConfigScreen
import com.example.connectany.presentation.home.HomeScreen
import com.example.connectany.presentation.onboarding.OnboardingScreen
import com.example.connectany.presentation.settings.SettingsScreen

@Composable
fun ConnectAnyNavGraph(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsState()
    val devices by viewModel.devices.collectAsState()
    
    if (settings == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (settings!!.hasSeenOnboarding) "home" else "onboarding"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onContinue = {
                    viewModel.completeOnboarding()
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onSkip = {
                    viewModel.completeOnboarding()
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            HomeScreen(
                devices = devices,
                isListening = settings!!.isListening,
                onNavigateToDevice = { address -> navController.navigate("device/$address") },
                onNavigateToSettings = { navController.navigate("settings") },
                onToggleDevice = viewModel::toggleDevice,
                onRefresh = viewModel::refreshBluetoothDevices,
                onToggleListening = viewModel::setListening
            )
        }
        
        composable("device/{address}") { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address")
            var deviceEntity by remember { mutableStateOf<com.example.connectany.data.local.entity.DeviceEntity?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            
            LaunchedEffect(address) {
                if (address != null && address != "new") {
                    deviceEntity = viewModel.getDevice(address)
                }
                isLoading = false
            }
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                DeviceConfigScreen(
                    deviceMac = address,
                    initialDevice = deviceEntity,
                    onSave = { device ->
                        viewModel.saveDevice(device)
                    },
                    onPreview = { device ->
                        val deviceTypeEnum = try {
                            val normalized = device.deviceType
                                .uppercase()
                                .replace(" ", "_")
                                .replace("-", "_")
                            com.example.connectany.domain.model.DeviceType.valueOf(normalized)
                        } catch (e: IllegalArgumentException) {
                            com.example.connectany.domain.model.DeviceType.OTHER
                        }
                        val uiModel = com.example.connectany.domain.model.ConnectAnyDeviceUiModel(
                            deviceKey = device.macAddress,
                            deviceName = device.name,
                            systemDeviceType = deviceTypeEnum,
                            paired = true,
                            connectionState = com.example.connectany.domain.model.ConnectionState.CONNECTED,
                            connectanyEnabled = device.isEnabled,
                            customImageUri = device.imageUri,
                            popupStyle = device.popupStyle,
                            autoLaunchPackage = device.autoLaunchPackage,
                            smartVolumeLevel = device.smartVolumeLevel
                        )
                        viewModel.simulateConnection(uiModel)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        
        composable("settings") {
            SettingsScreen(
                settings = settings!!,
                onThemeChanged = viewModel::setTheme,
                onListeningChanged = viewModel::setListening,
                onDefaultPopupChanged = viewModel::setDefaultPopupStyle,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
