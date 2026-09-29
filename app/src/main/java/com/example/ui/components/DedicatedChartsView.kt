package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.ui.TableDateRange
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

/**
 * 专属图谱视图分类
 */
enum class DedicatedChartType(val label: String, val desc: String) {
    DAILY_24H("单日24h走势", "左右滑动切换不同天，结合用药与进餐因果闭环"),
    AGP_OVERLAY("7天AGP叠图", "国际内分泌金标准，多日时段叠加发现规律性波动"),
    MULTI_DAY_DUAL("空腹/餐后双轨", "评估长期方案调药效果与晨起/餐后基线差距")
}

/**
 * 统计页专属折线图大屏（支持左右横滑切天、支持三种临床级图表切换）
 */
@Composable
fun DedicatedChartsView(
    allRecords: List<InsulinRecord>,
    currentRangeRecords: List<InsulinRecord>,
    selectedRange: TableDateRange,
    onRangeSelected: (TableDateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChartType by rememberSaveable { mutableStateOf(DedicatedChartType.DAILY_24H) }

    // 所有有效日期（降序排列，今天居首）
    val todayStr = remember { LocalDate.now().toString() }
    val availableDates = remember(allRecords, todayStr) {
        val set = allRecords.map { it.date }.toMutableSet()
        set.add(todayStr)
        set.toList().sortedDescending()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 图表二级模式切换胶囊
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DedicatedChartType.entries.forEach { type ->
                val isSel = type == selectedChartType
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSel) TealPrimary else Color.Transparent)
                        .clickable { selectedChartType = type }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. 模式说明轻量提示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TealPrimary.copy(alpha = 0.8f),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = selectedChartType.desc,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1
            )
        }

        // 3. 内容展示区（按选中的图表模式渲染）
        Crossfade(
            targetState = selectedChartType,
            label = "dedicated_chart_crossfade"
        ) { chartType ->
            when (chartType) {
                DedicatedChartType.DAILY_24H -> {
                    // 单日24h走势大图：支持与卡片页完全一样的左右横滑切天
                    Daily24hGlucosePagerView(
                        availableDates = availableDates,
                        allRecords = allRecords
                    )
                }
                DedicatedChartType.AGP_OVERLAY -> {
                    // 7天/14天 AGP 模态叠图
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgpModalDayCard(records = currentRangeRecords)
                    }
                }
                DedicatedChartType.MULTI_DAY_DUAL -> {
                    // 空腹 vs 餐后双轨走势图
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MultiDayDualTrendCard(records = currentRangeRecords)
                    }
                }
            }
        }
    }
}

/**
 * 单日 24h 走势大图（左右滑动切换不同天的记录，带日期导航与因果气泡标注）
 */
