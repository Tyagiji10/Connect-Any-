package com.example.connectany.presentation.device

import android.content.pm.PackageManager
import com.example.connectany.presentation.device.components.AppSelectorSheet
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.connectany.data.local.entity.DeviceEntity
import com.example.connectany.theme.Dimensions
import java.util.UUID
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.example.connectany.presentation.popup.toContrastColor
import com.example.connectany.data.local.entity.BatteryLogEntity
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DeviceConfigScreen(
    deviceMac: String?,
    initialDevice: DeviceEntity? = null,
    batteryLogs: List<BatteryLogEntity> = emptyList(),
    onSave: (DeviceEntity) -> Unit,
    onPreview: (DeviceEntity) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(initialDevice?.name ?: "") }
    var macAddress by remember { mutableStateOf(deviceMac?.takeIf { it != "new" } ?: "") }
    var type by remember { mutableStateOf(initialDevice?.deviceType ?: "Headphones") }
    var popupStyle by remember { mutableStateOf(initialDevice?.popupStyle ?: "Drop") }
    var popupColor by remember { mutableIntStateOf(initialDevice?.popupColor ?: android.graphics.Color.DKGRAY) }
    var duration by remember { mutableFloatStateOf((initialDevice?.durationMs ?: 3000L) / 1000f) }
    var imageUri by remember { mutableStateOf<String?>(initialDevice?.imageUri) }
    
    // New Toggles
    var vibration by remember { mutableStateOf(initialDevice?.vibration ?: false) }
    var showOnConnect by remember { mutableStateOf(initialDevice?.showOnConnect ?: true) }
    var showOnDisconnect by remember { mutableStateOf(initialDevice?.showOnDisconnect ?: false) }
    val showOnReconnect = initialDevice?.showOnReconnect ?: false
    var showBattery by remember { mutableStateOf(initialDevice?.showBattery ?: true) }
    var isEnabled by remember { mutableStateOf(initialDevice?.isEnabled ?: true) }
    val playSound = initialDevice?.playSound ?: false
    var autoLaunchPackage by remember { mutableStateOf<String?>(initialDevice?.autoLaunchPackage) }
    var smartVolumeLevel by remember { mutableStateOf<Int?>(initialDevice?.smartVolumeLevel) }
    var showGlow by remember { mutableStateOf(initialDevice?.showGlow ?: false) }
    
    var showAppSelector by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Live battery level for this device (null = not connected or unsupported)
    var liveBatteryLevel by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(macAddress) {
        if (macAddress.isBlank()) return@LaunchedEffect
        liveBatteryLevel = try {
            val btManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val adapter = btManager?.adapter ?: return@LaunchedEffect
            @Suppress("DEPRECATION")
            val device = adapter.getRemoteDevice(macAddress)
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            if (level in 0..100) level else null
        } catch (e: Exception) { null }
    }

    val pm = context.packageManager
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val isJson = context.contentResolver.getType(uri)?.contains("json") == true
                    val extension = if (isJson) ".json" else ".jpg"
                    val file = java.io.File(context.filesDir, "media_${UUID.randomUUID()}$extension")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    imageUri = file.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                    imageUri = uri.toString()
                }
            }
        }
    )
    
    val paletteColors = remember {
        listOf(
            0xFFFFFFFF, 0xFF808080, 0xFF000000,
            0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 
            0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
            0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39,
            0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722,
            0xFF795548, 0xFF9E9E9E, 0xFF607D8B, 0xFF141414
        ).map { it.toInt() }
    }
    
    // Auto-save effect
    val keys = listOf(name, macAddress, type, popupStyle, popupColor, duration, imageUri, vibration, showOnConnect, showOnDisconnect, showOnReconnect, showBattery, isEnabled, playSound, autoLaunchPackage, smartVolumeLevel, showGlow)
    LaunchedEffect(keys) {
        if (macAddress.isBlank() && initialDevice == null) return@LaunchedEffect
        val mac = macAddress.ifBlank { UUID.randomUUID().toString() }
        onSave(
            DeviceEntity(
                macAddress = mac,
                name = name.ifBlank { "Unknown Device" },
                deviceType = type,
                imageUri = imageUri,
                popupStyle = popupStyle,
                isEnabled = isEnabled,
                durationMs = (duration * 1000).toLong(),
                vibration = vibration,
                showOnConnect = showOnConnect,
                showOnDisconnect = showOnDisconnect,
                showOnReconnect = showOnReconnect,
                showBattery = showBattery,
                popupColor = popupColor,
                playSound = playSound,
                autoLaunchPackage = autoLaunchPackage,
                smartVolumeLevel = smartVolumeLevel,
                showGlow = showGlow
            )
        )
    }
    
    val types = listOf("Headphones", "Earbuds", "Watch", "Speaker", "Car audio", "Keyboard", "Controller", "Laptop", "Other")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 50)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect Any", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<", style = MaterialTheme.typography.titleMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        content = { paddingValues ->
            Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = Dimensions.Padding.medium)
                    .verticalScroll(rememberScrollState())
            ) {
                if (showAppSelector) {
                    AppSelectorSheet(
                        onDismiss = { showAppSelector = false },
                        onAppSelected = { pkg ->
                            autoLaunchPackage = pkg
                            showAppSelector = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            
                Column(modifier = Modifier.padding(vertical = 24.dp)) {
                        Text(
                            text = name.ifBlank { "New Device" },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        // Show live battery badge if device is connected and supports it
                        if (liveBatteryLevel != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when {
                                                liveBatteryLevel!! >= 60 -> Color(0xFF4CAF50)
                                                liveBatteryLevel!! >= 30 -> Color(0xFFFF9800)
                                                else -> Color(0xFFF44336)
                                            },
                                            shape = RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🔋 ${liveBatteryLevel}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Connected",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Art, motion, sound, and when the overlay is allowed to fire.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (liveBatteryLevel != null || batteryLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            BatteryDashboard(batteryLogs = batteryLogs, currentLevel = liveBatteryLevel ?: batteryLogs.lastOrNull()?.batteryLevel ?: 100)
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Toggles
                        ToggleRow("Enabled", null, isEnabled) { isEnabled = it }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Device name", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        ToggleRow("Vibration", "Uses the device vibrator when the overlay fires.", vibration) { vibration = it }
                        Spacer(modifier = Modifier.height(24.dp))
                        ToggleRow("Show on connect", "Show overlay animation when this device connects.", showOnConnect) { showOnConnect = it }
                        Spacer(modifier = Modifier.height(24.dp))
                        ToggleRow("Show on disconnect", "Show overlay animation when this device disconnects.", showOnDisconnect) { showOnDisconnect = it }
                        Spacer(modifier = Modifier.height(24.dp))
                        ToggleRow("Show battery", null, showBattery) { showBattery = it }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text("Type", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                types.forEach { t ->
                                    FilterChip(
                                        selected = type == t,
                                        onClick = { type = t },
                                        label = { Text(t) }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Cooldown
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Cooldown", style = MaterialTheme.typography.titleMedium)
                            Text("${duration}s", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Duplicate stack events are ignored while cooling down.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = duration,
                            onValueChange = { duration = (it * 10).roundToInt() / 10f },
                            valueRange = 1f..5f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text("Device media", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("PNG, WebP, JPG, or Lottie JSON. Solid mattes can be lifted automatically.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (imageUri != null) {
                            if (imageUri!!.endsWith(".json")) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Lottie Animation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                coil.compose.AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Selected Image",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = { 
                                    photoPickerLauncher.launch(arrayOf("image/*", "application/json"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(if (imageUri != null) "Change media" else "Select media", color = MaterialTheme.colorScheme.onBackground)
                            }

                            if (imageUri != null) {
                                OutlinedButton(
                                    onClick = { 
                                        imageUri = null
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text("Automation", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Auto-Launch App
                        val appName = remember(autoLaunchPackage) {
                            autoLaunchPackage?.let { pkg ->
                                try {
                                    val info = pm.getApplicationInfo(pkg, 0)
                                    pm.getApplicationLabel(info).toString()
                                } catch (e: Exception) {
                                    pkg
                                }
                            } ?: "None (Disable)"
                        }
                        
                        OutlinedCard(
                            onClick = { showAppSelector = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Auto-Launch App", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(appName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("Select ›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Smart Volume
                        var smartVolumeEnabled by remember { mutableStateOf(smartVolumeLevel != null) }
                        ToggleRow(
                            title = "Smart Volume Memory",
                            subtitle = "Auto-set media volume to a saved level when this device connects.",
                            checked = smartVolumeEnabled,
                            onCheckedChange = { enabled ->
                                smartVolumeEnabled = enabled
                                if (!enabled) smartVolumeLevel = null
                                else if (smartVolumeLevel == null) smartVolumeLevel = 50
                            }
                        )
                        if (smartVolumeEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Volume level", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${smartVolumeLevel ?: 50}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = (smartVolumeLevel ?: 50).toFloat(),
                                onValueChange = { smartVolumeLevel = it.roundToInt() },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text("Popup style", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            PopupStyleCard("Drop", "A liquid bead rises from the bottom and opens into a card.", popupStyle == "Drop", Modifier.fillMaxWidth()) { popupStyle = "Drop" }
                            PopupStyleCard("Glass", "Frosted panel with a quiet scale-in.", popupStyle == "Glass", Modifier.fillMaxWidth()) { popupStyle = "Glass" }
                            PopupStyleCard("Gaming", "RGB glowing borders with a futuristic vibe.", popupStyle == "Gaming", Modifier.fillMaxWidth()) { popupStyle = "Gaming" }
                            PopupStyleCard("Minimal", "A tiny toast at the top of the screen.", popupStyle == "Minimal", Modifier.fillMaxWidth()) { popupStyle = "Minimal" }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        ToggleRow(
                            title = "Premium Glow Effect",
                            subtitle = "Show an animated, colored shadow behind the popup window.",
                            checked = showGlow,
                            onCheckedChange = { showGlow = it }
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Popup color variation", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(paletteColors.size) { index ->
                                val colorInt = paletteColors[index]
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(colorInt), androidx.compose.foundation.shape.CircleShape)
                                        .border(
                                            width = if (popupColor == colorInt) 3.dp else 0.dp,
                                            color = if (popupColor == colorInt) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .clickable { popupColor = colorInt },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (popupColor == colorInt) {
                                        Text("✓", color = Color(colorInt).toContrastColor(), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Button(
                            onClick = {
                                val mac = macAddress.ifBlank { UUID.randomUUID().toString() }
                                onPreview(
                                    DeviceEntity(
                                        macAddress = mac,
                                        name = name.ifBlank { "Unknown Device" },
                                        deviceType = type,
                                        imageUri = imageUri,
                                        popupStyle = popupStyle,
                                        isEnabled = isEnabled,
                                        durationMs = (duration * 1000).toLong(),
                                        vibration = vibration,
                                        showOnConnect = showOnConnect,
                                        showOnDisconnect = showOnDisconnect,
                                        showOnReconnect = showOnReconnect,
                                        showBattery = showBattery,
                                        popupColor = popupColor,
                                        playSound = playSound,
                                        autoLaunchPackage = autoLaunchPackage,
                                        smartVolumeLevel = smartVolumeLevel,
                                        showGlow = showGlow
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Preview overlay", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
                        }
                        
                        Spacer(modifier = Modifier.height(64.dp))
                    }
            }
        }
    }
    )
}

@Composable
fun BatteryDashboard(batteryLogs: List<BatteryLogEntity>, currentLevel: Int) {
    if (batteryLogs.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Battery Health & Usage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Gathering data...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    val sortedLogs = batteryLogs.sortedBy { it.timestampMs }
    val firstLog = sortedLogs.first()
    val lastLog = sortedLogs.last()
    
    val timeDiffHours = (lastLog.timestampMs - firstLog.timestampMs) / 3600000.0
    val batteryDrop = firstLog.batteryLevel - lastLog.batteryLevel
    
    val estimatedText = if (timeDiffHours > 0.1 && batteryDrop > 0) {
        val drainPerHour = batteryDrop / timeDiffHours
        val hoursLeft = currentLevel / drainPerHour
        val hours = hoursLeft.toInt()
        val mins = ((hoursLeft - hours) * 60).toInt()
        if (hours > 0) "Estimated backup: ${hours}h ${mins}m" else "Estimated backup: ${mins}m"
    } else {
        "Gathering more data for estimate..."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Battery Health & Usage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(estimatedText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simple Line Graph
            val graphColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                val path = Path()
                val minTime = firstLog.timestampMs
                val maxTime = lastLog.timestampMs
                val timeRange = maxTime - minTime
                
                if (timeRange > 0) {
                    sortedLogs.forEachIndexed { index, log ->
                        val x = size.width * ((log.timestampMs - minTime).toFloat() / timeRange.toFloat())
                        val y = size.height - (size.height * (log.batteryLevel.toFloat() / 100f))
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = graphColor,
                        style = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${firstLog.batteryLevel}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${lastLog.batteryLevel}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupStyleCard(title: String, desc: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
