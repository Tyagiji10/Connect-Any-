package com.example.connectany.presentation.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow

import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import java.io.File

@Composable
fun PopupImage(
    imageUri: String?,
    deviceType: String,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            if (imageUri.endsWith(".json")) {
                val jsonString = remember(imageUri) {
                    try {
                        File(imageUri).readText()
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (jsonString != null) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(jsonString))
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = imageModifier
                    )
                } else {
                    Text("Error", color = Color.Red)
                }
            } else {
                coil.compose.AsyncImage(
                    model = imageUri,
                    contentDescription = "Device Image",
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Text(
                text = deviceType.take(1),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun PopupMiddleContent(
    deviceName: String,
    batteryLevel: Int?,
    theme: PopupThemeColors,
    textColor: Color,
    subtitleColor: Color,
    isConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = deviceName,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isConnected) "✓" else "✗",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = if (isConnected) theme.accent else Color(0xFFE53935)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isConnected) "Connected" else "Disconnected",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = if (isConnected) theme.accent else Color(0xFFE53935)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (isConnected) "Ready to use" else "Offline",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
            color = subtitleColor
        )
    }
}

@Composable
fun PopupRightContent(
    batteryLevel: Int?,
    textColor: Color,
    arrowBgColor: Color
) {
    if (batteryLevel != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Battery Icon
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Transparent)
                    .border(1.dp, textColor, RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(1.dp)
                        .fillMaxHeight()
                        .fillMaxWidth(batteryLevel / 100f)
                        .background(if (batteryLevel <= 20) Color.Red else Color(0xFF4CAF50))
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Battery Text
            Text(
                text = "Battery $batteryLevel%",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = textColor
            )
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Connected Icon (Bluetooth or Checkmark)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(arrowBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Connected",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = textColor
            )
        }
    }
}