@Composable
private fun Daily24hGlucosePagerView(
    availableDates: List<String>,
    allRecords: List<InsulinRecord>
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { availableDates.size }
    val currentDate = availableDates.getOrNull(pagerState.currentPage) ?: availableDates.firstOrNull() ?: ""

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 日期导航切换栏（支持点击前后翻日，显示当前星期与今天标记）
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (pagerState.currentPage < availableDates.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage < availableDates.size - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "前一天",
                        tint = if (pagerState.currentPage < availableDates.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val dateObj = try { LocalDate.parse(currentDate) } catch (_: Exception) { null }
                    val isToday = currentDate == LocalDate.now().toString()
                    val weekDayStr = dateObj?.let {
                        when (it.dayOfWeek.value) {
                            1 -> "周一"
                            2 -> "周二"
                            3 -> "周三"
                            4 -> "周四"
                            5 -> "周五"
                            6 -> "周六"
                            7 -> "周日"
                            else -> ""
                        }
                    } ?: ""

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = currentDate,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (weekDayStr.isNotBlank()) {
                            Text(
                                text = weekDayStr,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isToday) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = TealPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "今天",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "左右滑动屏幕可翻看任意一天的24h曲线",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }

                IconButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "后一天",
                        tint = if (pagerState.currentPage > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // HorizontalPager：每一页渲染对应日期的单日大图
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { pageIdx ->
            val date = availableDates.getOrNull(pageIdx) ?: ""
            val record = allRecords.find { it.date == date }
            SingleDayFullCurveCard(record = record, date = date)
        }
    }
}

/**
 * 单日 24h 血糖全景曲线卡片（含用药/进餐事件气泡插桩）
 */
@Composable
private fun SingleDayFullCurveCard(
    record: InsulinRecord?,
    date: String
) {
    val isDark = AppThemeColors.isDark
    val surfaceColor = MaterialTheme.colorScheme.surface

    // 提取当日各测量点
    data class TimePoint(
        val periodName: String,
        val bg: Float?,
        val insulinDose: Float?,
        val medName: String,
        val hasDiet: Boolean
    )

    val pointsData = remember(record) {
        if (record == null) emptyList()
        else listOf(
            TimePoint("空腹", record.fastingBG ?: record.preBfBG, record.bfInsulin, record.bfMedName, record.bfDiet.isNotBlank()),
            TimePoint("早后", record.postBfBG ?: record.getPostMealList(MealPeriod.MORNING).firstOrNull()?.value, null, "", false),
            TimePoint("午前", record.preLunchBG, record.lunchInsulin, record.lunchMedName, record.lunchDiet.isNotBlank()),
            TimePoint("午后", record.postLunchBG ?: record.getPostMealList(MealPeriod.LUNCH).firstOrNull()?.value, null, "", false),
            TimePoint("晚前", record.preDinnerBG, record.dinnerInsulin, record.dinnerMedName, record.dinnerDiet.isNotBlank()),
            TimePoint("晚后", record.postDinnerBG ?: record.getPostMealList(MealPeriod.DINNER).firstOrNull()?.value, null, "", false),
            TimePoint("睡前", record.preNightBG, record.bedtimeInsulin, record.nightMedName, record.nightDiet.isNotBlank())
        )
    }

    val validBgs = pointsData.mapNotNull { it.bg }
    val totalInsulin = record?.totalInsulin ?: 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 图表卡片顶栏：图例说明
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "全天血糖与用药对照",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFF22C55E), CircleShape))
                        Text("达标区(3.9-10)", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(7.dp).background(TealPrimary, CircleShape))
                        Text("血糖曲线", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (validBgs.isEmpty() && totalInsulin == 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$date 当天暂无血糖或用药数据",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 24小时全景 Canvas 画布
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingLeft = 28.dp.toPx()
                    val paddingRight = 14.dp.toPx()
                    val paddingTop = 26.dp.toPx() // 为上方用药气泡预留空间
                    val paddingBottom = 22.dp.toPx()

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    val minGlucose = 3.0f
                    val maxObserved = validBgs.maxOrNull() ?: 10f
                    val maxGlucose = max(13.0f, kotlin.math.ceil(maxObserved + 2.0f))

                    // 1. 绘制绿色 TIR 安全达标带 (3.9 ~ 10.0 mmol/L)
                    val tirTop = paddingTop + chartHeight * (1f - (10.0f - minGlucose) / (maxGlucose - minGlucose))
                    val tirBottom = paddingTop + chartHeight * (1f - (3.9f - minGlucose) / (maxGlucose - minGlucose))
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF22C55E).copy(alpha = if (isDark) 0.16f else 0.10f),
                                Color(0xFF22C55E).copy(alpha = if (isDark) 0.08f else 0.04f)
                            )
                        ),
                        topLeft = Offset(paddingLeft, tirTop),
                        size = Size(chartWidth, (tirBottom - tirTop).coerceAtLeast(0f))
                    )

                    // 达标带虚线边界
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    drawLine(
                        color = Color(0xFF22C55E).copy(alpha = 0.45f),
                        start = Offset(paddingLeft, tirTop),
                        end = Offset(width - paddingRight, tirTop),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = Color(0xFF22C55E).copy(alpha = 0.45f),
                        start = Offset(paddingLeft, tirBottom),
                        end = Offset(width - paddingRight, tirBottom),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // 2. 绘制水平参考刻度网格
                    val gridSteps = 3
                    val gridLineColor = if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0)
                    val textPaintColor = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.GRAY

                    for (i in 0..gridSteps) {
                        val bgValue = minGlucose + (maxGlucose - minGlucose) * (i / gridSteps.toFloat())
                        val y = paddingTop + chartHeight * (1f - (bgValue - minGlucose) / (maxGlucose - minGlucose))
                        drawLine(
                            color = gridLineColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 0.8.dp.toPx()
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "%.0f", bgValue),
                            paddingLeft - 6.dp.toPx(),
                            y + 3.5.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = textPaintColor
                                textSize = 9.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                                isAntiAlias = true
                            }
                        )
                    }

                    // 3. 计算 7 个时段的 X 轴坐标
                    val slotCount = pointsData.size
                    val slotWidth = chartWidth / (slotCount - 1).toFloat()
                    fun getSlotX(idx: Int): Float = paddingLeft + idx * slotWidth

                    // 绘制 X 轴时段文本
                    for (i in pointsData.indices) {
                        val cx = getSlotX(i)
                        drawContext.canvas.nativeCanvas.drawText(
                            pointsData[i].periodName,
                            cx,
                            height - 3.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = textPaintColor
                                textSize = 9.5.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }

                    // 4. 收集血糖折线点并绘制曲线与渐变区域
                    val curvePoints = mutableListOf<Pair<Offset, Float>>()
                    for (i in pointsData.indices) {
                        val bg = pointsData[i].bg
                        if (bg != null) {
                            val cx = getSlotX(i)
                            val fraction = ((bg - minGlucose) / (maxGlucose - minGlucose)).coerceIn(0f, 1f)
                            val cy = paddingTop + chartHeight * (1f - fraction)
                            curvePoints.add(Offset(cx, cy) to bg)
                        }
                    }

                    if (curvePoints.size > 1) {
                        // 曲线平滑连接
                        val path = Path().apply {
                            smoothCurveTo(curvePoints.map { it.first })
                        }
                        // 曲线下方柔和渐变填充
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(curvePoints.last().first.x, height - paddingBottom)
                            lineTo(curvePoints.first().first.x, height - paddingBottom)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    TealPrimary.copy(alpha = if (isDark) 0.28f else 0.18f),
                                    TealPrimary.copy(alpha = 0f)
                                ),
                                startY = paddingTop,
                                endY = height - paddingBottom
                            )
                        )
                        // 绘制主轮廓线
                        drawPath(
                            path = path,
                            color = TealPrimary,
                            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // 5. 绘制测点圆环与实测数值标签
                    for ((p, v) in curvePoints) {
                        val dotColor = when {
                            v < 3.9f -> Color(0xFFEF4444)
                            v > 10.0f -> Color(0xFFF97316)
                            else -> TealPrimary
                        }
                        drawCircle(color = surfaceColor, radius = 4.2.dp.toPx(), center = p)
                        drawCircle(color = dotColor, radius = 2.8.dp.toPx(), center = p)

                        // 绘制数值文本
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.US, "%.1f", v),
                            p.x,
                            p.y - 6.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = dotColor.toArgb()
                                textSize = 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                                isAntiAlias = true
                            }
                        )
                    }

                    // 6. 关键因果插桩：绘制用药/胰岛素气泡 (Event Pins)
                    for (i in pointsData.indices) {
                        val item = pointsData[i]
                        val cx = getSlotX(i)
                        val dose = item.insulinDose

                        if (dose != null && dose > 0f) {
                            val doseText = "💉${if (dose % 1f == 0f) dose.toInt() else dose}U"
                            val bubbleY = paddingTop - 12.dp.toPx()

                            // 用药气泡胶囊底色
                            val paint = android.graphics.Paint().apply {
                                textSize = 9.sp.toPx()
                                isFakeBoldText = true
                                isAntiAlias = true
                            }
                            val textWidth = paint.measureText(doseText)
                            val bubbleWidth = textWidth + 8.dp.toPx()
                            val bubbleHeight = 15.dp.toPx()

                            drawRoundRect(
                                color = if (isDark) Color(0xFF0F766E) else Color(0xFFCCFBF1),
                                topLeft = Offset(cx - bubbleWidth / 2f, bubbleY - bubbleHeight / 2f),
                                size = Size(bubbleWidth, bubbleHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                doseText,
                                cx,
                                bubbleY + 3.2.dp.toPx(),
                                paint.apply {
                                    color = (if (isDark) Color.White else Color(0xFF0F766E)).toArgb()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }
                    }
                }
            }

            // 当日数据关键指标看板（最高、最低、均值、胰岛素总量、TIR）
            if (validBgs.isNotEmpty() || totalInsulin > 0f) {
                val maxBg = validBgs.maxOrNull()
                val minBg = validBgs.minOrNull()
                val avgBg = if (validBgs.isNotEmpty()) validBgs.average().toFloat() else null
                val inRangeCount = validBgs.count { it in 3.9f..10.0f }
                val tirPercent = if (validBgs.isNotEmpty()) (inRangeCount * 100 / validBgs.size) else null

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatMetricColumn("最高", maxBg?.let { String.format(Locale.US, "%.1f", it) } ?: "--", if (maxBg != null && maxBg > 10.0f) Color(0xFFF97316) else MaterialTheme.colorScheme.onSurface)
                    StatMetricColumn("最低", minBg?.let { String.format(Locale.US, "%.1f", it) } ?: "--", if (minBg != null && minBg < 3.9f) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface)
                    StatMetricColumn("均值", avgBg?.let { String.format(Locale.US, "%.1f", it) } ?: "--", MaterialTheme.colorScheme.onSurface)
                    StatMetricColumn("TIR达标", tirPercent?.let { "$it%" } ?: "--", if (tirPercent != null && tirPercent >= 70) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurface)
                    StatMetricColumn("用药总量", if (totalInsulin > 0) "${if (totalInsulin % 1f == 0f) totalInsulin.toInt() else totalInsulin}U" else "--", TealPrimary)
                }
            }
        }
    }
}

