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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.connectany.theme.Dimensions
import kotlinx.coroutines.delay

@Composable
fun MinimalPopup(
    deviceName: String,
    batteryLevel: Int?,
    theme: PopupThemeColors,
    isConnected: Boolean = true,
    displayDurationMs: Long = 5000L,
    onAnimationComplete: () -> Unit
) {
    var animationState by remember { mutableStateOf(PopupAnimationState.HIDDEN) }

    LaunchedEffect(Unit) {
        animationState = PopupAnimationState.ENTERING
        delay(300)
        animationState = PopupAnimationState.VISIBLE
        delay((displayDurationMs - 600L).coerceAtLeast(0L))
        animationState = PopupAnimationState.EXITING
        delay(300)
        animationState = PopupAnimationState.HIDDEN
        onAnimationComplete()
    }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(animationState) {
        if (animationState == PopupAnimationState.VISIBLE) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val transition = updateTransition(targetState = animationState, label = "MinimalTransition")

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
            PopupAnimationState.HIDDEN, PopupAnimationState.EXITING -> -150f // Drop from top
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
    
    val bgColor = theme.dropBackground
    val contrastColor = bgColor.toContrastColor()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(top = Dimensions.Padding.medium), // Top placement
        contentAlignment = Alignment.TopCenter
    ) {
        if (animationState != PopupAnimationState.HIDDEN) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -20f) { // Swipe up to dismiss
                                animationState = PopupAnimationState.EXITING
                            }
                        }
                    }
                    .graphicsLayer {
                        this.translationY = translationY
                        this.alpha = alpha
                    }
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(bgColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connected indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (isConnected) theme.accent else Color.Red)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = contrastColor,
                    maxLines = 1
                )
                
                if (batteryLevel != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$batteryLevel%",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = contrastColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
