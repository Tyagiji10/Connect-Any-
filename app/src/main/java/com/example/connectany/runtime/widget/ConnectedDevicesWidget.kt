package com.example.connectany.runtime.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items

class ConnectedDevicesWidget(
    private val connectedDevices: List<WidgetDeviceData>
) : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(connectedDevices)
        }
    }
    
    @Composable
    private fun WidgetContent(devices: List<WidgetDeviceData>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .cornerRadius(16.dp)
                .padding(12.dp)
        ) {
            Text(
                text = "Connected Devices",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )
            
            if (devices.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No devices connected",
                        style = TextStyle(color = ColorProvider(Color.Gray))
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(devices) { device ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color(0x33FFFFFF))
                                .cornerRadius(8.dp)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = device.name,
                                style = TextStyle(color = ColorProvider(Color.White)),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            if (device.batteryLevel != null) {
                                Text(
                                    text = "🔋 ${device.batteryLevel}%",
                                    style = TextStyle(color = ColorProvider(Color(0xFF4CAF50)), fontWeight = FontWeight.Medium)
                                )
                            } else {
                                Text(
                                    text = "Connected",
                                    style = TextStyle(color = ColorProvider(Color(0xFF2196F3)), fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class WidgetDeviceData(
    val name: String,
    val batteryLevel: Int?
)
