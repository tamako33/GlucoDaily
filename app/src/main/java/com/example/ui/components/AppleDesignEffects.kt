package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppThemeColors

// =========================================================================
// Apple Design Standards (per VoltAgent/awesome-design-md: apple/DESIGN.md)
// =========================================================================

/** Apple Border Radius Scale */
val AppleRadiusSm: Dp = 8.dp       // sm: 8px (compact utility buttons, chips)
val AppleRadiusMd: Dp = 11.dp      // md: 11px (pearl capsules)
val AppleRadiusLg: Dp = 18.dp      // lg: 18px (store utility cards, health summary cards)
val AppleRadiusPill: Dp = 9999.dp  // pill: 9999px (signature Apple action pill)

val AppleCardShape = RoundedCornerShape(AppleRadiusLg)
val ApplePillShape = RoundedCornerShape(AppleRadiusPill)
val AppleSmShape = RoundedCornerShape(AppleRadiusSm)
val AppleMdShape = RoundedCornerShape(AppleRadiusMd)

/** Standard Apple Segmented Control (Tabs) Shapes - 100% unified across the app */
val AppleSegmentTrackShape = RoundedCornerShape(10.dp)
val AppleSegmentThumbShape = RoundedCornerShape(7.5.dp)

/**
 * Apple signature press micro-interaction:
 * "Use transform: scale(0.95) as the active/press state on every button — it's the system-wide micro-interaction."
 * Uses Apple spring physics for an authentic tactile feel.
 */
fun Modifier.applePressEffect(
    targetScale: Float = 0.96f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "apple_press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Standard Apple Hairline Border (1px crisp border for cards and controls, without heavy drop shadows)
 */
@Composable
fun appleCardBorder(
    color: Color = AppThemeColors.hairline,
    width: Dp = 1.dp
): BorderStroke = BorderStroke(width, color)
