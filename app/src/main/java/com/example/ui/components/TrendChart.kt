package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InsulinRecord
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import java.time.LocalDate
import java.util.Locale
import kotlin.math.max

/**
 * 顶部走势折线图展示模式
 */
enum class TopTrendChartType(val label: String) {
    RECENT_7D("7日趋势"),
    TODAY_24H("当日走势"),
    FASTING_POST("空腹/餐后")
}

/**
 * 首页顶部多功能轻量折线图看板（支持用户自由切换图表类型，支持随时折叠/展开）
 * - 支持随卡片滑动动态联动显示对应日期的“当日走势”
 */
@Composable
fun TrendChart(
    records: List<InsulinRecord>,
    allRecords: List<InsulinRecord> = emptyList(),
    selectedDate: String? = null,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null,
    selectedChartType: TopTrendChartType = TopTrendChartType.RECENT_7D,
    onSelectChartType: ((TopTrendChartType) -> Unit)? = null
) {
    var localSelectedChartType by rememberSaveable { mutableStateOf(selectedChartType) }
    val currentChartType = if (onSelectChartType != null) selectedChartType else localSelectedChartType
    val setChartType: (TopTrendChartType) -> Unit = { newType ->
        if (onSelectChartType != null) {
            onSelectChartType(newType)
        } else {
            localSelectedChartType = newType
        }
    }
    val todayStr = remember { LocalDate.now().toString() }
    val effectiveDate = selectedDate ?: todayStr
    val activeDayRecord = remember(allRecords, records, effectiveDate) {
        allRecords.find { it.date == effectiveDate } ?: records.find { it.date == effectiveDate }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trend_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            // 顶栏：左侧标题与折叠开关，右侧图表类型自由切换分段胶囊
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.clickable(enabled = onToggleExpanded != null) { onToggleExpanded?.invoke() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(TealPrimary, CircleShape)
                    )
                    Text(
                        text = "血糖走势",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (onToggleExpanded != null) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "折叠走势图" else "展开走势图",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isExpanded) {
                    // 图表类型自由选择分段控制器（7日趋势 | 当日走势 | 空腹/餐后）
                    val types = remember { TopTrendChartType.entries }
                    val selectedTypeIndex = types.indexOf(currentChartType).coerceAtLeast(0)
                    val animatedTypeIndex by animateFloatAsState(
                        targetValue = selectedTypeIndex.toFloat(),
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "top_chart_tab_indicator"
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .width(204.dp)
                            .height(28.dp)
                            .clip(AppleSegmentTrackShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(2.5.dp)
                    ) {
                        val tabWidth = maxWidth / types.size
                        // 滑动指示高亮块
                        Box(
                            modifier = Modifier
                                .offset(x = tabWidth * animatedTypeIndex)
                                .width(tabWidth)
                                .fillMaxHeight()
                                .clip(AppleSegmentThumbShape)
                                .background(TealPrimary)
                        )

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            types.forEach { type ->
                                val isSel = type == currentChartType
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(AppleSegmentThumbShape)
                                        .applePressEffect(0.96f)
                                        .clickable { setChartType(type) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "已折叠 · 点击展开",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.clickable(enabled = onToggleExpanded != null) { onToggleExpanded?.invoke() }
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))

                    AnimatedContent(
                        targetState = currentChartType,
                        transitionSpec = {
                            val order = mapOf(
                                TopTrendChartType.RECENT_7D to 0,
                                TopTrendChartType.TODAY_24H to 1,
                                TopTrendChartType.FASTING_POST to 2
                            )
                            val fromOrder = order[initialState] ?: 0
                            val toOrder = order[targetState] ?: 0
                            val duration = 200
                            val ease = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
                            val slideDist = { width: Int -> (width * 0.20f).toInt() }

                            if (toOrder > fromOrder) {
                                (fadeIn(animationSpec = tween(duration, easing = ease)) +
                                        slideInHorizontally(animationSpec = tween(duration, easing = ease)) { slideDist(it) } +
                                        scaleIn(initialScale = 0.98f, animationSpec = tween(duration, easing = ease)))
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(duration / 2, easing = LinearEasing)) +
                                                slideOutHorizontally(animationSpec = tween(duration / 2, easing = ease)) { -slideDist(it) } +
                                                scaleOut(targetScale = 0.98f, animationSpec = tween(duration / 2, easing = LinearEasing))
                                    )
                            } else {
                                (fadeIn(animationSpec = tween(duration, easing = ease)) +
                                        slideInHorizontally(animationSpec = tween(duration, easing = ease)) { -slideDist(it) } +
                                        scaleIn(initialScale = 0.98f, animationSpec = tween(duration, easing = ease)))
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(duration / 2, easing = LinearEasing)) +
                                                slideOutHorizontally(animationSpec = tween(duration / 2, easing = ease)) { slideDist(it) } +
                                                scaleOut(targetScale = 0.98f, animationSpec = tween(duration / 2, easing = LinearEasing))
                                    )
                            }.using(SizeTransform(clip = false))
                        },
                        label = "top_chart_type_slide_transition"
                    ) { chartType ->
                        when (chartType) {
                            TopTrendChartType.RECENT_7D -> {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LegendItem(color = AppThemeColors.breakfastColor, label = "空腹", isLine = true)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        LegendItem(color = AppThemeColors.lunchColor, label = "餐后均值", isLine = true, isDashed = true)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        LegendItem(color = if (AppThemeColors.isDark) Color(0xFF64748B) else Color(0xFF94A3B8), label = "胰岛素", isBar = true)
                                    }
                                    if (records.isEmpty()) {
                                        EmptyChartPlaceholder("暂无近期走势数据")
                                    } else {
                                        CanvasChart(records = records)
                                    }
                                }
                            }
                            TopTrendChartType.TODAY_24H -> {
                                val dateTarget = effectiveDate
                                val recordForDate = remember(allRecords, records, dateTarget) {
                                    allRecords.find { it.date == dateTarget } ?: records.find { it.date == dateTarget }
                                }
                                val dateLabel = if (dateTarget == todayStr) "当日实测" else "${if (dateTarget.length >= 10) dateTarget.substring(5) else dateTarget} 实测"
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LegendItem(color = TealPrimary, label = dateLabel, isLine = true)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        LegendItem(color = Color(0xFF059669).copy(alpha = 0.4f), label = "达标区(3.9-10)", isBar = true)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        LegendItem(color = if (AppThemeColors.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7), label = "时段用药", isBar = true)
                                    }
                                    TodayGlucoseCanvasChart(
                                        record = recordForDate,
                                        displayDate = dateTarget,
                                        allRecords = allRecords.ifEmpty { records }
                                    )
                                }
                            }
                            TopTrendChartType.FASTING_POST -> {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LegendItem(color = Color(0xFF0284C7), label = "晨起空腹", isLine = true)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        LegendItem(color = Color(0xFFEA580C), label = "餐后峰值", isLine = true)
                                    }
                                    if (records.isEmpty()) {
                                        EmptyChartPlaceholder("暂无空腹/餐后对比数据")
                                    } else {
                                        FastingVsPostprandialCanvasChart(records = records)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isLine: Boolean = false,
    isDashed: Boolean = false,
    isBar: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (isBar) {
            Box(
                modifier = Modifier
                    .size(width = 7.dp, height = 7.dp)
                    .background(color.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
            )
        } else if (isLine) {
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 2.5.dp)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 1. 近7日多日综合折线图（空腹 + 餐后均值 + 胰岛素柱）
 */
@Composable
private fun CanvasChart(records: List<InsulinRecord>) {
    val fastingColor = AppThemeColors.breakfastColor
    val postMealColor = AppThemeColors.lunchColor
    val isDark = AppThemeColors.isDark
    val insulinBarColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
    val gridLineColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textPaintColor = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.GRAY
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .testTag("trend_canvas")
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 26.dp.toPx()
        val paddingRight = 10.dp.toPx()
        val paddingTop = 8.dp.toPx()
        val paddingBottom = 20.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val minGlucose = 3.0f
        val maxObservedGlucose = records.flatMap {
            listOfNotNull(it.fastingBG, it.postMealAverageBG, it.postBfBG, it.postLunchBG, it.postDinnerBG)
        }.maxOrNull() ?: 10f

        val stepSize = kotlin.math.ceil((maxObservedGlucose + 1.5f - minGlucose) / 3f).coerceAtLeast(3f)
        val maxGlucose = minGlucose + stepSize * 3f

        val maxObservedInsulin = records.map { it.totalInsulin }.maxOrNull() ?: 0f
        val maxInsulin = max(40.0f, kotlin.math.ceil((maxObservedInsulin + 5f) / 10f) * 10f)

        // Draw horizontal grid lines for glucose (3, 6, 9, 12 mmol/L)
        val gridSteps = 3
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
                y + 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textPaintColor
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }

        val n = records.size
        if (n == 0) return@Canvas

        val totalSlots = 7
        val slotWidth = chartWidth / totalSlots.toFloat()
        fun getSlotCenterX(index: Int): Float = paddingLeft + (index + 0.5f) * slotWidth

        // 1. Draw Insulin Bars
        val barWidth = 10.dp.toPx()
        for (i in records.indices) {
            val r = records[i]
            val cx = getSlotCenterX(i)
            val totalInsulin = r.totalInsulin
            if (totalInsulin > 0) {
                val barFraction = (totalInsulin / maxInsulin).coerceIn(0f, 1f)
                val barH = chartHeight * barFraction
                drawRoundRect(
                    color = insulinBarColor.copy(alpha = 0.7f),
                    topLeft = Offset(cx - barWidth / 2f, height - paddingBottom - barH),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
            }

            val dateLabel = if (r.date.length >= 10) r.date.substring(5) else r.date
            drawContext.canvas.nativeCanvas.drawText(
                dateLabel,
                cx,
                height - 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textPaintColor
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }

        // 2. Draw Fasting Glucose Line (Solid)
        val fastingPoints = mutableListOf<Offset>()
        for (i in records.indices) {
            val bg = records[i].fastingBG ?: records[i].preBfBG
            if (bg != null) {
                val cx = getSlotCenterX(i)
                val fraction = ((bg - minGlucose) / (maxGlucose - minGlucose)).coerceIn(0f, 1f)
                val cy = paddingTop + chartHeight * (1f - fraction)
                fastingPoints.add(Offset(cx, cy))
            }
        }

        if (fastingPoints.size > 1) {
            val fastingPath = Path().apply {
                smoothCurveTo(fastingPoints)
            }
            drawPath(
                path = fastingPath,
                color = fastingColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        for (p in fastingPoints) {
            drawCircle(color = surfaceColor, radius = 3.dp.toPx(), center = p)
            drawCircle(color = fastingColor, radius = 1.8.dp.toPx(), center = p)
        }

        // 3. Draw Post-Meal Average Glucose Line (Soft Dashed Smooth Curve)
        val postMealPoints = mutableListOf<Offset>()
        for (i in records.indices) {
            val avg = records[i].postMealAverageBG
            if (avg != null) {
                val cx = getSlotCenterX(i)
                val fraction = ((avg - minGlucose) / (maxGlucose - minGlucose)).coerceIn(0f, 1f)
                val cy = paddingTop + chartHeight * (1f - fraction)
                postMealPoints.add(Offset(cx, cy))
            }
        }

        if (postMealPoints.size > 1) {
            val postMealPath = Path().apply {
                smoothCurveTo(postMealPoints)
            }
            drawPath(
                path = postMealPath,
                color = postMealColor,
                style = Stroke(
                    width = 1.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                    cap = StrokeCap.Round
                )
            )
        }
        for (p in postMealPoints) {
            drawCircle(color = surfaceColor, radius = 2.8.dp.toPx(), center = p)
            drawCircle(color = postMealColor, radius = 1.6.dp.toPx(), center = p)
        }
    }
}

private data class TodayMovingPoint(
    val x: Float,
    val y: Float,
    val value: Float,
    val alpha: Float,
    val scale: Float
)

internal fun calculateTodayMaxGlucose(maxObserved: Float): Pair<Float, List<Int>> {
    return when {
        maxObserved <= 10.5f -> 12f to listOf(3, 6, 9, 12)
        maxObserved <= 13.8f -> 15f to listOf(3, 7, 11, 15)
        maxObserved <= 16.8f -> 18f to listOf(3, 8, 13, 18)
        else -> 21f to listOf(3, 9, 15, 21)
    }
}

/**
 * 2. 当日血糖动态走势图（按时段打点 + 平滑曲线 + 达标走廊）
 * - 表格不动：网格线、Y轴刻度、TIR达标区、X轴时段完全静止
 * - 只对点的移动做衔接动画，动画不是渐变键入，而是物理移动与贝塞尔跟随形变
 */
@Composable
private fun TodayGlucoseCanvasChart(
    record: InsulinRecord?,
    displayDate: String? = null,
    allRecords: List<InsulinRecord> = emptyList()
) {
    val isDark = AppThemeColors.isDark
    val gridLineColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textPaintColor = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.GRAY
    val surfaceColor = MaterialTheme.colorScheme.surface

    val periods = listOf("空腹", "早后", "午前", "午后", "晚前", "晚后", "睡前")

    // 当前日期的目标血糖值列表（7个时段）
    val currentValues = remember(record) {
        if (record == null) List(7) { null }
        else listOf(
            record.fastingBG ?: record.preBfBG,
            record.postBfBG ?: record.getPostMealList(com.example.data.MealPeriod.MORNING).firstOrNull()?.value,
            record.preLunchBG,
            record.postLunchBG ?: record.getPostMealList(com.example.data.MealPeriod.LUNCH).firstOrNull()?.value,
            record.preDinnerBG,
            record.postDinnerBG ?: record.getPostMealList(com.example.data.MealPeriod.DINNER).firstOrNull()?.value,
            record.preNightBG
        )
    }

    // 当前日期的时段用药剂量列表（7个时段：空腹/晨前、早后、午间餐前、午后、晚间餐前、晚后、睡前）
    val currentInsulinValues = remember(record) {
        if (record == null) List(7) { null }
        else listOf(
            record.bfInsulin?.takeIf { it > 0 },
            null,
            record.lunchInsulin?.takeIf { it > 0 },
            null,
            record.dinnerInsulin?.takeIf { it > 0 },
            null,
            record.bedtimeInsulin?.takeIf { it > 0 }
        )
    }

    // 状态记录：记录动画起始点、目标点、手势滑动方向（-1 向左/前一天，1 向右/后一天）
    var fromValues by remember { mutableStateOf<List<Float?>>(currentValues) }
    var toValues by remember { mutableStateOf<List<Float?>>(currentValues) }
    var fromInsulinValues by remember { mutableStateOf<List<Float?>>(currentInsulinValues) }
    var toInsulinValues by remember { mutableStateOf<List<Float?>>(currentInsulinValues) }
    var previousDate by remember { mutableStateOf<String?>(displayDate) }
    var transitionDirection by remember { mutableFloatStateOf(1f) }

    val animProgress = remember { Animatable(1f) }

    LaunchedEffect(record, displayDate) {
        if (displayDate != previousDate || currentValues != toValues || currentInsulinValues != toInsulinValues) {
            val isEarlier = if (previousDate != null && displayDate != null) displayDate < previousDate!! else false
            transitionDirection = if (isEarlier) -1f else 1f

            // 保存当前帧的插值状态作为新的起点
            fromValues = if (animProgress.value < 1f) {
                List(7) { idx ->
                    val f = fromValues.getOrNull(idx)
                    val t = toValues.getOrNull(idx)
                    if (f != null && t != null) f + (t - f) * animProgress.value
                    else t ?: f
                }
            } else {
                toValues
            }
            toValues = currentValues

            fromInsulinValues = if (animProgress.value < 1f) {
                List(7) { idx ->
                    val f = fromInsulinValues.getOrNull(idx)
                    val t = toInsulinValues.getOrNull(idx)
                    if (f != null && t != null) f + (t - f) * animProgress.value
                    else t ?: f
                }
            } else {
                toInsulinValues
            }
            toInsulinValues = currentInsulinValues
            previousDate = displayDate

            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val todayStr = remember { LocalDate.now().toString() }
    val isToday = displayDate == null || displayDate == todayStr
    val datePrefix = if (isToday) "当日" else "${displayDate?.substring(5)} "

    val minGlucose = 3.0f

    // 动态 Y 轴量程自适应：根据当日实际数据最大值自适应量程，绝不浪费空间（例如最大值13.5自适应至15，杜绝固定21大片空白）
    val toMaxObserved = remember(toValues) {
        toValues.filterNotNull().maxOrNull() ?: 8f
    }
    val fromMaxObserved = remember(fromValues) {
        fromValues.filterNotNull().maxOrNull() ?: 8f
    }
    val (targetMaxGlucose, targetTicks) = remember(toMaxObserved) {
        calculateTodayMaxGlucose(toMaxObserved)
    }
    val (startMaxGlucose, _) = remember(fromMaxObserved) {
        calculateTodayMaxGlucose(fromMaxObserved)
    }

    val toMaxInsulin = remember(toInsulinValues) {
        toInsulinValues.filterNotNull().maxOrNull() ?: 8f
    }
    val fromMaxInsulin = remember(fromInsulinValues) {
        fromInsulinValues.filterNotNull().maxOrNull() ?: 8f
    }
    val dayMaxDose = max(toMaxInsulin, fromMaxInsulin)
    val maxInsulinScale = remember(dayMaxDose) {
        (kotlin.math.ceil(dayMaxDose * 1.15f / 2f) * 2f).coerceIn(12f, 40f)
    }

    val progress = animProgress.value
    val curMaxGlucose = startMaxGlucose + (targetMaxGlucose - startMaxGlucose) * progress

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(122.dp)
            .testTag("today_glucose_canvas")
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 26.dp.toPx()
        val paddingRight = 12.dp.toPx()
        val paddingTop = 12.dp.toPx()
        val paddingBottom = 20.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // 1. 完全静态绘制：TIR 绿色达标背景区（3.9 ~ 10.0 mmol/L）—— 表格绝对不动
        val tirTop = paddingTop + chartHeight * (1f - (10.0f - minGlucose) / (curMaxGlucose - minGlucose))
        val tirBottom = paddingTop + chartHeight * (1f - (3.9f - minGlucose) / (curMaxGlucose - minGlucose))
        drawRect(
            color = Color(0xFF059669).copy(alpha = if (isDark) 0.12f else 0.08f),
            topLeft = Offset(paddingLeft, tirTop),
            size = Size(chartWidth, (tirBottom - tirTop).coerceAtLeast(0f))
        )

        // 2. 完全静态绘制：水平网格线与 Y 轴刻度（3, 7, 11, 15 或 3, 6, 9, 12）—— 表格绝对不动
        val gridSteps = 3
        for (i in 0..gridSteps) {
            val lineFraction = i / gridSteps.toFloat()
            val y = paddingTop + chartHeight * (1f - lineFraction)
            drawLine(
                color = gridLineColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 0.8.dp.toPx()
            )

            val tickVal = targetTicks.getOrElse(i) {
                (minGlucose + (targetMaxGlucose - minGlucose) * lineFraction).toInt()
            }
            drawContext.canvas.nativeCanvas.drawText(
                "$tickVal",
                paddingLeft - 6.dp.toPx(),
                y + 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textPaintColor
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }

        // 3. 完全静态绘制：X 轴时段文字标签（空腹, 早后, 午前, 午后, 晚前, 晚后, 睡前）—— 表格绝对不动
        val slotWidth = chartWidth / (periods.size - 1).toFloat()
        fun getSlotX(idx: Int): Float = paddingLeft + idx * slotWidth

        for (i in periods.indices) {
            val cx = getSlotX(i)
            drawContext.canvas.nativeCanvas.drawText(
                periods[i],
                cx,
                height - 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textPaintColor
                    textSize = 8.5.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }

        // 4. 绘制当日时段用药柱状图（身材精巧，最高 30% 高度，避免遮挡上方血糖折线）
        val barWidth = 11.dp.toPx()
        val maxBarHeight = chartHeight * 0.30f
        val insulinBarColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

        for (i in 0 until 7) {
            val vFrom = fromInsulinValues.getOrNull(i)
            val vTo = toInsulinValues.getOrNull(i)
            val curDose = if (vFrom != null && vTo != null) {
                vFrom + (vTo - vFrom) * progress
            } else if (vFrom == null && vTo != null) {
                vTo * progress
            } else if (vFrom != null && vTo == null) {
                vFrom * (1f - progress)
            } else null

            if (curDose != null && curDose > 0.05f) {
                val cx = getSlotX(i)
                val doseFraction = (curDose / maxInsulinScale).coerceIn(0f, 1f)
                val barH = max(4.dp.toPx(), doseFraction * maxBarHeight)
                val baseY = height - paddingBottom
                val barTop = baseY - barH
                val barAlpha = if (vTo != null) progress.coerceIn(0.2f, 1f) else (1f - progress).coerceIn(0f, 1f)

                // 柱状图本体（圆角柔和）
                drawRoundRect(
                    color = insulinBarColor.copy(alpha = (if (isDark) 0.35f else 0.22f) * barAlpha),
                    topLeft = Offset(cx - barWidth / 2f, barTop),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                // 顶部高亮边线
                drawLine(
                    color = insulinBarColor.copy(alpha = (if (isDark) 0.85f else 0.70f) * barAlpha),
                    start = Offset(cx - barWidth / 2f + 1.dp.toPx(), barTop),
                    end = Offset(cx + barWidth / 2f - 1.dp.toPx(), barTop),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 标注时段用药具体数值（如 14u / 6u）
                val doseLabel = if (curDose % 1f == 0f) "${curDose.toInt()}u" else String.format(Locale.US, "%.1fu", curDose)
                val dosePaint = android.graphics.Paint().apply {
                    color = insulinBarColor.copy(alpha = (if (isDark) 0.95f else 0.85f) * barAlpha).toArgb()
                    textSize = 8.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                if (barH >= 15.dp.toPx()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        doseLabel,
                        cx,
                        barTop + 9.5.dp.toPx(),
                        dosePaint
                    )
                } else {
                    drawContext.canvas.nativeCanvas.drawText(
                        doseLabel,
                        cx,
                        barTop - 2.dp.toPx(),
                        dosePaint
                    )
                }
            }
        }

        // 5. 计算当前帧所有动态位移中的数据点（只对点的移动做衔接动画，不是渐变键入，而是物理移动）
        val movingPoints = mutableListOf<TodayMovingPoint>()

        for (i in 0 until 7) {
            val vFrom = fromValues.getOrNull(i)
            val vTo = toValues.getOrNull(i)
            val targetX = getSlotX(i)

            if (vFrom != null && vTo != null) {
                // 两天均有数据：沿垂直方向物理滑移（Y轴平滑移动）
                val curVal = vFrom + (vTo - vFrom) * progress
                val curY = paddingTop + chartHeight * (1f - (curVal - minGlucose) / (curMaxGlucose - minGlucose))
                movingPoints.add(
                    TodayMovingPoint(
                        x = targetX,
                        y = curY,
                        value = curVal,
                        alpha = 1f,
                        scale = 1f
                    )
                )
            } else if (vFrom == null && vTo != null) {
                // 新增点：从手势滑动方向物理滑入（水平位移滑入 + Y轴定位，非渐变键入）
                val targetY = paddingTop + chartHeight * (1f - (vTo - minGlucose) / (curMaxGlucose - minGlucose))
                val startX = targetX + transitionDirection * slotWidth * 0.7f
                val curX = startX + (targetX - startX) * progress
                val curScale = (0.4f + 0.6f * progress).coerceIn(0.4f, 1f)
                val curAlpha = progress.coerceIn(0.2f, 1f)
                movingPoints.add(
                    TodayMovingPoint(
                        x = curX,
                        y = targetY,
                        value = vTo,
                        alpha = curAlpha,
                        scale = curScale
                    )
                )
            } else if (vFrom != null && vTo == null) {
                // 消失点：沿手势滑动方向物理滑出（水平位移滑出，非渐变淡出）
                if (progress < 1f) {
                    val startY = paddingTop + chartHeight * (1f - (vFrom - minGlucose) / (curMaxGlucose - minGlucose))
                    val endX = targetX - transitionDirection * slotWidth * 0.7f
                    val curX = targetX + (endX - targetX) * progress
                    val curScale = (1f - 0.6f * progress).coerceIn(0.4f, 1f)
                    val curAlpha = (1f - progress).coerceIn(0f, 1f)
                    movingPoints.add(
                        TodayMovingPoint(
                            x = curX,
                            y = startY,
                            value = vFrom,
                            alpha = curAlpha,
                            scale = curScale
                        )
                    )
                }
            }
        }

        // 5. 绘制连接曲线（贝塞尔曲线连接当前帧的所有动态移动点）
        val sortedPoints = movingPoints.filter { it.alpha > 0.05f }.sortedBy { it.x }
        if (sortedPoints.size > 1) {
            val curvePath = Path().apply {
                smoothCurveTo(sortedPoints.map { Offset(it.x, it.y) })
            }
            drawPath(
                path = curvePath,
                color = TealPrimary.copy(alpha = if (toValues.any { it != null }) 1f else (1f - progress).coerceIn(0f, 1f)),
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 6. 绘制动态数据点与上方数值标签（随点一同位移，非渐变键入）
        for (mp in movingPoints) {
            val pointColor = when {
                mp.value < 3.9f -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                mp.value > 10.0f -> if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
                else -> TealPrimary
            }.copy(alpha = mp.alpha)

            val center = Offset(mp.x, mp.y)
            drawCircle(
                color = surfaceColor.copy(alpha = mp.alpha),
                radius = 3.8.dp.toPx() * mp.scale,
                center = center
            )
            drawCircle(
                color = pointColor,
                radius = 2.4.dp.toPx() * mp.scale,
                center = center
            )

            // 数值标签跟随数据点平滑位移（边界点智能避让，避免与 Y 轴刻度紧挨碰擦）
            if (mp.alpha > 0.2f) {
                val align = when {
                    mp.x <= paddingLeft + 6.dp.toPx() -> android.graphics.Paint.Align.LEFT
                    mp.x >= width - paddingRight - 6.dp.toPx() -> android.graphics.Paint.Align.RIGHT
                    else -> android.graphics.Paint.Align.CENTER
                }
                val labelX = when {
                    mp.x <= paddingLeft + 6.dp.toPx() -> mp.x + 2.dp.toPx()
                    mp.x >= width - paddingRight - 6.dp.toPx() -> mp.x - 2.dp.toPx()
                    else -> mp.x
                }
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%.1f", mp.value),
                    labelX,
                    mp.y - 5.dp.toPx() * mp.scale,
                    android.graphics.Paint().apply {
                        color = pointColor.toArgb()
                        textSize = 9.sp.toPx() * mp.scale
                        textAlign = align
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                )
            }
        }

        // 7. 当日无数据提示（若新旧状态均无任何有效数据，居中轻量提示，依然保持静态表格网格可见）
        if (movingPoints.isEmpty()) {
            drawContext.canvas.nativeCanvas.drawText(
                "${datePrefix}暂无实测血糖数据",
                paddingLeft + chartWidth / 2f,
                paddingTop + chartHeight / 2f + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textPaintColor
                    textSize = 10.5.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}

/**
 * 3. 近期空腹 vs 餐后双轨走势对比图
 */
@Composable
internal fun FastingVsPostprandialCanvasChart(records: List<InsulinRecord>) {
    val fastingColor = Color(0xFF0284C7)
    val postMealColor = Color(0xFFEA580C)
    val isDark = AppThemeColors.isDark
    val gridLineColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textPaintColor = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.GRAY
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .testTag("fasting_vs_post_canvas")
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 26.dp.toPx()
        val paddingRight = 10.dp.toPx()
        val paddingTop = 12.dp.toPx()
        val paddingBottom = 20.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val minGlucose = 3.0f
        val maxObserved = records.flatMap {
            listOfNotNull(it.fastingBG, it.preBfBG, it.postBfBG, it.postLunchBG, it.postDinnerBG)
        }.maxOrNull() ?: 10f
        val (maxGlucose, ticks) = calculateTodayMaxGlucose(maxObserved)

        // Grid lines
        val gridSteps = 3
        for (i in 0..gridSteps) {
            val lineFraction = i / gridSteps.toFloat()
            val y = paddingTop + chartHeight * (1f - lineFraction)
            drawLine(
                color = gridLineColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 0.8.dp.toPx()
            )

            val tickVal = ticks.getOrElse(i) { (minGlucose + (maxGlucose - minGlucose) * lineFraction).toInt() }
            drawContext.canvas.nativeCanvas.drawText(
                "$tickVal",
                paddingLeft - 6.dp.toPx(),
                y + 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textPaintColor
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }

        val totalSlots = 7
        val slotWidth = chartWidth / totalSlots.toFloat()
        fun getSlotCenterX(index: Int): Float = paddingLeft + (index + 0.5f) * slotWidth

        // Draw date labels
        for (i in records.indices) {
            val r = records[i]
            val cx = getSlotCenterX(i)
            val dateLabel = if (r.date.length >= 10) r.date.substring(5) else r.date
            drawContext.canvas.nativeCanvas.drawText(
                dateLabel,
                cx,
                height - 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textPaintColor
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }

        // Fasting line
        val fastingPoints = mutableListOf<Offset>()
        for (i in records.indices) {
            val bg = records[i].fastingBG ?: records[i].preBfBG
            if (bg != null) {
                val cx = getSlotCenterX(i)
                val fraction = ((bg - minGlucose) / (maxGlucose - minGlucose)).coerceIn(0f, 1f)
                val cy = paddingTop + chartHeight * (1f - fraction)
                fastingPoints.add(Offset(cx, cy))
            }
        }
        if (fastingPoints.size > 1) {
            val p = Path().apply { smoothCurveTo(fastingPoints) }
            drawPath(path = p, color = fastingColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
        for (pt in fastingPoints) {
            drawCircle(color = surfaceColor, radius = 3.dp.toPx(), center = pt)
            drawCircle(color = fastingColor, radius = 1.8.dp.toPx(), center = pt)
        }

        // Postprandial Peak line
        val postMealPoints = mutableListOf<Offset>()
        for (i in records.indices) {
            val r = records[i]
            val maxPost = listOfNotNull(r.postBfBG, r.postLunchBG, r.postDinnerBG).maxOrNull() ?: r.postMealAverageBG
            if (maxPost != null) {
                val cx = getSlotCenterX(i)
                val fraction = ((maxPost - minGlucose) / (maxGlucose - minGlucose)).coerceIn(0f, 1f)
                val cy = paddingTop + chartHeight * (1f - fraction)
                postMealPoints.add(Offset(cx, cy))
            }
        }
        if (postMealPoints.size > 1) {
            val p = Path().apply { smoothCurveTo(postMealPoints) }
            drawPath(path = p, color = postMealColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
        for (pt in postMealPoints) {
            drawCircle(color = surfaceColor, radius = 3.dp.toPx(), center = pt)
            drawCircle(color = postMealColor, radius = 1.8.dp.toPx(), center = pt)
        }
    }
}

/**
 * 贝塞尔平滑曲线连接点集（适度圆滑，消除过度臃肿波浪，紧贴数据走势）
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
