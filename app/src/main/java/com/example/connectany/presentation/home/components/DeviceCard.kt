package com.example.connectany.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.connectany.domain.model.ConnectionState
import com.example.connectany.domain.model.ConnectAnyDeviceUiModel
import com.example.connectany.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCard(
    device: ConnectAnyDeviceUiModel,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(Dimensions.CornerRadius.large)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.Padding.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (device.customImageUri != null) {
                        AsyncImage(
                            model = device.customImageUri,
                            contentDescription = "Device Image",
                            modifier = Modifier
                                .size(Dimensions.Sizing.iconSize)
                                .clip(RoundedCornerShape(Dimensions.CornerRadius.medium)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(Dimensions.Sizing.iconSize)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Dimensions.CornerRadius.medium)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = device.systemDeviceType.name.take(1),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(Dimensions.Padding.medium))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val statusText = if (device.paired) "Paired" else "Not Paired"
                        val connectionText = if (device.connectionState == ConnectionState.CONNECTED) " • Connected" else " • Disconnected"
                        Text(
                            text = statusText + connectionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val connectanyText = if (device.connectanyEnabled) "ConnectAny Animation: Enabled ›" else "+ Customize Connection Popup ›"
                        Text(
                            text = connectanyText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(Dimensions.Padding.small))
                
                Switch(
                    checked = device.connectanyEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(0.85f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.background,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}
