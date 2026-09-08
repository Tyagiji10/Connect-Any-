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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.connectany.theme.Dimensions
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

@Composable
fun GlassPopup(
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

    val transition = updateTransition(targetState = animationState, label = "GlassTransition")

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
            PopupAnimationState.HIDDEN, PopupAnimationState.EXITING -> 300f // Drop from bottom
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

    val glassBackground = Color.White.copy(alpha = 0.2f)
    val glassBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.5f),
            Color.White.copy(alpha = 0.1f)
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "glowAlpha")
    val glowAlpha by infiniteTransition.animateFloat(
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
            .padding(top = 64.dp, bottom = Dimensions.Padding.xlarge), // Bottom placement
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
                            color = theme.dropBackground,
                            alpha = glowAlpha,
                            borderRadius = 28.dp,
                            glowRadius = 32.dp
                        ) else Modifier.shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = Color.Black,
                            ambientColor = Color.Black
                        )
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(glassBackground)
                    // Note: True blur requires RenderEffect which is API 31+, Compose blur modifier might not blur behind content correctly in a WindowManager overlay, but works for the shape itself
                    .border(1.dp, glassBorder, RoundedCornerShape(28.dp))
                    .background(theme.dropBackground.copy(alpha = 0.85f)) // Base glass color
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Image
                    PopupImage(
                        imageUri = imageUri,
                        deviceType = deviceType,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        imageModifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                    
                    // Middle: Text & Status
                    val contrastColor = theme.dropBackground.toContrastColor()
                    
                    PopupMiddleContent(
                        deviceName = deviceName,
                        batteryLevel = batteryLevel,
                        theme = theme,
                        textColor = contrastColor,
                        subtitleColor = contrastColor.copy(alpha = 0.7f),
                        isConnected = isConnected,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Right: Actions / Battery
                    PopupRightContent(
                        batteryLevel = batteryLevel,
                        textColor = contrastColor.copy(alpha = 0.8f),
                        arrowBgColor = contrastColor.copy(alpha = 0.08f),
                        isConnected = isConnected
                    )
                }
            }
        }
    }
}
