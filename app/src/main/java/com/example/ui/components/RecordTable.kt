package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BGLevel
import com.example.data.BGUtils
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.data.MedicationData
import com.example.data.PrevNightInfo
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordTable(
    records: List<InsulinRecord>,
    allRecords: List<InsulinRecord>,
    todayStr: String,
    onEdit: (InsulinRecord) -> Unit,
    onDelete: (InsulinRecord) -> Unit,
    modifier: Modifier = Modifier,
    isEnlarged: Boolean = false,
    zoomScale: Float = 1.0f
) {
    // 单元格长按放大弹窗状态
    var zoomDetail by remember { mutableStateOf<CellZoomDetail?>(null) }
    // 根据用户实际的用药记录推断各餐段用药列标题和单位（优先从当前表格记录获取最新选择的用药，保证即时响应用户选择）
    val latestBfMed = records.firstOrNull { it.bfMedName.isNotBlank() }?.bfMedName
        ?: allRecords.firstOrNull { it.bfMedName.isNotBlank() }?.bfMedName
        ?: "胰岛素"
    val latestLunchMed = records.firstOrNull { it.lunchMedName.isNotBlank() }?.lunchMedName
        ?: allRecords.firstOrNull { it.lunchMedName.isNotBlank() }?.lunchMedName
        ?: "胰岛素"
    val latestDinnerMed = records.firstOrNull { it.dinnerMedName.isNotBlank() }?.dinnerMedName
        ?: allRecords.firstOrNull { it.dinnerMedName.isNotBlank() }?.dinnerMedName
        ?: "胰岛素"
    val latestNightMed = records.firstOrNull { it.nightMedName.isNotBlank() }?.nightMedName
        ?: allRecords.firstOrNull { it.nightMedName.isNotBlank() }?.nightMedName
        ?: "胰岛素"

    val bfColTitle = MedicationData.getColumnHeaderTitle(latestBfMed)
    val lunchColTitle = MedicationData.getColumnHeaderTitle(latestLunchMed)
    val dinnerColTitle = MedicationData.getColumnHeaderTitle(latestDinnerMed)
    val bedtimeColTitle = MedicationData.getColumnHeaderTitle(latestNightMed)

    // 预先在 Composable 上下文中提取颜色，供单元格长按放大回调中使用
    val cGlucoseLow = AppThemeColors.glucoseLow
    val cGlucoseNormal = AppThemeColors.glucoseNormal
    val cGlucoseHigh = AppThemeColors.glucoseHigh
    val cBreakfastColor = AppThemeColors.breakfastColor
    val cLunchColor = AppThemeColors.lunchColor
    val cDinnerColor = AppThemeColors.dinnerColor
    val cBedtimeColor = AppThemeColors.bedtimeColor

    // 缩放系数约束在合理范围（0.75x ~ 2.5x）
    val s = zoomScale.coerceIn(0.75f, 2.5f)

    // 列宽定义：针对明细模式展示全部列，结合双指缩放系数动态自适应
    val dateColWidth: Dp = ((if (isEnlarged) 96f else 88f) * s).dp

    // 早餐段列宽
    val prevNightWidth: Dp = ((if (isEnlarged) 76f else 72f) * s).dp
    val fastingWidth: Dp = ((if (isEnlarged) 78f else 76f) * s).dp
    val bfMedWidth: Dp = ((if (isEnlarged) 82f else 78f) * s).dp
    val postBfWidth: Dp = ((if (isEnlarged) 78f else 76f) * s).dp
    val bfDietWidth: Dp = (105f * s).dp
    val bfGroupTotalWidth = if (isEnlarged) {
        prevNightWidth + fastingWidth + bfMedWidth + postBfWidth + bfDietWidth
    } else {
        prevNightWidth + fastingWidth + bfMedWidth + postBfWidth
    }

    // 午餐段列宽
    val preLunchWidth: Dp = (78f * s).dp
    val lunchMedWidth: Dp = ((if (isEnlarged) 82f else 78f) * s).dp
    val postLunchWidth: Dp = ((if (isEnlarged) 78f else 78f) * s).dp
    val lunchDietWidth: Dp = (105f * s).dp
    val lunchGroupTotalWidth = if (isEnlarged) {
        preLunchWidth + lunchMedWidth + postLunchWidth + lunchDietWidth
    } else {
        lunchMedWidth + postLunchWidth
    }

    // 晚餐段列宽
    val preDinnerWidth: Dp = (78f * s).dp
    val dinnerMedWidth: Dp = ((if (isEnlarged) 82f else 78f) * s).dp
    val postDinnerWidth: Dp = ((if (isEnlarged) 78f else 78f) * s).dp
    val dinnerDietWidth: Dp = (105f * s).dp
    val dinnerGroupTotalWidth = if (isEnlarged) {
        preDinnerWidth + dinnerMedWidth + postDinnerWidth + dinnerDietWidth
    } else {
        dinnerMedWidth + postDinnerWidth
    }

    // 睡前段列宽
    val preNightWidth: Dp = (78f * s).dp
    val bedtimeMedWidth: Dp = ((if (isEnlarged) 82f else 80f) * s).dp
    val nightDietWidth: Dp = (95f * s).dp
    val bedtimeGroupTotalWidth = if (isEnlarged) {
        preNightWidth + bedtimeMedWidth + nightDietWidth
    } else {
        bedtimeMedWidth
    }

    // 统计与操作
    val totalInsulinWidth: Dp = ((if (isEnlarged) 80f else 74f) * s).dp
    val notesWidth: Dp = ((if (isEnlarged) 140f else 130f) * s).dp
    val actionsWidth: Dp = ((if (isEnlarged) 88f else 84f) * s).dp

    // 字号与行高（随缩放平滑适配，大字模式清晰醒目）
    val headerFontSize = ((if (isEnlarged) 14.5f else 13f) * s).sp
    val subHeaderFontSize = ((if (isEnlarged) 12.5f else 11.5f) * s).sp
    val dataFontSize = ((if (isEnlarged) 14.5f else 13f) * s).sp
    val rowHeight: Dp = ((if (isEnlarged) 54f else 46f) * s).dp

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("record_table_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = if (isEnlarged) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
        ) {
            // === 1. 上方表头 (上方固定，不随垂直滑动滚走) ===
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = if (isEnlarged) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左上角固定单元格：餐段 / 日期 (水平和垂直方向均固定)
                    Column(
                        modifier = Modifier
                            .width(dateColWidth)
                            .padding(vertical = if (isEnlarged) 6.dp else 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TableHeaderCell("餐段", dateColWidth, fontSize = headerFontSize, color = MaterialTheme.colorScheme.onSurface, bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        Spacer(modifier = Modifier.height(2.dp))
                        TableSubHeaderCell("日期", dateColWidth, fontSize = subHeaderFontSize, isLabel = true)
                    }

                    // 左边表头与数据区分隔线
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(((if (isEnlarged) 68f else 60f) * s).dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    )

                    // 上方表头列：随水平滑动同步滚动
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = if (isEnlarged) 6.dp else 4.dp)
                        ) {
                            // Row 1: Period Categories
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TableHeaderCell("🌅 早餐", bfGroupTotalWidth, fontSize = headerFontSize, color = AppThemeColors.breakfastColor, bgColor = AppThemeColors.breakfastHeaderBg)
                                TableHeaderCell("☀️ 午餐", lunchGroupTotalWidth, fontSize = headerFontSize, color = AppThemeColors.lunchColor, bgColor = AppThemeColors.lunchHeaderBg)
                                TableHeaderCell("🌙 晚餐", dinnerGroupTotalWidth, fontSize = headerFontSize, color = AppThemeColors.dinnerColor, bgColor = AppThemeColors.dinnerHeaderBg)
                                TableHeaderCell("🛌 睡前", bedtimeGroupTotalWidth, fontSize = headerFontSize, color = AppThemeColors.bedtimeColor, bgColor = AppThemeColors.bedtimeHeaderBg)
                                TableHeaderCell("总剂量", totalInsulinWidth, fontSize = headerFontSize, color = TealPrimary, bgColor = TealPrimary.copy(alpha = 0.15f))
                                TableHeaderCell("备注", notesWidth, fontSize = headerFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant, bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                TableHeaderCell("操作", actionsWidth, fontSize = headerFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant, bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Row 2: Sub-columns
                            Row(
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 早餐子项
                                TableSubHeaderCell("昨睡前", prevNightWidth, fontSize = subHeaderFontSize)
                                TableSubHeaderCell("空腹", fastingWidth, fontSize = subHeaderFontSize)
                                TableSubHeaderCell(bfColTitle, bfMedWidth, fontSize = subHeaderFontSize)
                                TableSubHeaderCell("餐后2h", postBfWidth, fontSize = subHeaderFontSize)
                                if (isEnlarged) {
                                    TableSubHeaderCell("饮食", bfDietWidth, fontSize = subHeaderFontSize)
                                }

                                // 午餐子项
                                if (isEnlarged) {
                                    TableSubHeaderCell("餐前", preLunchWidth, fontSize = subHeaderFontSize)
                                }
                                TableSubHeaderCell(lunchColTitle, lunchMedWidth, fontSize = subHeaderFontSize)
                                TableSubHeaderCell("餐后2h", postLunchWidth, fontSize = subHeaderFontSize)
                                if (isEnlarged) {
                                    TableSubHeaderCell("饮食", lunchDietWidth, fontSize = subHeaderFontSize)
                                }

                                // 晚餐子项
                                if (isEnlarged) {
                                    TableSubHeaderCell("餐前", preDinnerWidth, fontSize = subHeaderFontSize)
                                }
                                TableSubHeaderCell(dinnerColTitle, dinnerMedWidth, fontSize = subHeaderFontSize)
                                TableSubHeaderCell("餐后2h", postDinnerWidth, fontSize = subHeaderFontSize)
                                if (isEnlarged) {
                                    TableSubHeaderCell("饮食", dinnerDietWidth, fontSize = subHeaderFontSize)
                                }

                                // 睡前子项
                                if (isEnlarged) {
                                    TableSubHeaderCell("睡前", preNightWidth, fontSize = subHeaderFontSize)
                                }
                                TableSubHeaderCell(bedtimeColTitle, bedtimeMedWidth, fontSize = subHeaderFontSize)
                                if (isEnlarged) {
                                    TableSubHeaderCell("加餐", nightDietWidth, fontSize = subHeaderFontSize)
                                }

                                // 总剂量, 备注, 操作
                                TableSubHeaderCell("U", totalInsulinWidth, fontSize = subHeaderFontSize)
                                TableSubHeaderCell("", notesWidth, fontSize = subHeaderFontSize)
                                TableSubHeaderCell("", actionsWidth, fontSize = subHeaderFontSize)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), thickness = 1.dp)

            // === 2. 表格数据体 (垂直滚动，左边日期列固定不随水平滚动滑动) ===
            val bodyModifier = if (isEnlarged) {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(verticalScrollState)
            } else {
                Modifier.fillMaxWidth()
            }

            Box(modifier = bodyModifier) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // 左边表头 (日期列)：水平固定在左侧，随垂直滚动与右侧数据行同步移动
                    Column(
                        modifier = Modifier
                            .width(dateColWidth)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        records.forEach { record ->
                            val isToday = record.date == todayStr
                            Box(
                                modifier = Modifier
                                    .width(dateColWidth)
                                    .height(rowHeight)
                                    .background(
                                        if (isToday) TealPrimary.copy(alpha = if (AppThemeColors.isDark) 0.14f else 0.08f) else Color.Transparent
                                    )
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = record.date.substring(5),
                                        fontSize = dataFontSize,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isToday) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = TealPrimary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "今天",
                                                fontSize = 10.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                        }
                    }

                    // 左边固定列与右侧数据区分隔线
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(rowHeight * records.size + (records.size * 1).dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    )

                    // 右侧数据列：随水平滚动滑动，每行高度与左侧日期严格保持一致
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        Column {
                            records.forEach { record ->
                                val isToday = record.date == todayStr
                                val prevNight = BGUtils.getPrevNightInsulin(record.date, allRecords)

                                Row(
                                    modifier = Modifier
                                        .height(rowHeight)
                                        .background(
                                            if (isToday) TealPrimary.copy(alpha = if (AppThemeColors.isDark) 0.14f else 0.08f) else Color.Transparent
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // --- 🌅 早餐段数据 ---
                                    TablePrevNightCell(
                                        prevNight = prevNight,
                                        width = prevNightWidth,
                                        fontSize = dataFontSize,
                                        onLongClick = if (prevNight != null && prevNight.dose > 0) {
                                            {
                                                val unit = MedicationData.detectUnit(prevNight.medName)
                                                val doseStr = if (prevNight.dose % 1f == 0f) "${prevNight.dose.toInt()}$unit" else "${prevNight.dose}$unit"
                                                val medDesc = if (prevNight.medName.isNotBlank()) prevNight.medName else "前晚睡前胰岛素"
                                                zoomDetail = CellZoomDetail(
                                                    title = "🛌 前晚 · 睡前用药",
                                                    date = record.date,
                                                    mainText = doseStr,
                                                    subText = "$medDesc · 用于评估空腹基础血糖",
                                                    highlightColor = cBedtimeColor
                                                )
                                            }
                                        } else null
                                    )
                                    TableBGCellContainer(
                                        bg = record.fastingBG ?: record.preBfBG,
                                        width = fastingWidth,
                                        isFasting = true,
                                        fontSize = dataFontSize,
                                        onLongClick = (record.fastingBG ?: record.preBfBG)?.let { bgVal ->
                                            {
                                                val status = BGUtils.evaluateFasting(bgVal)
                                                val levelDesc = when (status?.level) {
                                                    BGLevel.LOW -> "⚠️ 偏低 (空腹参考标准: 3.9 ~ 6.1 mmol/L)"
                                                    BGLevel.HIGH -> "⚠️ 偏高 (空腹参考标准: 3.9 ~ 6.1 mmol/L)"
                                                    BGLevel.NORMAL -> "✅ 正常理想范围 (3.9 ~ 6.1 mmol/L)"
                                                    null -> ""
                                                }
                                                val fgColor = when (status?.level) {
                                                    BGLevel.LOW -> cGlucoseLow
                                                    BGLevel.NORMAL -> cGlucoseNormal
                                                    BGLevel.HIGH -> cGlucoseHigh
                                                    null -> TealPrimary
                                                }
                                                zoomDetail = CellZoomDetail(
                                                    title = "🌅 早餐 · 空腹血糖",
                                                    date = record.date,
                                                    mainText = "${String.format(Locale.US, "%.1f", bgVal)} mmol/L",
                                                    subText = levelDesc,
                                                    highlightColor = fgColor
                                                )
                                            }
                                        }
                                    )
                                    TableMedicationCell(
                                        dose = record.bfInsulin,
                                        medName = record.bfMedName,
                                        width = bfMedWidth,
                                        medTiming = record.bfMedTiming,
                                        fontSize = dataFontSize,
                                        onLongClick = if (record.bfInsulin != null && record.bfInsulin > 0) {
                                            {
                                                val unit = MedicationData.detectUnit(record.bfMedName)
                                                val doseStr = if (record.bfInsulin % 1f == 0f) "${record.bfInsulin.toInt()}$unit" else "${String.format(Locale.US, "%.1f", record.bfInsulin)}$unit"
                                                val medDesc = buildString {
                                                    if (record.bfMedName.isNotBlank()) append(record.bfMedName) else append("早餐用药")
                                                    if (record.bfMedTiming.isNotBlank()) append(" · ${record.bfMedTiming}")
                                                }
                                                zoomDetail = CellZoomDetail(
                                                    title = "🌅 早餐 · 用药",
                                                    date = record.date,
                                                    mainText = doseStr,
                                                    subText = medDesc,
                                                    highlightColor = TealPrimary
                                                )
                                            }
                                        } else null
                                    )
                                    val bfPosts = record.getPostMealList(MealPeriod.MORNING)
                                    TableBGCellContainer(
                                        bg = record.postBfBG,
                                        width = postBfWidth,
                                        isFasting = false,
                                        fontSize = dataFontSize,
                                        extraCount = (bfPosts.size - 1).coerceAtLeast(0),
                                        onLongClick = record.postBfBG?.let { bgVal ->
                                            {
                                                val status = BGUtils.evaluatePostMeal(bgVal)
                                                val levelDesc = when (status?.level) {
                                                    BGLevel.LOW -> "⚠️ 偏低 (餐后2h参考标准: 4.4 ~ 7.8 mmol/L)"
                                                    BGLevel.HIGH -> "⚠️ 偏高 (餐后2h参考标准: 4.4 ~ 7.8 mmol/L)"
                                                    BGLevel.NORMAL -> "✅ 正常理想范围 (4.4 ~ 7.8 mmol/L)"
                                                    null -> ""
                                                }
                                                val fgColor = when (status?.level) {
                                                    BGLevel.LOW -> cGlucoseLow
                                                    BGLevel.NORMAL -> cGlucoseNormal
                                                    BGLevel.HIGH -> cGlucoseHigh
                                                    null -> TealPrimary
                                                }
                                                val extraDetail = if (bfPosts.size > 1) {
                                                    "\n" + bfPosts.mapIndexed { i, e -> "第${i + 1}次: ${String.format(Locale.US, "%.1f", e.value)}mmol/L ${e.tag} ${e.time}".trim() }.joinToString("\n")
                                                } else ""
                                                zoomDetail = CellZoomDetail(
                                                    title = "🌅 早餐 · 餐后血糖",
                                                    date = record.date,
                                                    mainText = "${String.format(Locale.US, "%.1f", bgVal)} mmol/L",
                                                    subText = levelDesc + extraDetail,
                                                    highlightColor = fgColor
                                                )
                                            }
                                        }
                                    )
                                    if (isEnlarged) {
                                        TableDietCell(
                                            diet = record.bfDiet,
                                            width = bfDietWidth,
                                            fontSize = (dataFontSize.value - 2.5f).sp,
                                            onLongClick = if (record.bfDiet.isNotBlank()) {
                                                {
                                                    zoomDetail = CellZoomDetail(
                                                        title = "🌅 早餐 · 饮食明细",
                                                        date = record.date,
                                                        mainText = record.bfDiet,
                                                        subText = "早餐摄入餐食明细",
                                                        highlightColor = TealPrimary
                                                    )
                                                }
                                            } else null
                                        )
                                    }

                                    // --- ☀️ 午餐段数据 ---
                                    if (isEnlarged) {
                                        TableBGCellContainer(
                                            bg = record.preLunchBG,
                                            width = preLunchWidth,
                                            isFasting = false,
                                            fontSize = dataFontSize,
                                            onLongClick = record.preLunchBG?.let { bgVal ->
                                                {
                                                    val status = BGUtils.evaluatePostMeal(bgVal)
                                                    val levelDesc = when (status?.level) {
                                                        BGLevel.LOW -> "⚠️ 偏低 (餐前参考: 3.9 ~ 6.1 mmol/L)"
                                                        BGLevel.HIGH -> "⚠️ 偏高 (餐前参考: 3.9 ~ 6.1 mmol/L)"
                                                        BGLevel.NORMAL -> "✅ 正常理想范围"
                                                        null -> ""
                                                    }
                                                    val fgColor = when (status?.level) {
                                                        BGLevel.LOW -> cGlucoseLow
                                                        BGLevel.NORMAL -> cGlucoseNormal
                                                        BGLevel.HIGH -> cGlucoseHigh
                                                        null -> cLunchColor
                                                    }
                                                    zoomDetail = CellZoomDetail(
                                                        title = "☀️ 午餐 · 餐前血糖",
                                                        date = record.date,
                                                        mainText = "${String.format(Locale.US, "%.1f", bgVal)} mmol/L",
                                                        subText = levelDesc,
                                                        highlightColor = fgColor
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    TableMedicationCell(
                                        dose = record.lunchInsulin,
                                        medName = record.lunchMedName,
                                        width = lunchMedWidth,
                                        medTiming = record.lunchMedTiming,
                                        fontSize = dataFontSize,
                                        onLongClick = if (record.lunchInsulin != null && record.lunchInsulin > 0) {
                                            {
                                                val unit = MedicationData.detectUnit(record.lunchMedName)
                                                val doseStr = if (record.lunchInsulin % 1f == 0f) "${record.lunchInsulin.toInt()}$unit" else "${String.format(Locale.US, "%.1f", record.lunchInsulin)}$unit"
                                                val medDesc = buildString {
                                                    if (record.lunchMedName.isNotBlank()) append(record.lunchMedName) else append("午餐用药")
                                                    if (record.lunchMedTiming.isNotBlank()) append(" · ${record.lunchMedTiming}")
                                                }
                                                zoomDetail = CellZoomDetail(
                                                    title = "☀️ 午餐 · 用药",
                                                    date = record.date,
                                                    mainText = doseStr,
                                                    subText = medDesc,
                                                    highlightColor = cLunchColor
                                                )
                                            }
                                        } else null
                                    )
                                    val lunchPosts = record.getPostMealList(MealPeriod.LUNCH)
                                    TableBGCellContainer(
                                        bg = record.postLunchBG,
                                        width = postLunchWidth,
                                        isFasting = false,
                                        fontSize = dataFontSize,
                                        extraCount = (lunchPosts.size - 1).coerceAtLeast(0),
                                        onLongClick = record.postLunchBG?.let { bgVal ->
                                            {
                                                val status = BGUtils.evaluatePostMeal(bgVal)
                                                val levelDesc = when (status?.level) {
                                                    BGLevel.LOW -> "⚠️ 偏低 (餐后2h参考标准: 4.4 ~ 7.8 mmol/L)"
                                                    BGLevel.HIGH -> "⚠️ 偏高 (餐后2h参考标准: 4.4 ~ 7.8 mmol/L)"
                                                    BGLevel.NORMAL -> "✅ 正常理想范围 (4.4 ~ 7.8 mmol/L)"
                                                    null -> ""
                                                }
                                                val fgColor = when (status?.level) {
                                                    BGLevel.LOW -> cGlucoseLow
                                                    BGLevel.NORMAL -> cGlucoseNormal
                                                    BGLevel.HIGH -> cGlucoseHigh
                                                    null -> cLunchColor
                                                }
                                                val extraDetail = if (lunchPosts.size > 1) {
                                                    "\n" + lunchPosts.mapIndexed { i, e -> "第${i + 1}次: ${String.format(Locale.US, "%.1f", e.value)}mmol/L ${e.tag} ${e.time}".trim() }.joinToString("\n")
                                                } else ""
                                                zoomDetail = CellZoomDetail(
                                                    title = "☀️ 午餐 · 餐后血糖",
                                                    date = record.date,
                                                    mainText = "${String.format(Locale.US, "%.1f", bgVal)} mmol/L",
                                                    subText = levelDesc + extraDetail,
                                                    highlightColor = fgColor
                                                )
                                            }
                                        }
                                    )
                                    if (isEnlarged) {
                                        TableDietCell(
                                            diet = record.lunchDiet,
                                            width = lunchDietWidth,
                                            fontSize = (dataFontSize.value - 2.5f).sp,
                                            onLongClick = if (record.lunchDiet.isNotBlank()) {
                                                {
                                                    zoomDetail = CellZoomDetail(
                                                        title = "☀️ 午餐 · 饮食明细",
                                                        date = record.date,
                                                        mainText = record.lunchDiet,
                                                        subText = "午餐摄入餐食明细",
                                                        highlightColor = cLunchColor
                                                    )
                                                }
                                            } else null
                                        )
                                    }

                                    // --- 🌙 晚餐段数据 ---
                                    if (isEnlarged) {
                                        TableBGCellContainer(
                                            bg = record.preDinnerBG,
                                            width = preDinnerWidth,
                                            isFasting = false,
                                            fontSize = dataFontSize,
                                            onLongClick = record.preDinnerBG?.let { bgVal ->
                                                {
                                                    val status = BGUtils.evaluatePostMeal(bgVal)
                                                    val levelDesc = when (status?.level) {
                                                        BGLevel.LOW -> "⚠️ 偏低 (餐前参考: 3.9 ~ 6.1 mmol/L)"
                                                        BGLevel.HIGH -> "⚠️ 偏高 (餐前参考: 3.9 ~ 6.1 mmol/L)"
                                                        BGLevel.NORMAL -> "✅ 正常理想范围"
                                                        null -> ""
                                                    }
                                                    val fgColor = when (status?.level) {
                                                        BGLevel.LOW -> cGlucoseLow
                                                        BGLevel.NORMAL -> cGlucoseNormal
                                                        BGLevel.HIGH -> cGlucoseHigh
                                                        null -> cDinnerColor
                                                    }
                                                    zoomDetail = CellZoomDetail(
                                                        title = "🌙 晚餐 · 餐前血糖",
                                                        date = record.date,
                                                        mainText = "${String.format(Locale.US, "%.1f", bgVal)} mmol/L",
                                                        subText = levelDesc,
                                                        highlightColor = fgColor
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    TableMedicationCell(
                                        dose = record.dinnerInsulin,
                                        medName = record.dinnerMedName,
                                        width = dinnerMedWidth,
                                        medTiming = record.dinnerMedTiming,
                                        fontSize = dataFontSize,
                                        onLongClick = if (record.dinnerInsulin != null && record.dinnerInsulin > 0) {
                                            {
                                                val unit = MedicationData.detectUnit(record.dinnerMedName)
                                                val doseStr = if (record.dinnerInsulin % 1f == 0f) "${record.dinnerInsulin.toInt()}$unit" else "${String.format(Locale.US, "%.1f", record.dinnerInsulin)}$unit"
                                                val medDesc = buildString {
                                                    if (record.dinnerMedName.isNotBlank()) append(record.dinnerMedName) else append("晚餐用药")
                                                    if (record.dinnerMedTiming.isNotBlank()) append(" · ${record.dinnerMedTiming}")
                                                }
                                                zoomDetail = CellZoomDetail(
                                                    title = "🌙 晚餐 · 用药",
                                                    date = record.date,
                                                    mainText = doseStr,
                                                    subText = medDesc,
                                                    highlightColor = cDinnerColor
                                                )
                                            }
                                        } else null
                                    )
                                    val dinnerPosts = record.getPostMealList(MealPeriod.DINNER)
                                    TableBGCellContainer(
                                        bg = record.postDinnerBG,
                                        width = postDinnerWidth,
                                        isFasting = false,
                                        fontSize = dataFontSize,
                                        extraCount = (dinnerPosts.size - 1).coerceAtLeast(0),
                                        onLongClick = record.postDinnerBG?.let { bgVal ->
                                            {
                                                val status = BGUtils.evaluatePostMeal(bgVal)
                                                val levelDesc = when (status?.level) {
                                                    BGLevel.LOW -> "⚠️ 偏低 (餐后2h参考标准: 4.4 ~ 7.8 mmol/L)"
                                                    BGLevel.HIGH -> "⚠️ 偏高 (餐后2h参考标准: 4.4 ~ 7.8 mmol/L)"
                                                    BGLevel.NORMAL -> "✅ 正常理想范围 (4.4 ~ 7.8 mmol/L)"
                                                    null -> ""
                                                }
                                                val fgColor = when (status?.level) {
                                                    BGLevel.LOW -> cGlucoseLow
                                                    BGLevel.NORMAL -> cGlucoseNormal
                                                    BGLevel.HIGH -> cGlucoseHigh
                                                    null -> cDinnerColor
                                                }
                                                val extraDetail = if (dinnerPosts.size > 1) {
                                                    "\n" + dinnerPosts.mapIndexed { i, e -> "第${i + 1}次: ${String.format(Locale.US, "%.1f", e.value)}mmol/L ${e.tag} ${e.time}".trim() }.joinToString("\n")
                                                } else ""
                                                zoomDetail = CellZoomDetail(
                                                    title = "🌙 晚餐 · 餐后血糖",
                                                    date = record.date,
                                                    mainText = "${String.format(Locale.US, "%.1f", bgVal)} mmol/L",
                                                    subText = levelDesc + extraDetail,
                                                    highlightColor = fgColor
                                                )
                                            }
                                        }
                                    )
                                    if (isEnlarged) {
                                        TableDietCell(
                                            diet = record.dinnerDiet,
                                            width = dinnerDietWidth,
                                            fontSize = (dataFontSize.value - 2.5f).sp,
                                            onLongClick = if (record.dinnerDiet.isNotBlank()) {
                                                {
                                                    zoomDetail = CellZoomDetail(
                                                        title = "🌙 晚餐 · 饮食明细",
                                                        date = record.date,
                                                        mainText = record.dinnerDiet,
                                                        subText = "晚餐摄入餐食明细",
                                                        highlightColor = cDinnerColor
                                                    )
                                                }
                                            } else null
                                        )
                                    }

                                    // --- 🛌 睡前段数据 ---
                                    if (isEnlarged) {
                                        TableBGCellContainer(
                                            bg = record.preNightBG,
                                            width = preNightWidth,
                                            isFasting = false,
                                            fontSize = dataFontSize,
                                            onLongClick = record.preNightBG?.let { bgVal ->
                                                {
                                                    val status = BGUtils.evaluatePostMeal(bgVal)
                                                    val levelDesc = when (status?.level) {
                                                        BGLevel.LOW -> "⚠️ 偏低 (睡前参考: 5.0 ~ 7.8 mmol/L，防夜间低血糖)"
                                                        BGLevel.HIGH -> "⚠️ 偏高"
                                                        BGLevel.NORMAL -> "✅ 理想安全范围"
                                                        null -> ""
                                                    }
                                                    val fgColor = when (status?.level) {
                                                        BGLevel.LOW -> cGlucoseLow
                                                        BGLevel.NORMAL -> cGlucoseNormal
                                                        BGLevel.HIGH -> cGlucoseHigh
                                                        null -> cBedtimeColor
                                                    }
                                                    zoomDetail = CellZoomDetail(
                                                        title = "🛌 睡前 · 血糖",
                                                        date = record.date,
                                                        mainText = "${String.format(Locale.US, "%.1f", bgVal)} mmol/L",
                                                        subText = levelDesc,
                                                        highlightColor = fgColor
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    TableMedicationCell(
                                        dose = record.bedtimeInsulin,
                                        medName = record.nightMedName,
                                        width = bedtimeMedWidth,
                                        isBedtime = true,
                                        medTiming = record.nightMedTiming,
                                        fontSize = dataFontSize,
                                        onLongClick = if (record.bedtimeInsulin != null && record.bedtimeInsulin > 0) {
                                            {
                                                val unit = MedicationData.detectUnit(record.nightMedName)
                                                val doseStr = if (record.bedtimeInsulin % 1f == 0f) "${record.bedtimeInsulin.toInt()}$unit" else "${String.format(Locale.US, "%.1f", record.bedtimeInsulin)}$unit"
                                                val medDesc = buildString {
                                                    if (record.nightMedName.isNotBlank()) append(record.nightMedName) else append("睡前用药")
                                                    if (record.nightMedTiming.isNotBlank() && record.nightMedTiming != "餐前" && record.nightMedTiming != "睡前") {
                                                        append(" · ${record.nightMedTiming}")
                                                    }
                                                }
                                                zoomDetail = CellZoomDetail(
                                                    title = "🛌 睡前 · 用药",
                                                    date = record.date,
                                                    mainText = doseStr,
                                                    subText = medDesc,
                                                    highlightColor = cBedtimeColor
                                                )
                                            }
                                        } else null
                                    )
                                    if (isEnlarged) {
                                        TableDietCell(
                                            diet = record.nightDiet,
                                            width = nightDietWidth,
                                            fontSize = (dataFontSize.value - 2.5f).sp,
                                            onLongClick = if (record.nightDiet.isNotBlank()) {
                                                {
                                                    zoomDetail = CellZoomDetail(
                                                        title = "🛌 睡前 · 加餐",
                                                        date = record.date,
                                                        mainText = record.nightDiet,
                                                        subText = "睡前加餐摄入明细",
                                                        highlightColor = cBedtimeColor
                                                    )
                                                }
                                            } else null
                                        )
                                    }

                                    // 全天胰岛素总剂量
                                    Box(
                                        modifier = Modifier
                                            .width(totalInsulinWidth)
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = if (record.totalInsulin > 0) {
                                                    {
                                                        val doseStr = if (record.totalInsulin % 1f == 0f) "${record.totalInsulin.toInt()}U" else "${String.format(Locale.US, "%.1f", record.totalInsulin)}U"
                                                        zoomDetail = CellZoomDetail(
                                                            title = "📊 全天胰岛素总量",
                                                            date = record.date,
                                                            mainText = doseStr,
                                                            subText = "当天所有餐次及睡前注射胰岛素累计总量",
                                                            highlightColor = TealPrimary
                                                        )
                                                    }
                                                } else null
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (record.totalInsulin > 0) {
                                            Text(
                                                text = if (record.totalInsulin % 1f == 0f) {
                                                    "${record.totalInsulin.toInt()}U"
                                                } else {
                                                    "${String.format(Locale.US, "%.1f", record.totalInsulin)}U"
                                                },
                                                fontSize = dataFontSize,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = TealPrimary
                                            )
                                        } else {
                                            Text("-", fontSize = dataFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                        }
                                    }

                                    // 备注
                                    Box(
                                        modifier = Modifier
                                            .width(notesWidth)
                                            .padding(horizontal = 6.dp)
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = if (record.notes.isNotBlank()) {
                                                    {
                                                        zoomDetail = CellZoomDetail(
                                                            title = "📝 备忘记录",
                                                            date = record.date,
                                                            mainText = record.notes,
                                                            subText = "当天特殊备忘与注意事项",
                                                            highlightColor = TealPrimary
                                                        )
                                                    }
                                                } else null
                                            ),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = record.notes.ifBlank { "-" },
                                            fontSize = if (isEnlarged) 13.sp else 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = if (isEnlarged) 2 else 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // 操作按钮
                                    Row(
                                        modifier = Modifier.width(actionsWidth),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onEdit(record) },
                                            modifier = Modifier.size(if (isEnlarged) 34.dp else 30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "编辑",
                                                tint = TealPrimary,
                                                modifier = Modifier.size(if (isEnlarged) 18.dp else 16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDelete(record) },
                                            modifier = Modifier.size(if (isEnlarged) 34.dp else 30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "删除",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(if (isEnlarged) 18.dp else 16.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                            }
                        }
                    }
                }
            }
        }
    }

    // 长按单元格放大显示弹窗
    zoomDetail?.let { detail ->
        CellZoomDialog(
            detail = detail,
            onDismiss = { zoomDetail = null }
        )
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    width: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color = MaterialTheme.colorScheme.onSurface,
    bgColor: Color = Color.Transparent
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TableSubHeaderCell(
    text: String,
    width: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    isLabel: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = if (isLabel) FontWeight.Bold else FontWeight.Medium,
            color = if (isLabel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TablePrevNightCell(
    prevNight: PrevNightInfo?,
    width: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .width(width)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (prevNight != null && prevNight.dose > 0) {
            val unit = MedicationData.detectUnit(prevNight.medName)
            val doseStr = if (prevNight.dose % 1f == 0f) "${prevNight.dose.toInt()}$unit" else "${prevNight.dose}$unit"
            val shortName = MedicationData.getShortName(prevNight.medName)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = doseStr,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AppThemeColors.bedtimeColor,
                    textAlign = TextAlign.Center
                )
                if (shortName.isNotBlank()) {
                    Text(
                        text = shortName,
                        fontSize = (fontSize.value - 3.5f).coerceAtLeast(9.5f).sp,
                        fontWeight = FontWeight.Medium,
                        color = AppThemeColors.bedtimeColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text("-", fontSize = fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableMedicationCell(
    dose: Float?,
    medName: String,
    width: Dp,
    isBedtime: Boolean = false,
    medTiming: String = "",
    fontSize: androidx.compose.ui.unit.TextUnit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .width(width)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (dose != null && dose > 0) {
            val unit = MedicationData.detectUnit(medName)
            val doseStr = if (dose % 1f == 0f) "${dose.toInt()}$unit" else "${String.format(Locale.US, "%.1f", dose)}$unit"
            val fg = if (isBedtime) AppThemeColors.bedtimeColor else MaterialTheme.colorScheme.onSurface
            val shortName = MedicationData.getShortName(medName)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = doseStr,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = fg,
                    textAlign = TextAlign.Center
                )
                val subText = if (!isBedtime && medTiming.isNotBlank() && medTiming != "餐前") {
                    if (shortName.isNotBlank()) "$shortName·$medTiming" else medTiming
                } else {
                    shortName
                }
                if (subText.isNotBlank()) {
                    Text(
                        text = subText,
                        fontSize = (fontSize.value - 3.5f).coerceAtLeast(9.5f).sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isBedtime) AppThemeColors.bedtimeColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text("-", fontSize = fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableDietCell(
    diet: String,
    width: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (diet.isNotBlank()) {
            Text(
                text = diet,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        } else {
            Text("-", fontSize = fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableBGCellContainer(
    bg: Float?,
    width: Dp,
    isFasting: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    extraCount: Int = 0,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .width(width)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            TableBGCell(bg, isFasting, fontSize)
            if (extraCount > 0) {
                Text(
                    text = "+$extraCount",
                    fontSize = (fontSize.value - 4f).coerceAtLeast(8f).sp,
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TableBGCell(
    bg: Float?,
    isFasting: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val status = if (isFasting) BGUtils.evaluateFasting(bg) else BGUtils.evaluatePostMeal(bg)

    if (status != null) {
        val fgColor = when (status.level) {
            BGLevel.LOW -> AppThemeColors.glucoseLow
            BGLevel.NORMAL -> AppThemeColors.glucoseNormal
            BGLevel.HIGH -> AppThemeColors.glucoseHigh
        }

        Text(
            text = status.valueText,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = fgColor,
            textAlign = TextAlign.Center
        )
    } else {
        Text("-", fontSize = fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), textAlign = TextAlign.Center)
    }
}