@Composable
private fun StatMetricColumn(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

/**
 * 7天/14天 AGP 模态日叠图卡片
 */
@Composable
private fun AgpModalDayCard(records: List<InsulinRecord>) {
    val isDark = AppThemeColors.isDark
    val surfaceColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AGP 24小时时段叠加分析",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "共统计 ${records.size} 天数据",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "将周期内所有天数的测量点折叠在同一24h时间轴上，绿色带为目标区间(3.9-10.0)，青线为中位数均值。",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            // 绘制离散叠加 Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = size.width
                val height = size.height
                val paddingLeft = 28.dp.toPx()
                val paddingRight = 14.dp.toPx()
                val paddingTop = 16.dp.toPx()
                val paddingBottom = 22.dp.toPx()

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                val minGlucose = 3.0f
                val maxGlucose = 15.0f

                // 目标达标带
                val tirTop = paddingTop + chartHeight * (1f - (10.0f - minGlucose) / (maxGlucose - minGlucose))
                val tirBottom = paddingTop + chartHeight * (1f - (3.9f - minGlucose) / (maxGlucose - minGlucose))
                drawRect(
                    color = Color(0xFF22C55E).copy(alpha = if (isDark) 0.14f else 0.08f),
                    topLeft = Offset(paddingLeft, tirTop),
                    size = Size(chartWidth, (tirBottom - tirTop).coerceAtLeast(0f))
                )

                val periods = listOf("空腹", "早后", "午前", "午后", "晚前", "晚后", "睡前")
                val slotWidth = chartWidth / (periods.size - 1).toFloat()
                fun getSlotX(idx: Int): Float = paddingLeft + idx * slotWidth

                // 绘制 X 轴标签
                for (i in periods.indices) {
                    val cx = getSlotX(i)
                    drawContext.canvas.nativeCanvas.drawText(
                        periods[i],
                        cx,
                        height - 3.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.GRAY
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                    )
                }

                // 统计各时段的所有测值点并画出散点
                val periodPointsMap = mutableMapOf<Int, MutableList<Float>>()
                for (i in periods.indices) {
                    periodPointsMap[i] = mutableListOf()
                }

                records.forEach { r ->
                    r.fastingBG?.let { periodPointsMap[0]?.add(it) }
                    (r.postBfBG ?: r.getPostMealList(MealPeriod.MORNING).firstOrNull()?.value)?.let { periodPointsMap[1]?.add(it) }
                    r.preLunchBG?.let { periodPointsMap[2]?.add(it) }
                    (r.postLunchBG ?: r.getPostMealList(MealPeriod.LUNCH).firstOrNull()?.value)?.let { periodPointsMap[3]?.add(it) }
                    r.preDinnerBG?.let { periodPointsMap[4]?.add(it) }
                    (r.postDinnerBG ?: r.getPostMealList(MealPeriod.DINNER).firstOrNull()?.value)?.let { periodPointsMap[5]?.add(it) }
                    r.preNightBG?.let { periodPointsMap[6]?.add(it) }
                }

                // 画出各时段的历史半透明测点
                for ((idx, list) in periodPointsMap) {
                    val cx = getSlotX(idx)
                    list.forEach { v ->
                        val fraction = ((v - minGlucose) / (maxGlucose - minGlucose)).coerceIn(0f, 1f)
                        val cy = paddingTop + chartHeight * (1f - fraction)
                        drawCircle(
                            color = TealPrimary.copy(alpha = 0.35f),
                            radius = 2.5.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }

                // 计算各时段的中位均值并绘制连线
                val medianPoints = mutableListOf<Pair<Offset, Float>>()
                for (idx in 0 until periods.size) {
                    val list = periodPointsMap[idx] ?: emptyList()
                    if (list.isNotEmpty()) {
                        val sorted = list.sorted()
                        val median = sorted[sorted.size / 2]
                        val cx = getSlotX(idx)
                        val fraction = ((median - minGlucose) / (maxGlucose - minGlucose)).coerceIn(0f, 1f)
                        val cy = paddingTop + chartHeight * (1f - fraction)
                        medianPoints.add(Offset(cx, cy) to median)
                    }
                }

                if (medianPoints.size > 1) {
                    val medianPath = Path().apply {
                        smoothCurveTo(medianPoints.map { it.first })
                    }
                    drawPath(
                        path = medianPath,
                        color = TealPrimary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                for ((pt, v) in medianPoints) {
                    drawCircle(color = surfaceColor, radius = 4.5.dp.toPx(), center = pt)
                    drawCircle(color = TealPrimary, radius = 3.dp.toPx(), center = pt)
                }
            }
        }
    }
}

/**
 * 多日空腹 vs 餐后双轨走势大卡片
 */
@Composable
private fun MultiDayDualTrendCard(records: List<InsulinRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "多日空腹与餐后峰值追踪",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFF0284C7), CircleShape))
                        Text("空腹基线", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFFEA580C), CircleShape))
                        Text("餐后峰值", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text("当前周期暂无足够数据", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                FastingVsPostprandialCanvasChart(records = records.takeLast(14))
            }
        }
    }
}

/**
 * 贝塞尔平滑曲线插值连接（适度圆滑，消除过度臃肿波浪，紧贴数据走势）
 */
private fun Path.smoothCurveTo(points: List<Offset>, tension: Float = 0.09f) {
    if (points.isEmpty()) return
    moveTo(points.first().x, points.first().y)
    if (points.size == 1) return
    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else p2
        val control1 = Offset(
            p1.x + (p2.x - p0.x) * tension,
            p1.y + (p2.y - p0.y) * tension
        )
        val control2 = Offset(
            p2.x - (p3.x - p1.x) * tension,
            p2.y - (p3.y - p1.y) * tension
        )
        cubicTo(control1.x, control1.y, control2.x, control2.y, p2.x, p2.y)
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255.0f + 0.5f).toInt(),
        (red * 255.0f + 0.5f).toInt(),
        (green * 255.0f + 0.5f).toInt(),
        (blue * 255.0f + 0.5f).toInt()
    )
}
