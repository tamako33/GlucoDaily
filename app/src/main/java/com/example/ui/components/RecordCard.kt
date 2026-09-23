package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BGLevel
import com.example.data.BGUtils
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.data.PrevNightInfo
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import java.util.Locale

data class BGItemData(
    val label: String,
    val value: Float?,
    val isPostMeal: Boolean = false,
    val isFasting: Boolean = false
)

@Composable
fun RecordCard(
    record: InsulinRecord,
    prevNightInfo: PrevNightInfo?,
    isToday: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditPeriod: ((MealPeriod) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("record_card_${record.date}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // Card Top Action Bar (翻页栏已有日期，此处不重复显示日期)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Total Insulin Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (AppThemeColors.isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else TealPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "全天用药: ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (record.totalInsulin % 1.0f == 0f) {
                                record.totalInsulin.toInt().toString()
                            } else {
                                String.format(Locale.US, "%.1f", record.totalInsulin)
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TealPrimary
                        )
                        Text(
                            text = " U",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    }
                }

                // Right: Action buttons (Edit & Delete)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("edit_button_${record.date}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑记录",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("delete_button_${record.date}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除记录",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4 Big Period Cards: 晨间, 午间, 傍晚, 夜晚
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 晨间大卡片 (2个血糖数据: 空腹, 餐后)
                PeriodBigCard(
                    title = "🌅 晨间",
                    periodTag = "早餐时段",
                    bgItems = listOf(
                        BGItemData(label = "空腹", value = record.fastingBG ?: record.preBfBG, isFasting = true),
                        BGItemData(label = "餐后", value = record.postBfBG, isPostMeal = true)
                    ),
                    medName = record.bfMedName.ifBlank { "胰岛素" },
                    dose = record.bfInsulin,
                    medTiming = record.bfMedTiming,
                    dietText = record.bfDiet,
                    themeColor = AppThemeColors.breakfastColor,
                    themeBg = AppThemeColors.breakfastBg,
                    themeBorder = AppThemeColors.breakfastBorder,
                    prevNightInfo = prevNightInfo,
                    onEdit = { onEditPeriod?.invoke(MealPeriod.MORNING) ?: onEdit() }
                )

                // 2. 午间大卡片 (2个血糖数据: 餐前, 餐后)
                PeriodBigCard(
                    title = "☀️ 午间",
                    periodTag = "午餐时段",
                    bgItems = listOf(
                        BGItemData(label = "餐前", value = record.preLunchBG, isPostMeal = false),
                        BGItemData(label = "餐后", value = record.postLunchBG, isPostMeal = true)
                    ),
                    medName = record.lunchMedName.ifBlank { "胰岛素" },
                    dose = record.lunchInsulin,
                    medTiming = record.lunchMedTiming,
                    dietText = record.lunchDiet,
                    themeColor = AppThemeColors.lunchColor,
                    themeBg = AppThemeColors.lunchBg,
                    themeBorder = AppThemeColors.lunchBorder,
                    onEdit = { onEditPeriod?.invoke(MealPeriod.LUNCH) ?: onEdit() }
                )

                // 3. 傍晚大卡片 (2个血糖数据: 餐前, 餐后)
                PeriodBigCard(
                    title = "🌆 傍晚",
                    periodTag = "晚餐时段",
                    bgItems = listOf(
                        BGItemData(label = "餐前", value = record.preDinnerBG, isPostMeal = false),
                        BGItemData(label = "餐后", value = record.postDinnerBG, isPostMeal = true)
                    ),
                    medName = record.dinnerMedName.ifBlank { "胰岛素" },
                    dose = record.dinnerInsulin,
                    medTiming = record.dinnerMedTiming,
                    dietText = record.dinnerDiet,
                    themeColor = AppThemeColors.dinnerColor,
                    themeBg = AppThemeColors.dinnerBg,
                    themeBorder = AppThemeColors.dinnerBorder,
                    onEdit = { onEditPeriod?.invoke(MealPeriod.DINNER) ?: onEdit() }
                )

                // 4. 睡前大卡片 (仅1个血糖数据: 睡前)
                PeriodBigCard(
                    title = "🌙 睡前",
                    periodTag = "睡前时段",
                    bgItems = listOf(
                        BGItemData(label = "睡前", value = record.preNightBG ?: record.postNightBG, isPostMeal = false)
                    ),
                    medName = record.nightMedName.ifBlank { "胰岛素" },
                    dose = record.bedtimeInsulin,
                    medTiming = record.nightMedTiming,
                    dietText = record.nightDiet,
                    themeColor = AppThemeColors.bedtimeColor,
                    themeBg = AppThemeColors.bedtimeBg,
                    themeBorder = AppThemeColors.bedtimeBorder,
                    onEdit = { onEditPeriod?.invoke(MealPeriod.NIGHT) ?: onEdit() }
                )
            }

            // Optional Notes Section at bottom
            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (AppThemeColors.isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ModeComment,
                            contentDescription = "备注",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = record.notes,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Big card representing one period of the day (Morning, Noon, Evening, Night).
 * - Row 1: Blood glucose readings horizontally distributed
 * - Row 2: Medication data (medicine name & dosage)
 * - Row 3: Meal record (collapsible)
 */
@Composable
private fun PeriodBigCard(
    title: String,
    periodTag: String,
    bgItems: List<BGItemData>,
    medName: String,
    dose: Float?,
    medTiming: String = "餐前",
    dietText: String,
    themeColor: Color,
    themeBg: Color,
    themeBorder: Color,
    prevNightInfo: PrevNightInfo? = null,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDietExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = themeBg),
        border = BorderStroke(1.dp, if (AppThemeColors.isDark) themeBorder else themeBorder.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (AppThemeColors.isDark) 0.dp else 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            // Period Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(themeColor.copy(alpha = if (AppThemeColors.isDark) 0.25f else 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = periodTag,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (AppThemeColors.isDark) themeColor else themeColor.copy(alpha = 0.95f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Optional subtle badge for previous night reference (in morning)
                    if (prevNightInfo != null && prevNightInfo.dose != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppThemeColors.bedtimeColor.copy(alpha = if (AppThemeColors.isDark) 0.2f else 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightlightRound,
                                contentDescription = null,
                                tint = AppThemeColors.bedtimeColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "昨夜: ${if (prevNightInfo.dose % 1f == 0f) prevNightInfo.dose.toInt() else prevNightInfo.dose}U",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (AppThemeColors.isDark) AppThemeColors.bedtimeColor else Color(0xFF4338CA)
                            )
                        }
                    }

                    // 修改按钮
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = themeColor.copy(alpha = if (AppThemeColors.isDark) 0.22f else 0.12f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEdit() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "修改 $title",
                                tint = themeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "修改",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = themeColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // 第一行：血糖与用药并列在单张卡片内（3个数字并列显示）
            // ==========================================
            val autoUnit = com.example.data.MedicationData.detectUnit(medName)
            val displayMedName = medName.ifBlank { "胰岛素" }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (AppThemeColors.isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                       else MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (bgItems.size >= 2) {
                        // 晨间 / 午间 / 傍晚: 3个数字并列 [餐前/空腹] | [餐后] | [胰岛素用量(右侧)]
                        BGNumberColumn(
                            item = bgItems[0],
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.VerticalDivider(
                            modifier = Modifier.height(48.dp),
                            thickness = 0.8.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        BGNumberColumn(
                            item = bgItems[1],
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.VerticalDivider(
                            modifier = Modifier.height(48.dp),
                            thickness = 0.8.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        MedicationNumberColumn(
                            displayMedName = displayMedName,
                            dose = dose,
                            autoUnit = autoUnit,
                            medTiming = medTiming,
                            modifier = Modifier.weight(1f)
                        )
                    } else if (bgItems.size == 1) {
                        // 睡前: 并列 [睡前血糖] | [睡前胰岛素用量(右侧)]
                        BGNumberColumn(
                            item = bgItems[0],
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.VerticalDivider(
                            modifier = Modifier.height(48.dp),
                            thickness = 0.8.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        MedicationNumberColumn(
                            displayMedName = displayMedName,
                            dose = dose,
                            autoUnit = autoUnit,
                            medTiming = medTiming,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 第三行：餐食记录 (这一行可以折叠，内部无边框)
            // ==========================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                color = if (AppThemeColors.isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                       else MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDietExpanded = !isDietExpanded }
                        .padding(horizontal = 11.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = "餐食",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "餐食记录",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!isDietExpanded && dietText.isNotBlank()) {
                                Text(
                                    text = "· $dietText",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 160.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (isDietExpanded) "收起" else if (dietText.isBlank()) "未记录" else "展开",
                                fontSize = 11.sp,
                                color = if (isDietExpanded || dietText.isNotBlank()) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Icon(
                                imageVector = if (isDietExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isDietExpanded) "折叠" else "展开",
                                tint = if (isDietExpanded || dietText.isNotBlank()) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // 展开内容
                    if (isDietExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (dietText.isNotBlank()) {
                            Text(
                                text = dietText,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = "暂无此餐记录（点击卡片右上角编辑按钮可补充餐食记录）",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BGNumberColumn(
    item: BGItemData,
    modifier: Modifier = Modifier
) {
    val status = when {
        item.isFasting -> BGUtils.evaluateFasting(item.value)
        item.isPostMeal -> BGUtils.evaluatePostMeal(item.value)
        else -> BGUtils.evaluatePreMeal(item.value)
    }

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = item.label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.value?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "mmol/L",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (status != null) {
            val (badgeBg, badgeText) = when (status.level) {
                BGLevel.NORMAL -> (if (AppThemeColors.isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5)) to Color(0xFF059669)
                BGLevel.LOW -> (if (AppThemeColors.isDark) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFFFEF2F2)) to Color(0xFFDC2626)
                BGLevel.HIGH -> (if (AppThemeColors.isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFFFBEB)) to Color(0xFFD97706)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeBg)
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = status.label,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeText
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = "未测",
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun MedicationNumberColumn(
    displayMedName: String,
    dose: Float?,
    autoUnit: String,
    medTiming: String = "餐前",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 药物名
        Text(
            text = displayMedName,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        // 2. 数字用量
        Text(
            text = dose?.let { if (it % 1f == 0f) it.toInt().toString() else String.format(Locale.US, "%.1f", it) } ?: "--",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onSurface
        )
        // 3. 单位
        Text(
            text = if (dose != null) autoUnit else "U",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 4. 餐前/餐中/餐后 时机显示（替换原来的“用量”字样，不做颜色区分）
        val timingLabel = if (dose != null && dose > 0) medTiming.ifBlank { "餐前" } else "未用"
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (dose != null && dose > 0) 0.7f else 0.4f))
                .padding(horizontal = 5.dp, vertical = 1.5.dp)
        ) {
            Text(
                text = timingLabel,
                fontSize = 9.5.sp,
                fontWeight = if (dose != null && dose > 0) FontWeight.Bold else FontWeight.Normal,
                color = if (dose != null && dose > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
