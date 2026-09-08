package com.example.connectany.presentation.popup

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.connectany.theme.Dimensions
import kotlinx.coroutines.delay

@Composable
fun GamingPopup(
    deviceName: String,
    deviceType: String,
    batteryLevel: Int?,
    theme: PopupThemeColors,
    imageUri: String? = null,
    isConnected: Boolean = true,
    displayDurationMs: Long = 5000L,
    showGlow: Boolean = false,
    onAnimationComplete: () -> Unit
) {
    var animationState by remember { mutableStateOf(PopupAnimationState.HIDDEN) }

    LaunchedEffect(Unit) {
        animationState = PopupAnimationState.ENTERING
        delay(400)
        if (animationState == PopupAnimationState.ENTERING) {
            animationState = PopupAnimationState.VISIBLE
        }
        delay((displayDurationMs - 700L).coerceAtLeast(0L))
        if (animationState == PopupAnimationState.VISIBLE) {
            animationState = PopupAnimationState.EXITING
        }
    }

    LaunchedEffect(animationState) {
        if (animationState == PopupAnimationState.EXITING) {
            delay(300)
            animationState = PopupAnimationState.HIDDEN
            onAnimationComplete()
        }
    }

    val transition = updateTransition(targetState = animationState, label = "GamingTransition")

    val translationY by transition.animateFloat(
        transitionSpec = {
            when {
                PopupAnimationState.HIDDEN isTransitioningTo PopupAnimationState.ENTERING ->
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                PopupAnimationState.VISIBLE isTransitioningTo PopupAnimationState.EXITING ->
                    tween(durationMillis = 300, easing = FastOutSlowInEasing)
                else -> snap()
            }
        }, label = "TranslationY"
    ) { state ->
        when (state) {
            PopupAnimationState.HIDDEN, PopupAnimationState.EXITING -> 300f 
            PopupAnimationState.ENTERING, PopupAnimationState.VISIBLE -> 0f
        }
    }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) }, label = "Alpha"
    ) { state ->
        when (state) {
            PopupAnimationState.HIDDEN -> 0f
            PopupAnimationState.ENTERING -> 1f
            PopupAnimationState.VISIBLE -> 1f
            PopupAnimationState.EXITING -> 0f
        }
    }
    
    // RGB Glow Effect
    val infiniteTransition = rememberInfiniteTransition(label = "rgbGlow")
    val rgbOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rgbOffset"
    )
    
    // Cyberpunk/Gaming colors
    val neonColors = listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
    val glowBrush = Brush.sweepGradient(
        colors = neonColors,
        center = androidx.compose.ui.geometry.Offset(rgbOffset * 1000f, rgbOffset * 500f) // Fake sweep animation
    )
    
    val bgColor = Color(0xFF121212) // Pitch black/dark grey
    
    val infiniteTransitionGlow = rememberInfiniteTransition(label = "glowAlpha")
    val glowAlpha by infiniteTransitionGlow.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .safeDrawingPadding()
            .padding(top = 64.dp, bottom = Dimensions.Padding.xlarge),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (animationState != PopupAnimationState.HIDDEN) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .widthIn(max = Dimensions.Layout.popupMaxWidth)
                    .wrapContentHeight()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 20f) {
                                animationState = PopupAnimationState.EXITING
                            }
                        }
                    }
                    .graphicsLayer {
                        this.translationY = translationY
                        this.alpha = alpha
                    }
                    .then(
                        if (showGlow) Modifier.glow(
                            color = Color.Cyan,
                            alpha = glowAlpha,
                            borderRadius = 12.dp,
                            glowRadius = 32.dp
                        ) else Modifier.shadow(
                            elevation = 32.dp,
                            shape = RoundedCornerShape(12.dp),
                            spotColor = Color.Cyan,
                            ambientColor = Color.Magenta
                        )
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .border(2.dp, glowBrush, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PopupImage(
                        imageUri = imageUri,
                        deviceType = deviceType,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Cyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        imageModifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                    
                    val contrastColor = Color.White
                    
                    PopupMiddleContent(
                        deviceName = deviceName,
                        batteryLevel = batteryLevel,
                        theme = theme, // Not heavily used here, keeping parameter for signature match
                        textColor = contrastColor,
                        subtitleColor = Color.Cyan,
                        isConnected = isConnected,
                        modifier = Modifier.weight(1f)
                    )
                    
                    PopupRightContent(
                        batteryLevel = batteryLevel,
                        textColor = Color.Magenta,
                        arrowBgColor = Color.Magenta.copy(alpha = 0.1f),
                        isConnected = isConnected
                    )
                }
            }
        }
    }
}
