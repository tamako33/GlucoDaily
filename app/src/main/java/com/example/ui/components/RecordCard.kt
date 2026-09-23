package com.example.ui.components

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
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

/**
 * 纵向时间轴记录卡片（极简无边框设计）：
 * - 无沉重的大卡片与边框，纯净平铺于时间轴上
 * - 默认无记录时不渲染晨间/午间/傍晚/睡前 4 个空框，只显示极简轻量空提示与“记第一笔”
 * - 仅展示实际录入了数据的时段节点与条目，未录入的时段不抢占空间
 * - 每个已录条目（血糖、用药、餐食、餐后血糖）均为独立无边框轻卡片
 */
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
    val activePeriods = buildList {
        if (record.hasMorningData) add(MealPeriod.MORNING)
        if (record.hasLunchData) add(MealPeriod.LUNCH)
        if (record.hasDinnerData) add(MealPeriod.DINNER)
        if (record.hasNightData) add(MealPeriod.NIGHT)
    }
    val hasAnyData = activePeriods.isNotEmpty() || record.notes.isNotBlank()

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
            // 1. 卡片顶栏：全天用药总量胶囊 + 加一条快捷入口 + 编辑全天 + 删除
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
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
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
                                fontSize = 12.sp,
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

            Spacer(modifier = Modifier.height(4.dp))

            // 2. 纵向时间轴流 (Vertical Timeline)
            if (!hasAnyData) {
                // 默认完全无记录时：不显示晨午晚夜四个空大框，呈现极简轻量空提示
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (AppThemeColors.isDark) 0.22f else 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🗓️ 该日暂无记录",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "点击下方按钮记录今天的第一笔数据",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = TealPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onAddItem?.invoke(InsulinRecord.getPeriodForTime(), null) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "记第一笔",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                // 有数据时：仅渲染真正录入了数据的时段节点与条目（无边框，极简平铺）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    activePeriods.forEachIndexed { index, period ->
                        val isLast = index == activePeriods.lastIndex

                        val (preBG, preBGLabel, isFasting) = when (period) {
                            MealPeriod.MORNING -> Triple(record.fastingBG ?: record.preBfBG, "空腹血糖", true)
                            MealPeriod.LUNCH -> Triple(record.preLunchBG, "餐前血糖", false)
                            MealPeriod.DINNER -> Triple(record.preDinnerBG, "餐前血糖", false)
                            MealPeriod.NIGHT -> Triple(record.preNightBG, "睡前血糖", false)
                        }

                        val (medName, medDose, medTiming) = when (period) {
                            MealPeriod.MORNING -> Triple(record.bfMedName, record.bfInsulin, record.bfMedTiming)
                            MealPeriod.LUNCH -> Triple(record.lunchMedName, record.lunchInsulin, record.lunchMedTiming)
                            MealPeriod.DINNER -> Triple(record.dinnerMedName, record.dinnerInsulin, record.dinnerMedTiming)
                            MealPeriod.NIGHT -> Triple(record.nightMedName, record.bedtimeInsulin, record.nightMedTiming)
                        }

                        val dietText = when (period) {
                            MealPeriod.MORNING -> record.bfDiet
                            MealPeriod.LUNCH -> record.lunchDiet
                            MealPeriod.DINNER -> record.dinnerDiet
                            MealPeriod.NIGHT -> record.nightDiet
                        }

                        val exerciseText = when (period) {
                            MealPeriod.MORNING -> record.bfExercise
                            MealPeriod.LUNCH -> record.lunchExercise
                            MealPeriod.DINNER -> record.dinnerExercise
                            MealPeriod.NIGHT -> record.nightExercise
                        }

                        val themeColor = when (period) {
                            MealPeriod.MORNING -> AppThemeColors.breakfastColor
                            MealPeriod.LUNCH -> AppThemeColors.lunchColor
                            MealPeriod.DINNER -> AppThemeColors.dinnerColor
                            MealPeriod.NIGHT -> AppThemeColors.bedtimeColor
                        }

                        TimelinePeriodSection(
                            record = record,
                            period = period,
                            isLast = isLast,
                            preBG = preBG,
                            preBGLabel = preBGLabel,
                            isFasting = isFasting,
                            medName = medName,
                            medDose = medDose,
                            medTiming = medTiming,
                            dietText = dietText,
                            exerciseText = exerciseText,
                            postMealList = record.getPostMealList(period),
                            themeColor = themeColor,
                            prevNightInfo = if (period == MealPeriod.MORNING) prevNightInfo else null,
                            onEditPeriod = { onEditPeriod?.invoke(period) ?: onEdit() },
                            onAddItem = { itemType -> onAddItem?.invoke(period, itemType) },
                            onDeletePostMeal = { idx -> onDeletePostMeal?.invoke(period, idx) }
                        )
                    }

                    // 时段下方微型添加引导
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 38.dp, top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TealPrimary.copy(alpha = 0.1f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAddItem?.invoke(InsulinRecord.getPeriodForTime(), null) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "记录新条目",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 3. 备注栏（若有备注则显示）
            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (AppThemeColors.isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
 * 单个时段的纵向时间轴区块（竖排文字、时段色彩竖线、无大边框）
 */
@Composable
private fun TimelinePeriodSection(
    record: InsulinRecord,
    period: MealPeriod,
    isLast: Boolean,
    preBG: Float?,
    preBGLabel: String,
    isFasting: Boolean,
    medName: String,
    medDose: Float?,
    medTiming: String,
    dietText: String,
    exerciseText: String,
    postMealList: List<PostMealEntry>,
    themeColor: Color,
    prevNightInfo: PrevNightInfo? = null,
    onEditPeriod: () -> Unit,
    onAddItem: (ItemType?) -> Unit,
    onDeletePostMeal: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // 左侧时间轴轨道：节点圆圈 + 竖线段 + 竖排文字 + 贯穿竖线（全使用时段专属主题色！）
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
        ) {
            // 顶部时段图标圆圈
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = if (AppThemeColors.isDark) 0.25f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.iconText,
                    fontSize = 12.sp
                )
            }

            // 竖线段 1（时段主题色）
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(6.dp)
                    .background(themeColor.copy(alpha = 0.55f))
            )

            // 竖排时段文字（晨\n间 等），时段专属主题色
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(themeColor.copy(alpha = if (AppThemeColors.isDark) 0.25f else 0.14f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    period.title.forEach { ch ->
                        Text(
                            text = ch.toString(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            lineHeight = 11.sp
                        )
                    }
                }
            }

            // 竖线段 2：使用该时段主题色贯穿向下
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .weight(1f)
                    .background(themeColor.copy(alpha = 0.45f))
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 右侧无边框条目流：仅排列有实际数据的条目
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 2.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. 顶栏操作区（删去红色框住的“早餐时段”等标识，与左侧图标对齐）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (prevNightInfo != null && prevNightInfo.dose > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppThemeColors.bedtimeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "昨夜: ${if (prevNightInfo.dose % 1f == 0f) prevNightInfo.dose.toInt() else prevNightInfo.dose}U",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppThemeColors.bedtimeColor
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onAddItem(null) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "在此段增加条目",
                            tint = themeColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onEditPeriod,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "修改本段",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // 2. 独立条目：仅渲染有数据的项目（上方带微型时间，无灰底色）
            // 1) 餐前 / 空腹血糖
            if (preBG != null) {
                TimelineItemPreBGRow(
                    label = preBGLabel,
                    value = preBG,
                    isFasting = isFasting,
                    timeText = record.getItemTime(period, "preBG")
                )
            }

            // 2) 用药记录
            if (medDose != null && medDose > 0) {
                TimelineItemMedicationRow(
                    medName = medName,
                    dose = medDose,
                    timing = medTiming,
                    timeText = record.getItemTime(period, "med")
                )
            }

            // 3) 用餐情况
            if (dietText.isNotBlank()) {
                TimelineItemDietRow(
                    dietText = dietText,
                    timeText = record.getItemTime(period, "diet")
                )
            }

            // 4) 运动记录
            if (exerciseText.isNotBlank()) {
                TimelineItemExerciseRow(
                    exerciseText = exerciseText,
                    timeText = record.getItemTime(period, "exercise")
                )
            }

            // 5) 餐后血糖（支持多条，每条为独立条目，上方带时间）
            postMealList.forEachIndexed { idx, entry ->
                TimelineItemPostBGRow(
                    entry = entry,
                    fallbackTime = when (period) {
                        MealPeriod.MORNING -> "10:00"
                        MealPeriod.LUNCH -> "14:15"
                        MealPeriod.DINNER -> "20:15"
                        MealPeriod.NIGHT -> "23:00"
                    },
                    onDelete = { onDeletePostMeal(idx) }
                )
            }
        }
    }
}

