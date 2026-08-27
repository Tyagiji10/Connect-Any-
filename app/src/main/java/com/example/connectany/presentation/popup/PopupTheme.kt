package com.example.connectany.presentation.popup

import androidx.compose.ui.graphics.Color

data class PopupThemeColors(
    val dropBackground: Color,
    val accent: Color
)

fun Color.toContrastColor(): Color {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return if (luminance > 0.5) Color.Black else Color.White
}

fun getPopupThemeByColor(colorInt: Int): PopupThemeColors {
    val baseColor = Color(colorInt)
    
    val r = baseColor.red
    val g = baseColor.green
    val b = baseColor.blue
    
    // Check if the background is predominantly green
    val isGreen = g > r && g > b && g > 0.3f
    
    val accentColor = if (isGreen) {
        Color(0xFFFF9800) // Orange
    } else {
        Color(0xFF4CAF50) // Green
    }
    
    return PopupThemeColors(
        dropBackground = baseColor,
        accent = accentColor
    )
}

// Legacy: kept for compatibility
fun getPopupThemeForDevice(deviceName: String): PopupThemeColors {
    return getPopupThemeByColor(android.graphics.Color.DKGRAY)
}
