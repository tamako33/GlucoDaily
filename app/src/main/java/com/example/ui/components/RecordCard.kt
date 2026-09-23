package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.NightlightRound
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
import com.example.data.MedicationData
import com.example.data.PostMealEntry
import com.example.data.PrevNightInfo
import com.example.ui.ItemType
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import java.util.Locale

@Composable
fun RecordCard(
    record: InsulinRecord,
    prevNightInfo: PrevNightInfo?,
    isToday: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditPeriod: ((MealPeriod) -> Unit)? = null,
    onAddItem: ((MealPeriod, ItemType?) -> Unit)? = null,
    onDeletePostMeal: ((MealPeriod, Int) -> Unit)? = null,
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
            // 1. 卡片顶栏：全天用药胶囊、加条目快捷入口、全量修改、删除
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：全天用药总量
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (AppThemeColors.isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            else TealPrimary.copy(alpha = 0.12f)
                        )
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

                // 右侧：快捷添加条目 + 编辑全天 + 删除
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TealPrimary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAddItem?.invoke(InsulinRecord.getPeriodForTime(), null) }
                            .testTag("card_quick_add_${record.date}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "加一条",
                                tint = TealPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "加一条",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("edit_button_${record.date}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑全部记录",
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

            Spacer(modifier = Modifier.height(4.dp))

            // 2. 纵向时间轴模式 (Vertical Timeline)
            // 依次为：晨间 (早)、午间 (中)、傍晚 (晚)、睡前 (夜)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // 1) 晨间节点 (早)
                TimelinePeriodSection(
                    period = MealPeriod.MORNING,
                    isLast = false,
                    preBG = record.fastingBG ?: record.preBfBG,
                    preBGLabel = "空腹血糖",
                    isFasting = true,
                    medName = record.bfMedName,
                    medDose = record.bfInsulin,
                    medTiming = record.bfMedTiming,
                    dietText = record.bfDiet,
                    postMealList = record.getPostMealList(MealPeriod.MORNING),
                    themeColor = AppThemeColors.breakfastColor,
                    themeBg = AppThemeColors.breakfastBg,
                    themeBorder = AppThemeColors.breakfastBorder,
                    prevNightInfo = prevNightInfo,
                    onEditPeriod = { onEditPeriod?.invoke(MealPeriod.MORNING) ?: onEdit() },
                    onAddItem = { itemType -> onAddItem?.invoke(MealPeriod.MORNING, itemType) },
                    onDeletePostMeal = { idx -> onDeletePostMeal?.invoke(MealPeriod.MORNING, idx) }
                )

                // 2) 午间节点 (中)
                TimelinePeriodSection(
                    period = MealPeriod.LUNCH,
                    isLast = false,
                    preBG = record.preLunchBG,
                    preBGLabel = "餐前血糖",
                    isFasting = false,
                    medName = record.lunchMedName,
                    medDose = record.lunchInsulin,
                    medTiming = record.lunchMedTiming,
                    dietText = record.lunchDiet,
                    postMealList = record.getPostMealList(MealPeriod.LUNCH),
                    themeColor = AppThemeColors.lunchColor,
                    themeBg = AppThemeColors.lunchBg,
                    themeBorder = AppThemeColors.lunchBorder,
                    prevNightInfo = null,
                    onEditPeriod = { onEditPeriod?.invoke(MealPeriod.LUNCH) ?: onEdit() },
                    onAddItem = { itemType -> onAddItem?.invoke(MealPeriod.LUNCH, itemType) },
                    onDeletePostMeal = { idx -> onDeletePostMeal?.invoke(MealPeriod.LUNCH, idx) }
                )

                // 3) 傍晚节点 (晚)
                TimelinePeriodSection(
                    period = MealPeriod.DINNER,
                    isLast = false,
                    preBG = record.preDinnerBG,
                    preBGLabel = "餐前血糖",
                    isFasting = false,
                    medName = record.dinnerMedName,
                    medDose = record.dinnerInsulin,
                    medTiming = record.dinnerMedTiming,
                    dietText = record.dinnerDiet,
                    postMealList = record.getPostMealList(MealPeriod.DINNER),
                    themeColor = AppThemeColors.dinnerColor,
                    themeBg = AppThemeColors.dinnerBg,
                    themeBorder = AppThemeColors.dinnerBorder,
                    prevNightInfo = null,
                    onEditPeriod = { onEditPeriod?.invoke(MealPeriod.DINNER) ?: onEdit() },
                    onAddItem = { itemType -> onAddItem?.invoke(MealPeriod.DINNER, itemType) },
                    onDeletePostMeal = { idx -> onDeletePostMeal?.invoke(MealPeriod.DINNER, idx) }
                )

                // 4) 睡前节点 (睡前)
                TimelinePeriodSection(
                    period = MealPeriod.NIGHT,
                    isLast = true,
                    preBG = record.preNightBG,
                    preBGLabel = "睡前血糖",
                    isFasting = false,
                    medName = record.nightMedName,
                    medDose = record.bedtimeInsulin,
                    medTiming = record.nightMedTiming,
                    dietText = record.nightDiet,
                    postMealList = record.getPostMealList(MealPeriod.NIGHT),
                    themeColor = AppThemeColors.bedtimeColor,
                    themeBg = AppThemeColors.bedtimeBg,
                    themeBorder = AppThemeColors.bedtimeBorder,
                    prevNightInfo = null,
                    onEditPeriod = { onEditPeriod?.invoke(MealPeriod.NIGHT) ?: onEdit() },
                    onAddItem = { itemType -> onAddItem?.invoke(MealPeriod.NIGHT, itemType) },
                    onDeletePostMeal = { idx -> onDeletePostMeal?.invoke(MealPeriod.NIGHT, idx) }
                )
            }

            // 3. 备注栏
            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (AppThemeColors.isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
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
 * 单个时段的纵向时间轴区块 (含左侧时间轴轨道 + 右侧条目卡片)
 */
@Composable
private fun TimelinePeriodSection(
    period: MealPeriod,
    isLast: Boolean,
    preBG: Float?,
    preBGLabel: String,
    isFasting: Boolean,
    medName: String,
    medDose: Float?,
    medTiming: String,
    dietText: String,
    postMealList: List<PostMealEntry>,
    themeColor: Color,
    themeBg: Color,
    themeBorder: Color,
    prevNightInfo: PrevNightInfo? = null,
    onEditPeriod: () -> Unit,
    onAddItem: (ItemType?) -> Unit,
    onDeletePostMeal: (Int) -> Unit
) {
    val hasData = preBG != null || (medDose != null && medDose > 0) || dietText.isNotBlank() || postMealList.isNotEmpty()
    var isDietExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // 左侧时间轴轨道：圆点 + 垂直连接线
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight()
        ) {
            // 时间轴节点圆圈
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = if (AppThemeColors.isDark) 0.25f else 0.15f))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.iconText,
                    fontSize = 14.sp
                )
            }

            // 纵向连接线（最后一段不显示下方延伸线）
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .fillMaxHeight()
                        .background(themeColor.copy(alpha = if (AppThemeColors.isDark) 0.35f else 0.22f))
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 右侧内容卡片：纵向排列各项条目
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 4.dp else 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeBg),
            border = BorderStroke(1.dp, if (AppThemeColors.isDark) themeBorder else themeBorder.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (AppThemeColors.isDark) 0.dp else 0.5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // 1. 时段标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = period.title,
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
                                text = period.periodTag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (AppThemeColors.isDark) themeColor else themeColor.copy(alpha = 0.95f)
                            )
                        }
                    }

                    // 右侧动作：昨夜用药标签 + 加条目 + 修改
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (prevNightInfo != null && prevNightInfo.dose > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AppThemeColors.bedtimeColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightlightRound,
                                    contentDescription = null,
                                    tint = AppThemeColors.bedtimeColor,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "昨夜: ${if (prevNightInfo.dose % 1f == 0f) prevNightInfo.dose.toInt() else prevNightInfo.dose}U",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppThemeColors.bedtimeColor
                                )
                            }
                        }

                        // "+ 加条目" 按钮（一次增加一个条目）
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = themeColor.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onAddItem(null) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "在此段增加条目",
                                    tint = themeColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "加条目",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                            }
                        }

                        // "修改" 按钮
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onEditPeriod() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "修改本时段全部",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "修改",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 若该时段完全无数据，显示清爽引导添加行
                if (!hasData) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddItem(null) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "暂无${period.title}记录",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "+ 记一笔",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                            }
                        }
                    }
                } else {
                    // 该时段有数据：按纵向时间轴流展示各个已录入条目
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1) 餐前血糖 / 空腹血糖
                        TimelineItemPreBGRow(
                            label = preBGLabel,
                            value = preBG,
                            isFasting = isFasting,
                            onAdd = { onAddItem(ItemType.PRE_MEAL_BG) }
                        )

                        // 2) 用药记录
                        TimelineItemMedicationRow(
                            medName = medName,
                            dose = medDose,
                            timing = medTiming,
                            onAdd = { onAddItem(ItemType.MEDICATION) }
                        )

                        // 3) 用餐情况
                        TimelineItemDietRow(
                            dietText = dietText,
                            isExpanded = isDietExpanded,
                            onToggleExpand = { isDietExpanded = !isDietExpanded },
                            onAdd = { onAddItem(ItemType.DIET) }
                        )

                        // 4) 餐后血糖 (支持多条！)
                        TimelineItemPostBGSection(
                            period = period,
                            postMealList = postMealList,
                            themeColor = themeColor,
                            onAddAnother = { onAddItem(ItemType.POST_MEAL_BG) },
                            onDelete = onDeletePostMeal
                        )
                    }
                }
            }
        }
    }
}