/**
 * 纵向时间轴条目：餐前/空腹血糖
 * - 老年友好大字号（数值 21sp Bold）
 * - 偏高/达标/偏低状态标签居左
 * - 数值与单位齐右
 * - 独立无底色，上方附一行小记录时间
 */
@Composable
private fun TimelineItemPreBGRow(
    label: String,
    value: Float,
    isFasting: Boolean,
    timeText: String
) {
    val status = if (isFasting) BGUtils.evaluateFasting(value) else BGUtils.evaluatePreMeal(value)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        if (timeText.isNotBlank()) {
            Text(
                text = timeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：图标 + 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(text = "🩸", fontSize = 16.sp)
                Text(
                    text = label,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 右侧：标签居左，数值居右对齐
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (status != null) {
                    val (bg, fg) = when (status.level) {
                        BGLevel.NORMAL -> (if (AppThemeColors.isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5)) to Color(0xFF059669)
                        BGLevel.LOW -> (if (AppThemeColors.isDark) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFFFEF2F2)) to Color(0xFFDC2626)
                        BGLevel.HIGH -> (if (AppThemeColors.isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFFFBEB)) to Color(0xFFD97706)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(bg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = status.label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = fg
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", value),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "mmol/L",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 纵向时间轴条目：用药记录
 * - 老年友好大字号（剂量 21sp Bold）
 * - 餐前/睡前时机标签在数字左边
 * - 剂量数值与单位齐右
 * - 独立无底色，上方附一行小记录时间
 */
@Composable
private fun TimelineItemMedicationRow(
    medName: String,
    dose: Float,
    timing: String,
    timeText: String
) {
    val displayMed = medName.ifBlank { "胰岛素" }
    val unit = MedicationData.detectUnit(displayMed)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        if (timeText.isNotBlank()) {
            Text(
                text = timeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：图标 + 药名
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(text = "💊", fontSize = 16.sp)
                Text(
                    text = displayMed,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 右侧：时机标签在左，剂量数字齐右
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (timing.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (AppThemeColors.isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = timing,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = if (dose % 1f == 0f) "${dose.toInt()}" else "$dose",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                    Text(
                        text = unit,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 纵向时间轴条目：用餐情况（无底色，上方附一行小记录时间）
 */
@Composable
private fun TimelineItemDietRow(
    dietText: String,
    timeText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        if (timeText.isNotBlank()) {
            Text(
                text = timeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🍽️", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = dietText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * 纵向时间轴条目：运动记录（无底色，上方附一行小记录时间）
 */
@Composable
private fun TimelineItemExerciseRow(
    exerciseText: String,
    timeText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        if (timeText.isNotBlank()) {
            Text(
                text = timeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🏃", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = exerciseText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * 纵向时间轴条目：单笔餐后血糖
 * - 老年友好大字号（数值 21sp Bold）
 * - 偏高/达标/偏低状态标签居左
 * - 数值与单位齐右
 * - 独立无底色，上方附一行小记录时间
 */
@Composable
private fun TimelineItemPostBGRow(
    entry: PostMealEntry,
    fallbackTime: String,
    onDelete: () -> Unit
) {
    val status = BGUtils.evaluatePostMeal(entry.value)
    val displayTime = entry.time.ifBlank { fallbackTime }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        if (displayTime.isNotBlank()) {
            Text(
                text = displayTime,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：图标 + 餐后阶段标签
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(text = "📈", fontSize = 16.sp)
                Text(
                    text = entry.tag.ifBlank { "餐后血糖" },
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 右侧：偏高标签在左，数值在右齐右，最后带删除按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (status != null) {
                    val (bg, fg) = when (status.level) {
                        BGLevel.NORMAL -> (if (AppThemeColors.isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5)) to Color(0xFF059669)
                        BGLevel.LOW -> (if (AppThemeColors.isDark) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFFFEF2F2)) to Color(0xFFDC2626)
                        BGLevel.HIGH -> (if (AppThemeColors.isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFFFBEB)) to Color(0xFFD97706)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(bg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = status.label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = fg
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", entry.value),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "mmol/L",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除该笔餐后血糖",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
