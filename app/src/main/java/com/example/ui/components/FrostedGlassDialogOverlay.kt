package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppThemeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 通用浅色/深色毛玻璃弹窗蒙层容器：
 * - 浅色模式：半透明浅色乳白毛玻璃蒙层，杜绝深灰色死板感，呈现通透现代的雾面效果；
 * - 黑夜模式：色彩逻辑相反，采用深邃夜空黑曜石半透明微透微光质感；
 * - 灵动动画：呼出时微阻尼弹性微缩放 (0.92f -> 1.0f) + 顺滑浮动上浮 (24dp -> 0dp) + 透明度渐入；
 *   退出时顺滑微缩回渐隐 (1.0f -> 0.95f, 170ms)；
 * - 点击遮罩外或按返回键触发顺滑退场动效，并自动通知 onDismissRequest。
 */
@Composable
fun FrostedGlassDialogOverlay(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable BoxScope.(dismissWithAnimation: () -> Unit) -> Unit
) {
    val isDark = AppThemeColors.isDark
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun dismissWithAnimation() {
        if (!isVisible) return
        isVisible = false
        coroutineScope.launch {
            delay(170)
            onDismissRequest()
        }
    }

    if (dismissOnBackPress) {
        BackHandler {
            dismissWithAnimation()
        }
    }

    val animProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = if (isVisible) {
            spring(dampingRatio = 0.78f, stiffness = 400f)
        } else {
            tween(durationMillis = 170, easing = FastOutLinearInEasing)
        },
        label = "frosted_dialog_anim"
    )

    // 强化毛玻璃模糊与散射感：浅色模式使用高密度乳白多层雾面渐变，彻底虚化底层细节；黑夜模式使用深邃夜空星曜渐变
    val scrimBrush = remember(isDark, animProgress) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF040711).copy(alpha = 0.88f * animProgress),
                    Color(0xFF0A101D).copy(alpha = 0.82f * animProgress),
                    Color(0xFF02050C).copy(alpha = 0.90f * animProgress)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF).copy(alpha = 0.88f * animProgress),
                    Color(0xFFF1F5F9).copy(alpha = 0.82f * animProgress),
                    Color(0xFFE2E8F0).copy(alpha = 0.85f * animProgress)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scrimBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = dismissOnClickOutside
            ) {
                dismissWithAnimation()
            },
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = 0.92f + 0.08f * animProgress
                    scaleY = 0.92f + 0.08f * animProgress
                    alpha = animProgress.coerceIn(0f, 1f)
                    translationY = (1f - animProgress) * 24.dp.toPx()
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = true
                ) {
                    // 消费卡片内部点击，避免事件冒泡至外部遮罩
                },
            contentAlignment = Alignment.Center
        ) {
            content { dismissWithAnimation() }
        }
    }
}