/**
 * 纵向时间轴条目 1：餐前/空腹血糖行
 */
@Composable
private fun TimelineItemPreBGRow(
    label: String,
    value: Float?,
    isFasting: Boolean,
    onAdd: () -> Unit
) {
    val status = if (isFasting) BGUtils.evaluateFasting(value) else BGUtils.evaluatePreMeal(value)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (AppThemeColors.isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "🩸", fontSize = 13.sp)
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (value != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", value),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "mmol/L",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (status != null) {
                        val (bg, fg) = when (status.level) {
                            BGLevel.NORMAL -> (if (AppThemeColors.isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5)) to Color(0xFF059669)
                            BGLevel.LOW -> (if (AppThemeColors.isDark) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFFFEF2F2)) to Color(0xFFDC2626)
                            BGLevel.HIGH -> (if (AppThemeColors.isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFFFBEB)) to Color(0xFFD97706)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(bg)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = status.label,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = fg
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAdd() }
                ) {
                    Text(
                        text = "未测",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ 补录",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealPrimary
                    )
                }
            }
        }
    }
}

/**
 * 纵向时间轴条目 2：用药记录行
 */
@Composable
private fun TimelineItemMedicationRow(
    medName: String,
    dose: Float?,
    timing: String,
    onAdd: () -> Unit
) {
    val hasMed = dose != null && dose > 0
    val displayMed = medName.ifBlank { "胰岛素" }
    val unit = MedicationData.detectUnit(displayMed)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (AppThemeColors.isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "💊", fontSize = 13.sp)
                Text(
                    text = "用药记录",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (hasMed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayMed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (dose % 1f == 0f) dose.toInt().toString() else String.format(Locale.US, "%.1f", dose),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = TealPrimary
                    )
                    Text(
                        text = unit,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = timing.ifBlank { "餐前" },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAdd() }
                ) {
                    Text(
                        text = "未用药",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ 录入",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealPrimary
                    )
                }
            }
        }
    }
}

