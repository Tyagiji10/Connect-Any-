package com.example.connectany.presentation.popup

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.connectany.theme.Dimensions
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

enum class PopupAnimationState {
    HIDDEN, ENTERING, VISIBLE, EXITING
}

@Composable
fun DropPopup(
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
        delay(400) // Entrance duration
        animationState = PopupAnimationState.VISIBLE
        delay((displayDurationMs - 700L).coerceAtLeast(0L)) // Hold for configured time minus entrance+exit
        animationState = PopupAnimationState.EXITING
        delay(300) // Exit duration
        animationState = PopupAnimationState.HIDDEN
        onAnimationComplete()
    }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(animationState) {
        if (animationState == PopupAnimationState.VISIBLE) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val transition = updateTransition(targetState = animationState, label = "PopupTransition")

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

    val scale by transition.animateFloat(
        transitionSpec = {
            if (PopupAnimationState.HIDDEN isTransitioningTo PopupAnimationState.ENTERING) {
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            } else {
                tween(200)
            }
        }, label = "Scale"
    ) { state ->
        when (state) {
            PopupAnimationState.HIDDEN -> 0.8f
            PopupAnimationState.ENTERING, PopupAnimationState.VISIBLE -> 1f
            PopupAnimationState.EXITING -> 0.9f
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
                    .fillMaxWidth(0.9f)
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
                        this.scaleX = scale
                        this.scaleY = scale
                    }
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color.Black,
                        ambientColor = Color.Black
                    )
                    .background(
                        color = theme.dropBackground,
                        shape = RoundedCornerShape(28.dp) // Adjusted to match mockup curves
                    )
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
                        arrowBgColor = contrastColor.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }
}
