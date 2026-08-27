package com.example.connectany.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.connectany.data.settings.AppSettings
import com.example.connectany.data.settings.ThemeOption
import com.example.connectany.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeChanged: (ThemeOption) -> Unit,
    onListeningChanged: (Boolean) -> Unit,
    onDefaultPopupChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = Dimensions.Layout.contentMaxWidth)
                    .fillMaxHeight()
                    .padding(horizontal = Dimensions.Padding.large)
            ) {
                Spacer(modifier = Modifier.height(Dimensions.Padding.medium))
                
                Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("System follows the OS.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(Dimensions.Padding.small))
            
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.Padding.small)) {
                ThemeOption.entries.forEach { option ->
                    FilterChip(
                        selected = settings.theme == option,
                        onClick = { onThemeChanged(option) },
                        label = { Text(option.name) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Dimensions.Padding.xlarge))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Listening", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("Event-driven only. No polling.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.isListening,
                    onCheckedChange = onListeningChanged
                )
            }
            
            Spacer(modifier = Modifier.height(Dimensions.Padding.xlarge))
            
            Text("Default popup style", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(Dimensions.Padding.small))
            
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.Padding.small)) {
                listOf("Drop", "Glass", "Magnetic").forEach { style ->
                    FilterChip(
                        selected = settings.defaultPopupStyle == style,
                        onClick = { onDefaultPopupChanged(style) },
                        label = { Text(style) }
                    )
                }
            }
        }
    }
}
}