/**
 * 纵向时间轴条目 3：用餐情况行 (支持展开折叠)
 */
@Composable
private fun TimelineItemDietRow(
    dietText: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAdd: () -> Unit
) {
    val hasDiet = dietText.isNotBlank()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (AppThemeColors.isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = hasDiet, onClick = onToggleExpand)
                .padding(horizontal = 10.dp, vertical = 7.dp)
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
                    Text(text = "🍽️", fontSize = 13.sp)
                    Text(
                        text = "用餐情况",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasDiet && !isExpanded) {
                        Text(
                            text = "· $dietText",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (hasDiet) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = TealPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onAdd() }
                    ) {
                        Text(
                            text = "未记录",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ 记录食谱",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealPrimary
                        )
                    }
                }
            }

            // 展开状态完整食谱内容
            if (hasDiet && isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dietText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 纵向时间轴条目 4：餐后血糖区域（支持该时段增加多条，依次纵向列出）
 */
@Composable
private fun TimelineItemPostBGSection(
    period: MealPeriod,
    postMealList: List<PostMealEntry>,
    themeColor: Color,
    onAddAnother: () -> Unit,
    onDelete: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (AppThemeColors.isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            // 头部标题 + 加测按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🩸", fontSize = 13.sp)
                    Text(
                        text = "餐后血糖",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (postMealList.size > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TealPrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${postMealList.size}条记录",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }
                    }
                }

                // 快速加测按钮
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TealPrimary.copy(alpha = 0.1f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onAddAnother() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (postMealList.isEmpty()) "+ 录入" else "+ 加测餐后",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    }
                }
            }

            // 餐后血糖条目列表
            if (postMealList.isEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "尚未测量餐后血糖",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 20.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    postMealList.forEachIndexed { index, entry ->
                        val status = BGUtils.evaluatePostMeal(entry.value)
                        val (bg, fg) = when (status?.level) {
                            BGLevel.NORMAL -> (if (AppThemeColors.isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5)) to Color(0xFF059669)
                            BGLevel.LOW -> (if (AppThemeColors.isDark) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFFFEF2F2)) to Color(0xFFDC2626)
                            BGLevel.HIGH -> (if (AppThemeColors.isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFFFBEB)) to Color(0xFFD97706)
                            null -> Color.Transparent to MaterialTheme.colorScheme.onSurface
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 标签与时间点
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val tagDisplay = when {
                                    entry.tag.isNotBlank() -> entry.tag
                                    postMealList.size > 1 -> "餐后 #${index + 1}"
                                    else -> "餐后"
                                }
                                Text(
                                    text = tagDisplay,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (entry.time.isNotBlank()) {
                                    Text(
                                        text = entry.time,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // 数值、单位、达标徽章、删除按钮
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", entry.value),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "mmol/L",
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                if (status != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(bg)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = status.label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = fg
                                        )
                                    }
                                }
                                // 删除该条餐后血糖按钮
                                IconButton(
                                    onClick = { onDelete(index) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "删除该条餐后血糖",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
