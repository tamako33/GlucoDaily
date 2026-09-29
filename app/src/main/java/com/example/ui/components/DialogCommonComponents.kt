package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MealPeriod
import com.example.data.MedCategory
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import java.util.Locale

/**
 * 通用弹窗交互组件库 (DialogCommonComponents)：
 *
 * 架构目标：
 * 消除 [AddItemDialog] 与 [RecordEditDialog] 之间的高重复交互代码，统一 UI 行为与视觉规范，
 * 避免后续修改某一组件细节（如常用药标签、时段颜色、大类分段）时产生跨模块牵连漏洞。
 */

/**
 * 时段选择分段胶囊（晨间、午间、傍晚、睡前，配备物理滑块动画与精准文字居中对齐）
 */
@Composable
fun MealPeriodSelectorCapsule(
    selectedPeriod: MealPeriod,
    onPeriodSelected: (MealPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods = remember { MealPeriod.entries }
    val selectedIndex = periods.indexOf(selectedPeriod).coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "period_tab_slider"
    )

    val currentThemeColor = when (selectedPeriod) {
        MealPeriod.MORNING -> AppThemeColors.breakfastColor
        MealPeriod.LUNCH -> AppThemeColors.lunchColor
        MealPeriod.DINNER -> AppThemeColors.dinnerColor
        MealPeriod.NIGHT -> AppThemeColors.bedtimeColor
    }

    val animatedColor by animateColorAsState(
        targetValue = currentThemeColor,
        animationSpec = tween(220),
        label = "period_slider_color"
    )

    val isDark = AppThemeColors.isDark
    val trackBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA).copy(alpha = 0.6f)
    val trackBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(AppleSegmentTrackShape)
            .background(trackBg)
            .border(BorderStroke(1.dp, trackBorderColor), AppleSegmentTrackShape)
            .padding(2.5.dp)
    ) {
        val tabWidth = maxWidth / periods.size

        // 滑动的时段实体色块（无沉重投影，纯净实色）
        Box(
            modifier = Modifier
                .offset(x = tabWidth * animatedIndex)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(AppleSegmentThumbShape)
                .background(animatedColor)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            periods.forEachIndexed { index, period ->
                val isSelected = period == selectedPeriod
                val animText by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "period_text_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(AppleSegmentThumbShape)
                        .applePressEffect(0.96f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onPeriodSelected(period) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = period.iconText,
                            fontSize = 12.5.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = period.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            letterSpacing = (-0.224).sp,
                            color = animText,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 药品三大类分段选择胶囊（胰岛素、口服药、GLP-1/针剂，配备物理滑块动画与精准文字居中对齐）
 */
@Composable
fun MedCategorySelectorCapsule(
    selectedCategory: MedCategory,
    onCategorySelected: (MedCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember { listOf(MedCategory.INSULIN, MedCategory.ORAL, MedCategory.GLP1) }
    val selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cat_tab_slider"
    )

    val isDark = AppThemeColors.isDark
    val trackBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA).copy(alpha = 0.6f)
    val trackBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(AppleSegmentTrackShape)
            .background(trackBg)
            .border(BorderStroke(1.dp, trackBorderColor), AppleSegmentTrackShape)
            .padding(2.5.dp)
    ) {
        val tabWidth = maxWidth / categories.size

        // 主题色滑块（Teal 纯实色）
        Box(
            modifier = Modifier
                .offset(x = tabWidth * animatedIndex)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(AppleSegmentThumbShape)
                .background(TealPrimary)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEachIndexed { index, cat ->
                val isSel = selectedCategory == cat
                val animText by animateColorAsState(
                    targetValue = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "cat_text_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(AppleSegmentThumbShape)
                        .applePressEffect(0.96f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onCategorySelected(cat) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = cat.icon, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = cat.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                            letterSpacing = (-0.224).sp,
                            color = animText,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * 用药时机分段选择滑块胶囊（餐前、餐中、餐后，配备物理滑块动画与精准对齐，响应用户动效诉求）
 */
@Composable
fun MedicationTimingSelectorCapsule(
    selectedTiming: String,
    onTimingSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 34.dp
) {
    val timings = remember { listOf("餐前", "餐中", "餐后") }
    val selectedIndex = timings.indexOf(selectedTiming).let { if (it >= 0) it else 0 }
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "timing_slider"
    )

    val isDark = AppThemeColors.isDark
    val trackBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA).copy(alpha = 0.6f)
    val trackBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)

    BoxWithConstraints(
        modifier = modifier
            .height(height)
            .clip(AppleSegmentTrackShape)
            .background(trackBg)
            .border(BorderStroke(1.dp, trackBorderColor), AppleSegmentTrackShape)
            .padding(2.5.dp)
    ) {
        val tabWidth = maxWidth / timings.size

        // 滑动的实体主题色滑块
        Box(
            modifier = Modifier
                .offset(x = tabWidth * animatedIndex)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(AppleSegmentThumbShape)
                .background(TealPrimary)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            timings.forEachIndexed { index, timing ->
                val isSel = timing == selectedTiming
                val textColor by animateColorAsState(
                    targetValue = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "timing_text_color_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(AppleSegmentThumbShape)
                        .applePressEffect(0.96f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTimingSelected(timing) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timing,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = (-0.224).sp,
                        color = textColor,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
    }
}

/**
 * 常见 / 历史高频使用药物快捷横向滑动胶囊推荐栏
 */
@Composable
fun QuickMedChips(
    medList: List<String>,
    selectedMedName: String,
    onMedSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(medList.take(8)) { med ->
            val isCurrent = selectedMedName == med
            val animBg by animateColorAsState(
                targetValue = if (isCurrent) TealPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                animationSpec = tween(200),
                label = "quick_med_bg"
            )
            val animBorderColor by animateColorAsState(
                targetValue = if (isCurrent) TealPrimary else Color.Transparent,
                animationSpec = tween(200),
                label = "quick_med_border"
            )
            val animTextColor by animateColorAsState(
                targetValue = if (isCurrent) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "quick_med_text"
            )
            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.04f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "quick_med_scale"
            )
            Surface(
                shape = ApplePillShape,
                color = animBg,
                border = BorderStroke(1.dp, animBorderColor),
                modifier = Modifier
                    .clip(ApplePillShape)
                    .applePressEffect(0.95f)
                    .clickable { onMedSelected(med) }
            ) {
                Text(
                    text = med,
                    fontSize = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = animTextColor,
                    letterSpacing = (-0.12).sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * 拍照识药状态与结果提示横幅
 */
@Composable
fun OcrStatusBanner(
    isRecognizing: Boolean,
    recognitionMessage: String?,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isRecognizing) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TealPrimary.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(13.dp),
                strokeWidth = 2.dp,
                color = TealPrimary
            )
            Text(
                text = "🔍 正在拍照识别药盒文字...",
                fontSize = 11.5.sp,
                color = TealPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    } else if (!recognitionMessage.isNullOrBlank()) {
        val isSuccess = recognitionMessage.startsWith("已识别")
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSuccess) TealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = recognitionMessage,
                    fontSize = 11.5.sp,
                    color = if (isSuccess) TealPrimary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismissMessage,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 血糖数值微调步长按钮 [-0.1] [+0.1]
 */
@Composable
fun StepAdjustButtons(
    currentValue: String,
    onValueChange: (String) -> Unit,
    step: Float = 0.1f,
    defaultValue: Float = 6.0f,
    minValue: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = AppleSmShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .clip(AppleSmShape)
                .applePressEffect(0.92f)
                .clickable {
                    val cur = currentValue.toFloatOrNull() ?: defaultValue
                    val nextVal = (cur - step).coerceAtLeast(minValue)
                    onValueChange(String.format(Locale.US, "%.1f", nextVal))
                }
        ) {
            Text(
                text = "-$step",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.12).sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
            )
        }
        Surface(
            shape = AppleSmShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .clip(AppleSmShape)
                .applePressEffect(0.92f)
                .clickable {
                    val cur = currentValue.toFloatOrNull() ?: defaultValue
                    val nextVal = cur + step
                    onValueChange(String.format(Locale.US, "%.1f", nextVal))
                }
        ) {
            Text(
                text = "+$step",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TealPrimary,
                letterSpacing = (-0.12).sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
            )
        }
    }
}

/**
 * 图片来源选择弹窗（相机拍照 vs 从相册选取）
 */
@Composable
fun ImageSourcePickerDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit
) {
    if (!visible) return

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val isDark = AppThemeColors.isDark
        Surface(
            shape = AppleCardShape,
            color = if (isDark) Color(0xFF1C1C1E) else MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            border = appleCardBorder(),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(horizontal = 4.dp)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "选择识药图片",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.28).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                Text(
                    text = "支持识别药盒包装、化验单、处方单文本",
                    fontSize = 12.sp,
                    letterSpacing = (-0.12).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 拍照按钮
                    Surface(
                        shape = AppleMdShape,
                        color = TealPrimary.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(AppleMdShape)
                            .applePressEffect(0.96f)
                            .clickable {
                                onDismiss()
                                onTakePhoto()
                            }
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📷", fontSize = 24.sp)
                            Text(
                                text = "拍照识药",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp,
                                color = TealPrimary,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }

                    // 相册选择按钮
                    Surface(
                        shape = AppleMdShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = appleCardBorder(),
                        modifier = Modifier
                            .weight(1f)
                            .clip(AppleMdShape)
                            .applePressEffect(0.96f)
                            .clickable {
                                onDismiss()
                                onPickGallery()
                            }
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🖼️", fontSize = 24.sp)
                            Text(
                                text = "相册选取",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }
                }

                androidx.compose.material3.TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .applePressEffect(0.96f)
                ) {
                    Text(
                        "取消",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }
        }
    }
}

/**
 * 关怀模式物理状态滑动开关（统一双模交互，带长者图标与平滑滑块）
 */
@Composable
fun CareModeToggleSwitch(
    isCareMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val thumbOffset by animateDpAsState(
        targetValue = if (isCareMode) 14.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "thumbOffset"
    )
    val trackColor by animateColorAsState(
        targetValue = if (isCareMode) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        label = "trackColor"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isCareMode) TealPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (isCareMode) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .height(35.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Elderly,
                contentDescription = if (isCareMode) "关怀模式开（点击切回标准版）" else "关怀模式关（点击开启）",
                tint = if (isCareMode) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = "关怀版",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCareMode) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            // 真实物理滑动开关轨道
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(trackColor),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(1.dp, CircleShape)
                )
            }
        }
    }
}
