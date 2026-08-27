package com.example.connectany.presentation.popup

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.connectany.theme.Dimensions
import kotlinx.coroutines.delay

@Composable
fun MagneticPopup(
    deviceName: String,
    deviceType: String,
    batteryLevel: Int?,
    theme: PopupThemeColors,
    imageUri: String? = null,
    isConnected: Boolean = true,
    displayDurationMs: Long = 5000L,
    onAnimationComplete: () -> Unit
) {
    var animationState by remember { mutableStateOf(PopupAnimationState.HIDDEN) }

    LaunchedEffect(Unit) {
        animationState = PopupAnimationState.ENTERING
        delay(300)
        animationState = PopupAnimationState.VISIBLE
        delay((displayDurationMs - 550L).coerceAtLeast(0L))
        animationState = PopupAnimationState.EXITING
        delay(250)
        animationState = PopupAnimationState.HIDDEN
        onAnimationComplete()
    }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(animationState) {
        if (animationState == PopupAnimationState.VISIBLE) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val transition = updateTransition(targetState = animationState, label = "MagneticTransition")

    val translationY by transition.animateFloat(
        transitionSpec = {
            when {
                PopupAnimationState.HIDDEN isTransitioningTo PopupAnimationState.ENTERING ->
                    spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium) // Snappy!
                PopupAnimationState.VISIBLE isTransitioningTo PopupAnimationState.EXITING ->
                    tween(durationMillis = 250, easing = FastOutSlowInEasing)
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
        transitionSpec = { tween(durationMillis = 200) }, label = "Alpha"
    ) { state ->
        when (state) {
            PopupAnimationState.HIDDEN -> 0f
            PopupAnimationState.ENTERING -> 1f
            PopupAnimationState.VISIBLE -> 1f
            PopupAnimationState.EXITING -> 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(bottom = Dimensions.Padding.xlarge),
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
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color.Black,
                        ambientColor = Color.Black
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(theme.dropBackground)
                    .border(1.dp, theme.dropBackground.toContrastColor().copy(alpha = 0.05f), RoundedCornerShape(28.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Image
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PopupImage(
                            imageUri = imageUri,
                            deviceType = deviceType,
                            modifier = Modifier.fillMaxSize(),
                            imageModifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    
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
                        arrowBgColor = contrastColor.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }
}
