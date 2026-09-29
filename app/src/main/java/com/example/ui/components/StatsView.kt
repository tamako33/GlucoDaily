package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.ui.TableDateRange
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import java.time.LocalDate
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * 临床级综合控糖统计图表中心 (StatsView)
 *
 * 核心指标矩阵：
 * 1. TIR (Time in Range, 葡萄糖目标范围内时间占比) 环形图 —— 国际控糖金标准（目标 ≥ 70%）；
 * 2. 日内 7 点时段血糖波动与趋势模式图 (离散版 AGP) —— 展现各餐段中位均值与最大波动带；
 * 3. 胰岛素用药量与血糖联动趋势双轴图 (Insulin vs Glucose) —— 直观呈现药量增减与控糖效果因果关系；
 * 4. 餐后血糖漂移差值图 (PPGE) —— 评估早/中/晚三餐主食升糖负荷与餐前给药时机；
 * 5. 低血糖事件时段预警监控 —— 深度防范严重及夜间无感知低血糖风险。
 *
 * 拓展：支持右滑/点击切换进入专属全屏折线走势图谱大屏（按天翻看24h因果全景、7天AGP叠图、双轨走势）
 */
@Composable
fun StatsView(
    records: List<InsulinRecord>,
    allRecords: List<InsulinRecord>,
    selectedRange: TableDateRange,
    onRangeSelected: (TableDateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    // 提取当前筛选周期内的所有有效血糖测量值
    val allBGs = remember(records) {
        val list = mutableListOf<Float>()
        records.forEach { r ->
            r.fastingBG?.let { if (it > 0) list.add(it) }
            r.preBfBG?.let { if (it > 0) list.add(it) }
            r.getPostMealList(MealPeriod.MORNING).forEach { if (it.value > 0) list.add(it.value) }
            r.preLunchBG?.let { if (it > 0) list.add(it) }
            r.getPostMealList(MealPeriod.LUNCH).forEach { if (it.value > 0) list.add(it.value) }
            r.preDinnerBG?.let { if (it > 0) list.add(it) }
            r.getPostMealList(MealPeriod.DINNER).forEach { if (it.value > 0) list.add(it.value) }
            r.preNightBG?.let { if (it > 0) list.add(it) }
            r.getPostMealList(MealPeriod.NIGHT).forEach { if (it.value > 0) list.add(it.value) }
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 日期维度切换器胶囊（近7天、近14天、近30天、全部，配有物理滑块动效）
        DateRangeSelectorCapsule(
            selectedRange = selectedRange,
            onRangeSelected = onRangeSelected
        )

        if (allBGs.isEmpty()) {
            EmptyStatsCard()
        } else {
            // 2. 总体控糖概况大看板 (核心KPI总结) —— 卡片本体固定不淡入淡出，内部数值平滑数字变动
            StatsSummaryHeader(allBGs = allBGs, recordCount = records.size)

            // 3. TIR 目标范围内时间占比 (国际控糖金标准环形图) —— 饼形图不做渐入渐出，而是对比例的改变直接做平滑过渡！
            TirDoughnutChartCard(allBGs = allBGs)

            // 4. 全天 24 小时血糖与用药对照走势 (直接呈现在看板主信息流中，支持左右切天、翻页、对齐底栏指标)
            Daily24hGlucoseDashboardCard(allRecords = allRecords)

            // 5 ~ 8. 下方各图表卡片（AGP、双轴、差值、低血糖监控）—— 随天数切换平滑滑动动画
            AnimatedContent(
                targetState = selectedRange,
                transitionSpec = {
                    val ranges = TableDateRange.entries
                    val fromIdx = ranges.indexOf(initialState)
                    val toIdx = ranges.indexOf(targetState)
                    val isForward = toIdx > fromIdx
                    val duration = 180
                    val ease = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
                    val slideDist = { width: Int -> (width * 0.15f).toInt() }
                    if (isForward) {
                        (fadeIn(tween(duration, easing = ease)) +
                                slideInHorizontally(tween(duration, easing = ease)) { slideDist(it) })
                            .togetherWith(
                                fadeOut(tween(duration / 2)) +
                                        slideOutHorizontally(tween(duration / 2)) { -slideDist(it) }
                            )
                    } else {
                        (fadeIn(tween(duration, easing = ease)) +
                                slideInHorizontally(tween(duration, easing = ease)) { -slideDist(it) })
                            .togetherWith(
                                fadeOut(tween(duration / 2)) +
                                        slideOutHorizontally(tween(duration / 2)) { slideDist(it) }
                            )
                    }.using(SizeTransform(clip = false))
                },
                label = "stats_charts_transition"
            ) { _ ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 5. 日内 7 点时段血糖波动与趋势图 (离散版 AGP)
                    DailySevenPointAgpChartCard(records = records)

                    // 6. 每日用药量与血糖联动趋势双轴图 (Insulin vs Glucose)
                    MedicationAndGlucoseDualAxisCard(records = records)

                    // 7. 早中晚三餐餐后血糖漂移差值图 (PPGE)
                    PostprandialExcursionChartCard(records = records)

                    // 8. 低血糖安全监控与时段预警卡片
                    HypoglycemiaSafetyAlertCard(records = records)
                }
            }
        }

        // 底部充足防遮挡间距，防止浮动按钮(FAB)遮挡任何图表或指标
        Spacer(modifier = Modifier.height(96.dp))
    }
}

/**
 * 统计视图日期范围筛选胶囊（严格遵循 Apple 设计标准：全胶囊轨槽、spring 物理滑块与 applePressEffect）
 */
@Composable
fun DateRangeSelectorCapsule(
    selectedRange: TableDateRange,
    onRangeSelected: (TableDateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val ranges = remember { TableDateRange.entries }
    val selectedIndex = ranges.indexOf(selectedRange).coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "range_slider"
    )

    val isDark = AppThemeColors.isDark
    val trackBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA).copy(alpha = 0.6f)
    val trackBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(AppleSegmentTrackShape)
            .background(trackBg)
            .border(BorderStroke(1.dp, trackBorderColor), AppleSegmentTrackShape)
            .padding(2.5.dp)
    ) {
        val tabWidth = maxWidth / ranges.size

        // 主题色滑块（与全局分段控制器 100% 统一圆角）
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
            ranges.forEachIndexed { index, range ->
                val isSelected = range == selectedRange
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "range_text_color_$index"
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
                        ) { onRangeSelected(range) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.label,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
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

private data class DashboardMovingPoint(
    val x: Float,
    val y: Float,
    val value: Float,
    val alpha: Float,
    val scale: Float,
    val insulinDose: Float?,
    val insulinAlpha: Float
)

/**
 * 全天 24 小时血糖与用药对照走势卡片（直接嵌入看板主信息流）
 * - 边框统一，质感轻盈，严格遵循 Apple 18dp 圆角与 1px 发丝边框；
 * - 顶部日期微调切换器胶囊与手势左右拖动协同切日；
 * - 表格绝对不动，只对数据点做平滑物理移动动画（与当日走势完全一致）；
 * - 底部 5 大统计指标居中对齐，数值切换采用 spring 曲线平滑数字插值（与周控糖概况完全一致）。
 */
@Composable
private fun Daily24hGlucoseDashboardCard(
    allRecords: List<InsulinRecord>,
    modifier: Modifier = Modifier
) {
    val isDark = AppThemeColors.isDark
    val availableDates = remember(allRecords) {
        allRecords.map { it.date }.distinct().sortedDescending()
    }

    var currentDateIndex by rememberSaveable { mutableIntStateOf(0) }
    val validIndex = if (availableDates.isNotEmpty()) currentDateIndex.coerceIn(0, availableDates.size - 1) else 0
    val currentDate = availableDates.getOrNull(validIndex) ?: ""
    val record = remember(allRecords, currentDate) { allRecords.find { it.date == currentDate } }

    data class TimeSlotData(
        val periodName: String,
        val bg: Float?,
        val insulinDose: Float?,
        val medName: String
    )

    val periods = listOf("空腹", "早后", "午前", "午后", "晚前", "晚后", "睡前")

    // 当前日期的时段数据（7个时段）
    val currentSlots = remember(record) {
        if (record == null) List(7) { TimeSlotData(periods[it], null, null, "") }
        else listOf(
            TimeSlotData("空腹", record.fastingBG ?: record.preBfBG, record.bfInsulin, record.bfMedName),
            TimeSlotData("早后", record.postBfBG ?: record.getPostMealList(MealPeriod.MORNING).firstOrNull()?.value, null, ""),
            TimeSlotData("午前", record.preLunchBG, record.lunchInsulin, record.lunchMedName),
            TimeSlotData("午后", record.postLunchBG ?: record.getPostMealList(MealPeriod.LUNCH).firstOrNull()?.value, null, ""),
            TimeSlotData("晚前", record.preDinnerBG, record.dinnerInsulin, record.dinnerMedName),
            TimeSlotData("晚后", record.postDinnerBG ?: record.getPostMealList(MealPeriod.DINNER).firstOrNull()?.value, null, ""),
            TimeSlotData("睡前", record.preNightBG, record.bedtimeInsulin, record.nightMedName)
        )
    }

    var fromSlots by remember { mutableStateOf<List<TimeSlotData>>(currentSlots) }
    var toSlots by remember { mutableStateOf<List<TimeSlotData>>(currentSlots) }
    var previousDate by remember { mutableStateOf<String?>(currentDate) }
    var transitionDirection by remember { mutableFloatStateOf(1f) }

    val animProgress = remember { Animatable(1f) }

    LaunchedEffect(record, currentDate) {
        if (currentDate != previousDate || currentSlots != toSlots) {
            val isEarlier = if (previousDate != null && currentDate.isNotBlank()) currentDate < previousDate!! else false
            transitionDirection = if (isEarlier) -1f else 1f

            fromSlots = if (animProgress.value < 1f) {
                List(7) { idx ->
                    val f = fromSlots.getOrNull(idx)
                    val t = toSlots.getOrNull(idx)
                    val fBg = f?.bg
                    val tBg = t?.bg
                    val interpBg = if (fBg != null && tBg != null) fBg + (tBg - fBg) * animProgress.value else tBg ?: fBg
                    val fDose = f?.insulinDose
                    val tDose = t?.insulinDose
                    val interpDose = if (fDose != null && tDose != null) fDose + (tDose - fDose) * animProgress.value else tDose ?: fDose
                    TimeSlotData(periods[idx], interpBg, interpDose, t?.medName ?: f?.medName ?: "")
                }
            } else {
                toSlots
            }
            toSlots = currentSlots
            previousDate = currentDate

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

    // 动态 Y 轴量程自适应：根据当日实际数据最大值紧凑自适应，绝不浪费空间（例如最大值13.5自适应至15，杜绝固定21大片空白）
    val minGlucose = 3.0f
    val toMaxObserved = remember(toSlots) { toSlots.mapNotNull { it.bg }.maxOrNull() ?: 8f }
    val fromMaxObserved = remember(fromSlots) { fromSlots.mapNotNull { it.bg }.maxOrNull() ?: 8f }
    val (targetMaxGlucose, targetTicks) = remember(toMaxObserved) { calculateTodayMaxGlucose(toMaxObserved) }
    val (startMaxGlucose, _) = remember(fromMaxObserved) { calculateTodayMaxGlucose(fromMaxObserved) }
    val curMaxGlucose = startMaxGlucose + (targetMaxGlucose - startMaxGlucose) * animProgress.value

    // 下方 5 大统计指标（采用 spring 曲线平滑数字插值，和周控糖情况一致）
    val currentValidBgs = remember(currentSlots) { currentSlots.mapNotNull { it.bg } }
    val curMaxBg = remember(currentValidBgs) { currentValidBgs.maxOrNull() ?: 0f }
    val curMinBg = remember(currentValidBgs) { currentValidBgs.minOrNull() ?: 0f }
    val curAvgBg = remember(currentValidBgs) { if (currentValidBgs.isNotEmpty()) currentValidBgs.average().toFloat() else 0f }
    val curInRangeCount = remember(currentValidBgs) { currentValidBgs.count { it in 3.9f..10.0f } }
    val curTirPercent = remember(currentValidBgs) { if (currentValidBgs.isNotEmpty()) (curInRangeCount * 100f / currentValidBgs.size) else 0f }
    val curTotalInsulin = remember(record) { record?.totalInsulin ?: 0f }

    val animMaxBg by animateFloatAsState(
        targetValue = curMaxBg,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "anim_dash_max"
    )
    val animMinBg by animateFloatAsState(
        targetValue = curMinBg,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "anim_dash_min"
    )
    val animAvgBg by animateFloatAsState(
        targetValue = curAvgBg,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "anim_dash_avg"
    )
    val animTirPercent by animateFloatAsState(
        targetValue = curTirPercent,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "anim_dash_tir"
    )
    val animTotalInsulin by animateFloatAsState(
        targetValue = curTotalInsulin,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "anim_dash_insulin"
    )

    var totalDragX by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppleCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = appleCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. 顶栏：标题（左）与日期微调切换器（右），独享整行宽度，空间充裕，绝不错位
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "全天血糖与用药对照",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.28).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )

                // 日期微调切日器（Apple 药丸胶囊）
                if (availableDates.isNotEmpty()) {
                    Surface(
                        shape = ApplePillShape,
                        color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF1F5F9),
                        border = appleCardBorder()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (validIndex < availableDates.size - 1) {
                                        currentDateIndex = validIndex + 1
                                    }
                                },
                                enabled = validIndex < availableDates.size - 1,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .applePressEffect(0.92f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                    contentDescription = "前一天",
                                    tint = if (validIndex < availableDates.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(10.dp)
                                )
                            }

                            AnimatedContent(
                                targetState = currentDate,
                                transitionSpec = {
                                    val isForward = targetState < initialState
                                    val duration = 200
                                    val ease = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                                    if (isForward) {
                                        (fadeIn(tween(duration, easing = ease)) +
                                                slideInHorizontally(tween(duration, easing = ease)) { it / 2 })
                                            .togetherWith(fadeOut(tween(duration / 2)) + slideOutHorizontally(tween(duration / 2)) { -it / 2 })
                                    } else {
                                        (fadeIn(tween(duration, easing = ease)) +
                                                slideInHorizontally(tween(duration, easing = ease)) { -it / 2 })
                                            .togetherWith(fadeOut(tween(duration / 2)) + slideOutHorizontally(tween(duration / 2)) { it / 2 })
                                    }
                                },
                                label = "stats_date_text_transition"
                            ) { dateStr ->
                                val curObj = try { LocalDate.parse(dateStr) } catch (_: Exception) { null }
                                val curIsToday = dateStr == LocalDate.now().toString()
                                val curWeekDay = curObj?.let {
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = dateStr.removePrefix("2026-"),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.12).sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                    )
                                    if (curWeekDay.isNotBlank()) {
                                        Text(
                                            text = curWeekDay,
                                            fontSize = 10.5.sp,
                                            letterSpacing = (-0.12).sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                        )
                                    }
                                    if (curIsToday) {
                                        Surface(
                                            shape = ApplePillShape,
                                            color = TealPrimary,
                                            modifier = Modifier.padding(start = 2.dp)
                                        ) {
                                            Text(
                                                text = "今天",
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (-0.12).sp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                            )
                                        }
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (validIndex > 0) {
                                        currentDateIndex = validIndex - 1
                                    }
                                },
                                enabled = validIndex > 0,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .applePressEffect(0.92f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "后一天",
                                    tint = if (validIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. 次级图例行（独立成行，舒展美观）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF059669), CircleShape))
                    Text("3.9~10.0达标区", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(TealPrimary, CircleShape))
                    Text("血糖走势", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                    Text("💉", fontSize = 10.sp)
                    Text("用药气泡", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 3. 画布区域（支持左右滑动手势切日；表格绝对不动，只对数据点做物理移动动画）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(availableDates, validIndex) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onDragEnd = {
                                if (totalDragX > 50f && validIndex < availableDates.size - 1) {
                                    currentDateIndex = validIndex + 1
                                } else if (totalDragX < -50f && validIndex > 0) {
                                    currentDateIndex = validIndex - 1
                                }
                                totalDragX = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDragX += dragAmount
                            }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val progress = animProgress.value

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingLeft = 28.dp.toPx()
                    val paddingRight = 18.dp.toPx()
                    val paddingTop = 14.dp.toPx()
                    val paddingBottom = 34.dp.toPx()

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom
                    val chartBaseY = paddingTop + chartHeight

                    // 1. 完全静态绘制：TIR 达标区间带 (3.9 ~ 10.0 mmol/L) 沉稳翡翠绿 (#059669) —— 表格绝对不动
                    val tirTop = paddingTop + chartHeight * (1f - (10.0f - minGlucose) / (curMaxGlucose - minGlucose))
                    val tirBottom = paddingTop + chartHeight * (1f - (3.9f - minGlucose) / (curMaxGlucose - minGlucose))
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF059669).copy(alpha = if (isDark) 0.16f else 0.10f),
                                Color(0xFF059669).copy(alpha = if (isDark) 0.08f else 0.04f)
                            )
                        ),
                        topLeft = Offset(paddingLeft, tirTop),
                        size = Size(chartWidth, (tirBottom - tirTop).coerceAtLeast(0f))
                    )

                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    drawLine(
                        color = Color(0xFF059669).copy(alpha = 0.45f),
                        start = Offset(paddingLeft, tirTop),
                        end = Offset(width - paddingRight, tirTop),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = Color(0xFF059669).copy(alpha = 0.45f),
                        start = Offset(paddingLeft, tirBottom),
                        end = Offset(width - paddingRight, tirBottom),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // 2. 完全静态绘制：Y 轴刻度与水平线 —— 表格绝对不动（网格线位置固定，数值随当日范围紧凑自适应）
                    val gridLineColor = if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0)
                    val textPaintColor = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.GRAY

                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val lineFraction = i / gridSteps.toFloat()
                        val gy = paddingTop + chartHeight * (1f - lineFraction)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(paddingLeft, gy),
                            end = Offset(width - paddingRight, gy),
                            strokeWidth = 0.5.dp.toPx()
                        )
                        val tickVal = targetTicks.getOrElse(i) {
                            (minGlucose + (targetMaxGlucose - minGlucose) * lineFraction).toInt()
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "$tickVal",
                            paddingLeft - 6.dp.toPx(),
                            gy + 3.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = textPaintColor
                                textSize = 9.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                                isAntiAlias = true
                            }
                        )
                    }

                    // 3. 完全静态绘制：X 轴时间槽标签（位于图表基准线下方，下方为用药气泡留出位置）
                    val slotCount = 7
                    val slotStep = chartWidth / (slotCount - 1).toFloat()
                    fun getSlotX(idx: Int): Float = paddingLeft + idx * slotStep

                    val periodY = chartBaseY + 12.dp.toPx()
                    for (i in periods.indices) {
                        val cx = getSlotX(i)
                        val slotName = periods[i]
                        drawContext.canvas.nativeCanvas.drawText(
                            slotName,
                            cx,
                            periodY,
                            android.graphics.Paint().apply {
                                color = textPaintColor
                                textSize = 9.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }

                    // 5. 计算当前帧所有动态位移中的数据点（只对点的移动做衔接动画，不是渐变键入，与当日走势完全一致）
                    val movingPoints = mutableListOf<DashboardMovingPoint>()

                    for (i in 0 until 7) {
                        val slotFrom = fromSlots.getOrNull(i)
                        val slotTo = toSlots.getOrNull(i)
                        val vFrom = slotFrom?.bg
                        val vTo = slotTo?.bg
                        val targetX = getSlotX(i)

                        val doseFrom = slotFrom?.insulinDose
                        val doseTo = slotTo?.insulinDose
                        val curDose = doseTo ?: doseFrom
                        val doseAlpha = if (doseTo != null && doseFrom != null) 1f
                        else if (doseTo != null) progress
                        else if (doseFrom != null) (1f - progress).coerceIn(0f, 1f)
                        else 0f

                        if (vFrom != null && vTo != null) {
                            val curVal = vFrom + (vTo - vFrom) * progress
                            val curY = paddingTop + chartHeight * (1f - (curVal - minGlucose) / (curMaxGlucose - minGlucose))
                            movingPoints.add(
                                DashboardMovingPoint(
                                    x = targetX,
                                    y = curY,
                                    value = curVal,
                                    alpha = 1f,
                                    scale = 1f,
                                    insulinDose = curDose,
                                    insulinAlpha = doseAlpha
                                )
                            )
                        } else if (vFrom == null && vTo != null) {
                            val targetY = paddingTop + chartHeight * (1f - (vTo - minGlucose) / (curMaxGlucose - minGlucose))
                            val startX = targetX + transitionDirection * slotStep * 0.7f
                            val curX = startX + (targetX - startX) * progress
                            val curScale = (0.4f + 0.6f * progress).coerceIn(0.4f, 1f)
                            val curAlpha = progress.coerceIn(0.2f, 1f)
                            movingPoints.add(
                                DashboardMovingPoint(
                                    x = curX,
                                    y = targetY,
                                    value = vTo,
                                    alpha = curAlpha,
                                    scale = curScale,
                                    insulinDose = curDose,
                                    insulinAlpha = doseAlpha
                                )
                            )
                        } else if (vFrom != null && vTo == null) {
                            if (progress < 1f) {
                                val startY = paddingTop + chartHeight * (1f - (vFrom - minGlucose) / (curMaxGlucose - minGlucose))
                                val endX = targetX - transitionDirection * slotStep * 0.7f
                                val curX = targetX + (endX - targetX) * progress
                                val curScale = (1f - 0.6f * progress).coerceIn(0.4f, 1f)
                                val curAlpha = (1f - progress).coerceIn(0f, 1f)
                                movingPoints.add(
                                    DashboardMovingPoint(
                                        x = curX,
                                        y = startY,
                                        value = vFrom,
                                        alpha = curAlpha,
                                        scale = curScale,
                                        insulinDose = curDose,
                                        insulinAlpha = doseAlpha
                                    )
                                )
                            }
                        }
                    }

                    // 5. 绘制连接曲线与渐变充填（连接当前帧的所有动态移动点，张力 0.09f）
                    val activeCurvePoints = movingPoints.filter { it.alpha > 0.05f }.sortedBy { it.x }
                    if (activeCurvePoints.size >= 2) {
                        val pts = activeCurvePoints.map { Offset(it.x, it.y) }
                        val path = Path().apply { smoothCurveTo(pts, tension = 0.09f) }
                        val fillPath = Path().apply {
                            smoothCurveTo(pts, tension = 0.09f)
                            lineTo(pts.last().x, height - paddingBottom)
                            lineTo(pts.first().x, height - paddingBottom)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    TealPrimary.copy(alpha = if (isDark) 0.16f else 0.10f),
                                    Color.Transparent
                                )
                            )
                        )

                        drawPath(
                            path = path,
                            color = TealPrimary,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // 6. 数据圆点与数值
                    for (p in movingPoints) {
                        val dotColor = when {
                            p.value < 3.9f -> if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
                            p.value > 10.0f -> if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)
                            else -> TealPrimary
                        }

                        drawCircle(
                            color = dotColor.copy(alpha = 0.22f * p.alpha),
                            radius = 6.dp.toPx() * p.scale,
                            center = Offset(p.x, p.y)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = p.alpha),
                            radius = 3.5.dp.toPx() * p.scale,
                            center = Offset(p.x, p.y)
                        )
                        drawCircle(
                            color = dotColor.copy(alpha = p.alpha),
                            radius = 2.dp.toPx() * p.scale,
                            center = Offset(p.x, p.y)
                        )

                        if (p.alpha > 0.3f) {
                            val align = when {
                                p.x <= paddingLeft + 6.dp.toPx() -> android.graphics.Paint.Align.LEFT
                                p.x >= width - paddingRight - 6.dp.toPx() -> android.graphics.Paint.Align.RIGHT
                                else -> android.graphics.Paint.Align.CENTER
                            }
                            val labelX = when {
                                p.x <= paddingLeft + 6.dp.toPx() -> p.x + 2.dp.toPx()
                                p.x >= width - paddingRight - 6.dp.toPx() -> p.x - 2.dp.toPx()
                                else -> p.x
                            }
                            val paint = android.graphics.Paint().apply {
                                color = dotColor.copy(alpha = p.alpha).toArgb()
                                textSize = 9.5.sp.toPx() * p.scale
                                textAlign = align
                                isFakeBoldText = true
                                isAntiAlias = true
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                String.format(Locale.US, "%.1f", p.value),
                                labelX,
                                p.y - 6.dp.toPx() * p.scale,
                                paint
                            )
                        }
                    }

                    // 7. 用药气泡 (移至时间段字样正下方，直观对应各时段给药量)
                    val bubbleY = periodY + 12.dp.toPx()
                    for (i in 0 until 7) {
                        val slotTo = toSlots.getOrNull(i)
                        val slotFrom = fromSlots.getOrNull(i)
                        val doseTo = slotTo?.insulinDose
                        val doseFrom = slotFrom?.insulinDose
                        val dose = doseTo ?: doseFrom
                        val doseAlpha = if (doseTo != null && doseFrom != null) 1f
                        else if (doseTo != null) progress
                        else if (doseFrom != null) (1f - progress).coerceIn(0f, 1f)
                        else 0f

                        val cx = getSlotX(i)

                        if (dose != null && dose > 0f && doseAlpha > 0.05f) {
                            val doseText = "💉${if (dose % 1f == 0f) dose.toInt() else dose}U"

                            val paint = android.graphics.Paint().apply {
                                textSize = 8.sp.toPx()
                                isFakeBoldText = true
                                isAntiAlias = true
                            }
                            val textWidth = paint.measureText(doseText)
                            val bubbleWidth = textWidth + 7.dp.toPx()
                            val bubbleHeight = 13.dp.toPx()

                            drawRoundRect(
                                color = (if (isDark) Color(0xFF0F766E) else Color(0xFFCCFBF1)).copy(alpha = doseAlpha),
                                topLeft = Offset(cx - bubbleWidth / 2f, bubbleY - bubbleHeight / 2f),
                                size = Size(bubbleWidth, bubbleHeight),
                                cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                doseText,
                                cx,
                                bubbleY + 2.8.dp.toPx(),
                                paint.apply {
                                    color = (if (isDark) Color.White else Color(0xFF0F766E)).copy(alpha = doseAlpha).toArgb()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }
                    }
                }

                // 底部指标分割线与 5 大指标对齐网格（数字变动动效和周控糖一致）
                HorizontalDivider(
                    color = if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF1F5F9),
                    thickness = 1.dp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DailyStatMetricItem(
                        label = "最高",
                        value = if (animMaxBg > 0.5f) String.format(Locale.US, "%.1f", animMaxBg) else "--",
                        valueColor = if (animMaxBg > 10.0f) (if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    DailyStatMetricItem(
                        label = "最低",
                        value = if (animMinBg > 0.5f) String.format(Locale.US, "%.1f", animMinBg) else "--",
                        valueColor = if (animMinBg < 3.9f && animMinBg > 0f) (if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    DailyStatMetricItem(
                        label = "均值",
                        value = if (animAvgBg > 0.5f) String.format(Locale.US, "%.1f", animAvgBg) else "--",
                        valueColor = TealPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    DailyStatMetricItem(
                        label = "达标率",
                        value = if (currentValidBgs.isNotEmpty()) "${animTirPercent.toInt()}%" else "--",
                        valueColor = if (animTirPercent >= 70f) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    DailyStatMetricItem(
                        label = "用药总量",
                        value = if (animTotalInsulin > 0.1f) "${if (animTotalInsulin % 1f < 0.05f) animTotalInsulin.toInt() else String.format(Locale.US, "%.1f", animTotalInsulin)}U" else "--",
                        valueColor = TealPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


/**
 * 每日走势卡片底部单项统计指标组件（严谨居中与字体边距剔除）
 */
@Composable
private fun DailyStatMetricItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}

/**
 * 总体控糖概况大看板
 */
@Composable
private fun StatsSummaryHeader(allBGs: List<Float>, recordCount: Int) {
    val isDark = isSystemInDarkTheme()
    val meanBG = remember(allBGs) { if (allBGs.isNotEmpty()) allBGs.average().toFloat() else 0f }
    val minBG = remember(allBGs) { allBGs.minOrNull() ?: 0f }
    val maxBG = remember(allBGs) { allBGs.maxOrNull() ?: 0f }

    // TIR 达标率
    val inRangeCount = remember(allBGs) { allBGs.count { it in 3.9f..10.0f } }
    val tirRate = remember(allBGs) { if (allBGs.isNotEmpty()) (inRangeCount * 100f / allBGs.size).toInt() else 0 }

    // 数字变动动画（spring 曲线平滑数字插值，彻底摒弃整卡淡入淡出闪动）
    val animMeanBG by animateFloatAsState(
        targetValue = meanBG,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "anim_header_mean_bg"
    )
    val animMinBG by animateFloatAsState(
        targetValue = minBG,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "anim_header_min_bg"
    )
    val animMaxBG by animateFloatAsState(
        targetValue = maxBG,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "anim_header_max_bg"
    )
    val animTirRate by animateFloatAsState(
        targetValue = tirRate.toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "anim_header_tir_rate"
    )
    val animMeasureCount by animateFloatAsState(
        targetValue = allBGs.size.toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "anim_header_measure_count"
    )
    val animRecordCount by animateFloatAsState(
        targetValue = recordCount.toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "anim_header_record_count"
    )

    val targetBadgeColor = when {
        allBGs.any { it < 3.9f } -> if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
        tirRate >= 70 -> Color(0xFF059669)
        tirRate >= 50 -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488) // 沉稳平和医用青绿：基本达标不刺眼不警示
        else -> if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)
    }
    val badgeText = when {
        allBGs.any { it < 3.9f } -> "低血糖预警"
        tirRate >= 70 -> "控糖理想"
        tirRate >= 50 -> "基本达标"
        else -> "血糖偏高"
    }
    val animBadgeColor by animateColorAsState(
        targetValue = targetBadgeColor,
        animationSpec = tween(220),
        label = "anim_header_badge_color"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppleCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = appleCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "周期控糖概况",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.28).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    Text(
                        text = "(${animMeasureCount.toInt()}次测量 / ${animRecordCount.toInt()} 天)",
                        fontSize = 11.5.sp,
                        letterSpacing = (-0.12).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }

                Surface(
                    shape = ApplePillShape,
                    color = animBadgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.12).sp,
                        color = animBadgeColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }

            HorizontalDivider(color = if (AppThemeColors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryMetricItem(
                    label = "平均血糖",
                    value = String.format(Locale.CHINA, "%.1f", animMeanBG),
                    unit = "mmol/L",
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    label = "TIR 达标率",
                    value = "${animTirRate.toInt()}%",
                    unit = "目标≥70%",
                    valueColor = if (animTirRate >= 70f) Color(0xFF059669) else if (animTirRate >= 50f) (if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)) else (if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    label = "最低血糖",
                    value = String.format(Locale.CHINA, "%.1f", animMinBG),
                    unit = "mmol/L",
                    valueColor = if (animMinBG < 3.9f) (if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    label = "最高血糖",
                    value = String.format(Locale.CHINA, "%.1f", animMaxBG),
                    unit = "mmol/L",
                    valueColor = if (animMaxBG > 13.9f) (if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)) else if (animMaxBG > 10.0f) (if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    label: String,
    value: String,
    unit: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = (-0.12).sp,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            letterSpacing = (-0.28).sp,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
        Text(
            text = unit,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = (-0.1).sp,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}

/**
 * 图表 1: TIR 目标范围内时间占比 (国际控糖金标准环形图)
 */
@Composable
private fun TirDoughnutChartCard(allBGs: List<Float>) {
    val total = allBGs.size.toFloat()
    val lowCount = allBGs.count { it < 3.9f }
    val inRangeCount = allBGs.count { it in 3.9f..10.0f }
    val highCount = allBGs.count { it in 10.1f..13.9f }
    val veryHighCount = allBGs.count { it > 13.9f }

    val lowPct = if (total > 0) lowCount / total else 0f
    val inRangePct = if (total > 0) inRangeCount / total else 0f
    val highPct = if (total > 0) highCount / total else 0f
    val veryHighPct = if (total > 0) veryHighCount / total else 0f

    // 饼形图不做渐入渐出，而是对比例的改变做一个平滑弹簧过渡
    val animLowPct by animateFloatAsState(
        targetValue = lowPct,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
        label = "anim_low_pct"
    )
    val animInRangePct by animateFloatAsState(
        targetValue = inRangePct,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
        label = "anim_in_range_pct"
    )
    val animHighPct by animateFloatAsState(
        targetValue = highPct,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
        label = "anim_high_pct"
    )
    val animVeryHighPct by animateFloatAsState(
        targetValue = veryHighPct,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
        label = "anim_very_high_pct"
    )

    val isDark = AppThemeColors.isDark
    val colorLow = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)      // 纯正警示红: 极低/低血糖风险
    val colorInRange = Color(0xFF059669)  // 翡翠绿: 达标 TIR (3.9~10.0)
    val colorHigh = if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)     // 暖金琥珀: 轻度偏高 (10.1~13.9，向黄色倾斜)
    val colorVeryHigh = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444) // 纯正红: 极高血糖 (>13.9)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppleCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appleCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🎯 TIR 目标范围内时间占比",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.28).sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
                Text(
                    text = "国际控糖金标准",
                    fontSize = 11.5.sp,
                    color = TealPrimary,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.12).sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧: 自定义 Canvas 甜甜圈环形图（比例变化时优雅平滑过渡）
                Box(modifier = Modifier.size(122.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(112.dp)) {
                        val strokeWidth = 18.dp.toPx()
                        val arcSize = size.width - strokeWidth
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        var startAngle = -90f
                        val sweepInRange = animInRangePct * 360f
                        val sweepHigh = animHighPct * 360f
                        val sweepVeryHigh = animVeryHighPct * 360f
                        val sweepLow = animLowPct * 360f

                        // 绘制各区间圆弧
                        if (sweepInRange > 0f) {
                            drawArc(colorInRange, startAngle, sweepInRange, false, topLeft = topLeft, size = Size(arcSize, arcSize), style = Stroke(strokeWidth, cap = StrokeCap.Butt))
                            startAngle += sweepInRange
                        }
                        if (sweepHigh > 0f) {
                            drawArc(colorHigh, startAngle, sweepHigh, false, topLeft = topLeft, size = Size(arcSize, arcSize), style = Stroke(strokeWidth, cap = StrokeCap.Butt))
                            startAngle += sweepHigh
                        }
                        if (sweepVeryHigh > 0f) {
                            drawArc(colorVeryHigh, startAngle, sweepVeryHigh, false, topLeft = topLeft, size = Size(arcSize, arcSize), style = Stroke(strokeWidth, cap = StrokeCap.Butt))
                            startAngle += sweepVeryHigh
                        }
                        if (sweepLow > 0f) {
                            drawArc(colorLow, startAngle, sweepLow, false, topLeft = topLeft, size = Size(arcSize, arcSize), style = Stroke(strokeWidth, cap = StrokeCap.Butt))
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(animInRangePct * 100).toInt()}%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorInRange,
                            letterSpacing = (-0.38).sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                        Text(
                            text = "TIR 达标率",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = (-0.1).sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 右侧: 图例与占比明细
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TirLegendRow(color = colorInRange, label = "目标达标 (3.9~10.0)", pct = animInRangePct, goal = "目标 ≥70%")
                    TirLegendRow(color = colorHigh, label = "轻度偏高 (10.1~13.9)", pct = animHighPct)
                    TirLegendRow(color = colorVeryHigh, label = "显著偏高 (>13.9)", pct = animVeryHighPct)
                    TirLegendRow(color = colorLow, label = "低血糖风险 (<3.9)", pct = animLowPct, goal = "警戒 <4%")
                }
            }

            Surface(
                shape = AppleMdShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = appleCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                    Text(
                        text = "国内外指南推荐：保持 TIR > 70% 且低血糖 < 4% 可显著降低心肾及眼底并发症风险。",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TirLegendRow(color: Color, label: String, pct: Float, goal: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Column {
                Text(
                    text = label,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
                if (goal != null) {
                    Text(
                        text = goal,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
        Text(
            text = "${(pct * 100).toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * 图表 2: 日内 7 点时段血糖波动与趋势模式图 (离散版 AGP)
 */
data class PeriodStat(val name: String, val avg: Float, val min: Float, val max: Float, val count: Int)

@Composable
private fun DailySevenPointAgpChartCard(records: List<InsulinRecord>) {
    val periodStats = remember(records) {
        val fastingList = records.mapNotNull { it.fastingBG }.filter { it > 0 }
        val postBfList = records.flatMap { it.getPostMealList(MealPeriod.MORNING).map { p -> p.value } }.filter { it > 0 }
        val preLunchList = records.mapNotNull { it.preLunchBG }.filter { it > 0 }
        val postLunchList = records.flatMap { it.getPostMealList(MealPeriod.LUNCH).map { p -> p.value } }.filter { it > 0 }
        val preDinnerList = records.mapNotNull { it.preDinnerBG }.filter { it > 0 }
        val postDinnerList = records.flatMap { it.getPostMealList(MealPeriod.DINNER).map { p -> p.value } }.filter { it > 0 }
        val preNightList = records.mapNotNull { it.preNightBG }.filter { it > 0 }

        fun makeStat(name: String, list: List<Float>): PeriodStat? {
            if (list.isEmpty()) return null
            return PeriodStat(
                name = name,
                avg = list.map { it.toDouble() }.average().toFloat(),
                min = list.minOrNull() ?: 0f,
                max = list.maxOrNull() ?: 0f,
                count = list.size
            )
        }

        listOf(
            makeStat("空腹", fastingList),
            makeStat("早后", postBfList),
            makeStat("午餐前", preLunchList),
            makeStat("午餐后", postLunchList),
            makeStat("晚餐前", preDinnerList),
            makeStat("晚餐后", postDinnerList),
            makeStat("睡前", preNightList)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppleCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appleCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 日内各餐段模式图 (AGP)",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.28).sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(5.dp).background(TealPrimary.copy(alpha = 0.5f), CircleShape))
                    Text(text = "极值虚线", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(5.dp).background(TealPrimary, CircleShape))
                    Text(text = "均值曲线", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 自定义 Canvas: 绿色目标安全带 + 贝塞尔平滑波动区间带 + 极值柔和虚线 + 均值平滑曲线
            val activeStats = periodStats.filterNotNull()
            if (activeStats.size >= 2) {
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 24.dp.toPx()
                    val chartHeight = h - bottomPadding
                    val maxScale = 16f
                    val minScale = 2f

                    // 两端留出 24dp 边距，防止“空腹”和“睡前”文本超出屏幕左/右边缘截断
                    val horizontalPadding = 24.dp.toPx()
                    val chartWidth = w - 2 * horizontalPadding

                    fun yFor(bg: Float): Float {
                        val clamped = bg.coerceIn(minScale, maxScale)
                        return chartHeight - ((clamped - minScale) / (maxScale - minScale)) * chartHeight
                    }

                    // 1. 绘制 3.9 ~ 10.0 mmol/L 绿色目标理想区间背景带 (沉稳翡翠绿 #059669)
                    val targetTopY = yFor(10.0f)
                    val targetBottomY = yFor(3.9f)
                    drawRect(
                        color = Color(0xFF059669).copy(alpha = 0.08f),
                        topLeft = Offset(0f, targetTopY),
                        size = Size(w, targetBottomY - targetTopY)
                    )

                    // 绘制 7.0 mmol/L 参考参考虚线
                    val line7Y = yFor(7.0f)
                    drawLine(
                        color = Color(0xFF059669).copy(alpha = 0.25f),
                        start = Offset(0f, line7Y),
                        end = Offset(w, line7Y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    val stepX = chartWidth / (activeStats.size - 1)

                    val maxPoints = activeStats.mapIndexed { i, stat ->
                        Offset(horizontalPadding + i * stepX, yFor(stat.max))
                    }
                    val minPoints = activeStats.mapIndexed { i, stat ->
                        Offset(horizontalPadding + i * stepX, yFor(stat.min))
                    }
                    val meanPoints = activeStats.mapIndexed { i, stat ->
                        Offset(horizontalPadding + i * stepX, yFor(stat.avg))
                    }

                    // 2. 绘制 Min ~ Max 贝塞尔平滑波动范围阴影带 (彻底告别尖锐三角形刺角)
                    val bandPath = Path().apply {
                        moveTo(maxPoints.first().x, maxPoints.first().y)
                        for (i in 0 until maxPoints.size - 1) {
                            val p0 = if (i > 0) maxPoints[i - 1] else maxPoints[i]
                            val p1 = maxPoints[i]
                            val p2 = maxPoints[i + 1]
                            val p3 = if (i + 2 < maxPoints.size) maxPoints[i + 2] else p2
                            val c1 = Offset(p1.x + (p2.x - p0.x) * 0.2f, p1.y + (p2.y - p0.y) * 0.2f)
                            val c2 = Offset(p2.x - (p3.x - p1.x) * 0.2f, p2.y - (p3.y - p1.y) * 0.2f)
                            cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
                        }
                        lineTo(minPoints.last().x, minPoints.last().y)
                        val revMin = minPoints.reversed()
                        for (i in 0 until revMin.size - 1) {
                            val p0 = if (i > 0) revMin[i - 1] else revMin[i]
                            val p1 = revMin[i]
                            val p2 = revMin[i + 1]
                            val p3 = if (i + 2 < revMin.size) revMin[i + 2] else p2
                            val c1 = Offset(p1.x + (p2.x - p0.x) * 0.2f, p1.y + (p2.y - p0.y) * 0.2f)
                            val c2 = Offset(p2.x - (p3.x - p1.x) * 0.2f, p2.y - (p3.y - p1.y) * 0.2f)
                            cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
                        }
                        close()
                    }
                    drawPath(bandPath, color = TealPrimary.copy(alpha = 0.12f))

                    // 3. 绘制 Max / Min 边界柔和虚线（柔和且边界分明）
                    val softDash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    val maxPath = Path().apply { smoothCurveTo(maxPoints) }
                    val minPath = Path().apply { smoothCurveTo(minPoints) }
                    drawPath(
                        path = maxPath,
                        color = TealPrimary.copy(alpha = 0.45f),
                        style = Stroke(width = 1.4.dp.toPx(), pathEffect = softDash, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = minPath,
                        color = TealPrimary.copy(alpha = 0.45f),
                        style = Stroke(width = 1.4.dp.toPx(), pathEffect = softDash, cap = StrokeCap.Round)
                    )

                    // 4. 绘制均值平滑曲线
                    val meanPath = Path().apply { smoothCurveTo(meanPoints) }
                    drawPath(
                        path = meanPath,
                        color = TealPrimary,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 5. 绘制数据节点圆点与下方 X 轴文字
                    val paint = android.graphics.Paint().apply {
                        color = onSurfaceColor.toArgb()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val valuePaint = android.graphics.Paint().apply {
                        color = TealPrimary.toArgb()
                        textSize = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        isAntiAlias = true
                    }

                    activeStats.forEachIndexed { i, stat ->
                        val p = meanPoints[i]

                        // 绘制双层圆点 (外白光晕 + 内绿核心)
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = p)
                        drawCircle(color = TealPrimary, radius = 2.5.dp.toPx(), center = p)

                        // 数值文字
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.CHINA, "%.1f", stat.avg),
                            p.x,
                            p.y - 7.dp.toPx(),
                            valuePaint
                        )

                        // X 轴时段名
                        drawContext.canvas.nativeCanvas.drawText(
                            stat.name,
                            p.x,
                            h - 4.dp.toPx(),
                            paint
                        )
                    }
                }

                // 临床洞察结论
                val maxAvgStat = activeStats.maxByOrNull { it.avg }
                val maxFluctStat = activeStats.maxByOrNull { it.max - it.min }
                Surface(
                    shape = AppleMdShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = appleCardBorder()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "💡 临床分析：${maxAvgStat?.name ?: ""}时段血糖均值最高 (${String.format(Locale.CHINA, "%.1f", maxAvgStat?.avg ?: 0f)} mmol/L)；${maxFluctStat?.name ?: ""}时段波动最大 (${String.format(Locale.CHINA, "%.1f", maxFluctStat?.min ?: 0f)} ~ ${String.format(Locale.CHINA, "%.1f", maxFluctStat?.max ?: 0f)})，建议重点关注该餐饮食与用药时机匹配。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "当前时段记录较少，需记录至少 2 个时段以绘制连贯波动模式图。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

internal fun calculateInsulinAxis(maxObserved: Float, stepCount: Int = 1): Pair<Float, List<Int>> {
    val target = if (maxObserved <= 0f) 16f else maxObserved
    val candidates = listOf(8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 36, 40, 50)
    val chosenStep = candidates.firstOrNull { step ->
        step * stepCount >= target + (if (target >= 25f) 2f else 1f)
    } ?: kotlin.math.ceil((target * 1.08f) / stepCount).toInt()

    val axisMax = (chosenStep * stepCount).toFloat()
    val ticks = (0..stepCount).map { it * chosenStep }
    return axisMax to ticks
}

internal fun calculateBgAxis(observedMax: Float, minBg: Float = 3.0f, stepCount: Int = 4): Pair<Float, List<Float>> {
    val target = max(10.5f, observedMax) // Always ensure TIR 10.0 is accommodated
    val candidates = listOf(1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 5.0f)
    val chosenStep = candidates.firstOrNull { step ->
        minBg + step * stepCount >= target + 0.8f
    } ?: kotlin.math.ceil((target + 0.8f - minBg) / stepCount)

    val maxBg = minBg + chosenStep * stepCount
    val ticks = (0..stepCount).map { minBg + it * chosenStep }
    return maxBg to ticks
}

/**
 * 图表 3: 每日用药量与血糖联动趋势双轴图 (Insulin vs Glucose)
 * - 左轴（mmol/L）：精准呈现每日平均血糖数值与折线走势
 * - 右轴（U）：清晰对应每日胰岛素注射总量柱状图
 * - 数据点与柱顶全量标注具体数值，彻底解决“没有数值看不懂”的问题
 */
@Composable
private fun MedicationAndGlucoseDualAxisCard(records: List<InsulinRecord>) {
    val isDark = AppThemeColors.isDark
    val bgCurveColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)
    val dailyData = remember(records) {
        records.sortedBy { it.date }.takeLast(10).map { r ->
            val bgs = mutableListOf<Float>()
            r.fastingBG?.let { if (it > 0) bgs.add(it) }
            r.preBfBG?.let { if (it > 0) bgs.add(it) }
            r.getPostMealList(MealPeriod.MORNING).forEach { if (it.value > 0) bgs.add(it.value) }
            r.preLunchBG?.let { if (it > 0) bgs.add(it) }
            r.getPostMealList(MealPeriod.LUNCH).forEach { if (it.value > 0) bgs.add(it.value) }
            r.preDinnerBG?.let { if (it > 0) bgs.add(it) }
            r.getPostMealList(MealPeriod.DINNER).forEach { if (it.value > 0) bgs.add(it.value) }
            r.preNightBG?.let { if (it > 0) bgs.add(it) }
            r.getPostMealList(MealPeriod.NIGHT).forEach { if (it.value > 0) bgs.add(it.value) }

            val avgBg = if (bgs.isNotEmpty()) bgs.map { it.toDouble() }.average().toFloat() else null
            val mealInsulin = (r.bfInsulin ?: 0f) + (r.lunchInsulin ?: 0f) + (r.dinnerInsulin ?: 0f)
            val bedtimeInsulin = r.bedtimeInsulin ?: 0f
            Triple(r.date.takeLast(5), avgBg, mealInsulin to bedtimeInsulin)
        }
    }

    val avgDailyInsulin = remember(dailyData) {
        val total = dailyData.sumOf { (it.third.first + it.third.second).toDouble() }
        if (dailyData.isNotEmpty()) total / dailyData.size else 0.0
    }

    val avgDailyBg = remember(dailyData) {
        val validBgs = dailyData.mapNotNull { it.second }
        if (validBgs.isNotEmpty()) validBgs.average().toFloat() else null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppleCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appleCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 顶栏：标题与综合统计标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💉 胰岛素用量与血糖联动趋势",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.28).sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "日均 ${String.format(Locale.CHINA, "%.1f", avgDailyInsulin)}U",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealPrimary,
                        letterSpacing = (-0.12).sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    if (avgDailyBg != null) {
                        Text(
                            text = "· 均糖 ${String.format(Locale.CHINA, "%.1f", avgDailyBg)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = bgCurveColor,
                            letterSpacing = (-0.12).sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }
            }

            // 临床双轴解读指导说明（让用户一眼看懂）
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.55f) else Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("💡", fontSize = 11.sp)
                    Text(
                        text = "双轴对照：折线（左轴/mmol/L）为每日平均血糖，柱状（右轴/U）为胰岛素总量。可直观对比用药量增减对血糖的实际改善效果。",
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (dailyData.isNotEmpty()) {
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                val gridLineColor = if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0)
                val surfaceColor = MaterialTheme.colorScheme.surface

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    val paddingLeft = 32.dp.toPx()
                    val paddingRight = 32.dp.toPx()
                    val paddingTop = 22.dp.toPx()
                    val paddingBottom = 22.dp.toPx()

                    val chartWidth = w - paddingLeft - paddingRight
                    val chartHeight = h - paddingTop - paddingBottom

                    val stepX = chartWidth / dailyData.size.toFloat()
                    val barWidth = (stepX * 0.36f).coerceIn(7.dp.toPx(), 13.dp.toPx())

                    val minBg = 3.0f
                    val observedMaxBg = dailyData.mapNotNull { it.second }.maxOrNull() ?: 10f
                    val (maxBg, bgTicks) = calculateBgAxis(observedMaxBg, minBg = minBg, stepCount = 4)

                    val observedMaxInsulin = dailyData.map { it.third.first + it.third.second }.maxOrNull() ?: 20f
                    val (maxInsulin, insTicks) = calculateInsulinAxis(observedMaxInsulin, stepCount = 1)

                    // 1. 绘制 TIR 3.9 ~ 10.0 mmol/L 绿色理想达标背景带
                    val tirTop = paddingTop + chartHeight * (1f - (10.0f - minBg) / (maxBg - minBg))
                    val tirBottom = paddingTop + chartHeight * (1f - (3.9f - minBg) / (maxBg - minBg))
                    drawRect(
                        color = Color(0xFF059669).copy(alpha = if (isDark) 0.08f else 0.04f),
                        topLeft = Offset(paddingLeft, tirTop),
                        size = Size(chartWidth, (tirBottom - tirTop).coerceAtLeast(0f))
                    )
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    drawLine(
                        color = Color(0xFF059669).copy(alpha = 0.35f),
                        start = Offset(paddingLeft, tirTop),
                        end = Offset(w - paddingRight, tirTop),
                        strokeWidth = 0.8.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = Color(0xFF059669).copy(alpha = 0.35f),
                        start = Offset(paddingLeft, tirBottom),
                        end = Offset(w - paddingRight, tirBottom),
                        strokeWidth = 0.8.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // 2. 绘制 5 档水平网格线，左轴（血糖）与右轴（胰岛素）刻度完全共享同一基准线，对齐极度美观
                    val stepCount = 4
                    val bgTextPaint = android.graphics.Paint().apply {
                        color = bgCurveColor.toArgb()
                        textSize = 8.5.sp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                    val insulinTextPaint = android.graphics.Paint().apply {
                        color = (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)).toArgb()
                        textSize = 8.5.sp.toPx()
                        textAlign = android.graphics.Paint.Align.LEFT
                        isAntiAlias = true
                    }

                    for (i in 0..stepCount) {
                        val fraction = i / stepCount.toFloat()
                        val gy = (h - paddingBottom) - chartHeight * fraction

                        // 水平参考虚线
                        drawLine(
                            color = gridLineColor,
                            start = Offset(paddingLeft, gy),
                            end = Offset(w - paddingRight, gy),
                            strokeWidth = 0.6.dp.toPx(),
                            pathEffect = dashEffect
                        )

                        // 左轴刻度值（血糖 mmol/L）
                        val bgVal = bgTicks.getOrElse(i) { minBg + (maxBg - minBg) * fraction }
                        val bgText = if (bgVal % 1f == 0f) "${bgVal.toInt()}" else String.format(Locale.US, "%.1f", bgVal)
                        drawContext.canvas.nativeCanvas.drawText(
                            bgText,
                            paddingLeft - 4.dp.toPx(),
                            gy + 3.dp.toPx(),
                            bgTextPaint
                        )

                        // 右轴刻度值（胰岛素用量 U）：仅映射在最底部 0..1 档（0%、25%高度）
                        if (i in 0..1) {
                            val insVal = insTicks.getOrElse(i) { 0 }
                            drawContext.canvas.nativeCanvas.drawText(
                                "$insVal",
                                w - paddingRight + 4.dp.toPx(),
                                gy + 3.dp.toPx(),
                                insulinTextPaint
                            )
                        }
                    }

                    // 左轴单位 (mmol/L) - 置于顶端
                    drawContext.canvas.nativeCanvas.drawText(
                        "mmol/L",
                        paddingLeft - 2.dp.toPx(),
                        paddingTop - 8.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = bgCurveColor.toArgb()
                            textSize = 8.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )

                    // 右轴单位 (用药(U)) - 置于第 1 档（25%上限）上方，清晰标明用药量程对应底部 25% 空间
                    val gyFirstLine = (h - paddingBottom) - chartHeight * 0.25f
                    drawContext.canvas.nativeCanvas.drawText(
                        "用药(U)",
                        w - paddingRight + 4.dp.toPx(),
                        gyFirstLine - 7.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)).toArgb()
                            textSize = 8.sp.toPx()
                            textAlign = android.graphics.Paint.Align.LEFT
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )

                    val baseY = h - paddingBottom
                    // 柱状图最高仅占图表底部 25% 高度，小巧精致，绝不冲顶穿过折线
                    val maxBarHeight = chartHeight * 0.25f

                    // 4. 绘制胰岛素用量柱状图 + 顶部具体数值标注（清楚看到用了多少药）
                    dailyData.forEachIndexed { i, item ->
                        val centerX = paddingLeft + (i + 0.5f) * stepX
                        val (mealU, bedU) = item.third
                        val totalU = mealU + bedU

                        val mealH = if (maxInsulin > 0) (mealU / maxInsulin) * maxBarHeight else 0f
                        val bedH = if (maxInsulin > 0) (bedU / maxInsulin) * maxBarHeight else 0f
                        val totalH = mealH + bedH

                        // 餐时胰岛素柱（湖蓝主色，质感饱满）
                        if (mealH > 0) {
                            drawRoundRect(
                                color = TealPrimary.copy(alpha = if (isDark) 0.82f else 0.72f),
                                topLeft = Offset(centerX - barWidth / 2, baseY - mealH),
                                size = Size(barWidth, mealH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                        // 睡前长效胰岛素柱（宝蓝辅色）
                        if (bedH > 0) {
                            drawRoundRect(
                                color = Color(0xFF1E88E5).copy(alpha = if (isDark) 0.82f else 0.72f),
                                topLeft = Offset(centerX - barWidth / 2, baseY - mealH - bedH),
                                size = Size(barWidth, bedH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }

                        // 柱顶总量数值标注（置于柱顶上方，清晰通透，绝不与上方血糖折线冲突）
                        if (totalU > 0) {
                            val doseText = if (totalU % 1f == 0f) "${totalU.toInt()}U" else String.format(Locale.US, "%.1fU", totalU)
                            val textY = baseY - totalH - 3.dp.toPx()
                            val textColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            drawContext.canvas.nativeCanvas.drawText(
                                doseText,
                                centerX,
                                textY,
                                android.graphics.Paint().apply {
                                    color = textColor.toArgb()
                                    textSize = 7.8.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                    isAntiAlias = true
                                }
                            )
                        }
                    }

                    // 5. 绘制上半部平均血糖折线（平滑贝塞尔曲线）
                    val bgPoints = mutableListOf<Offset>()
                    dailyData.forEachIndexed { i, item ->
                        val centerX = paddingLeft + (i + 0.5f) * stepX
                        val bg = item.second
                        if (bg != null) {
                            val y = paddingTop + chartHeight * (1f - (bg.coerceIn(minBg, maxBg) - minBg) / (maxBg - minBg))
                            bgPoints.add(Offset(centerX, y))
                        }
                    }

                    if (bgPoints.size > 1) {
                        val bgPath = Path().apply { smoothCurveTo(bgPoints) }
                        drawPath(
                            path = bgPath,
                            color = bgCurveColor,
                            style = Stroke(
                                width = 2.4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // 6. 绘制血糖节点与节点上方具体数值标注（让折线每一点都有精准数值）
                    val textPaint = android.graphics.Paint().apply {
                        color = onSurfaceVariant.toArgb()
                        textSize = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    dailyData.forEachIndexed { i, item ->
                        val centerX = paddingLeft + (i + 0.5f) * stepX
                        val bg = item.second
                        if (bg != null) {
                            val y = paddingTop + chartHeight * (1f - (bg.coerceIn(minBg, maxBg) - minBg) / (maxBg - minBg))

                            // 节点圆圈（带白色遮罩确保清晰）
                            drawCircle(color = surfaceColor, radius = 4.2.dp.toPx(), center = Offset(centerX, y))
                            drawCircle(color = bgCurveColor, radius = 2.4.dp.toPx(), center = Offset(centerX, y))

                            // 节点上方精准均糖数值（如 8.9 / 7.2）
                            val bgValStr = String.format(Locale.US, "%.1f", bg)
                            drawContext.canvas.nativeCanvas.drawText(
                                bgValStr,
                                centerX,
                                y - 6.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = bgCurveColor.toArgb()
                                    textSize = 8.8.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                    isAntiAlias = true
                                }
                            )
                        }

                        // X 轴日期标注
                        drawContext.canvas.nativeCanvas.drawText(
                            item.first,
                            centerX,
                            h - 3.dp.toPx(),
                            textPaint
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = bgCurveColor, label = "平均血糖 (左轴/mmol/L)", isLine = true)
                    Spacer(modifier = Modifier.width(12.dp))
                    LegendItem(color = TealPrimary, label = "餐时针 (右轴/U)", isBar = true)
                    Spacer(modifier = Modifier.width(12.dp))
                    LegendItem(color = Color(0xFF1E88E5), label = "基础针 (右轴/U)", isBar = true)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isDashed: Boolean = false,
    isLine: Boolean = false,
    isBar: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isDashed) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 3.dp)
                    .background(color, RoundedCornerShape(1.5.dp))
            )
        } else if (isBar) {
            Box(
                modifier = Modifier
                    .size(width = 7.dp, height = 7.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        } else if (isLine) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 2.5.dp)
                    .background(color, RoundedCornerShape(1.dp))
            )
        } else {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * 图表 4: 早中晚三餐餐后血糖漂移差值图 (PPGE)
 */
@Composable
private fun PostprandialExcursionChartCard(records: List<InsulinRecord>) {
    val (bfDiff, lunchDiff, dinnerDiff) = remember(records) {
        val bf = records.mapNotNull { r ->
            val pre = r.fastingBG ?: r.preBfBG
            val post = r.getPostMealList(MealPeriod.MORNING).firstOrNull()?.value
            if (pre != null && post != null && pre > 0 && post > 0) {
                post - pre
            } else null
        }
        val lunch = records.mapNotNull { r ->
            val pre = r.preLunchBG ?: r.fastingBG
            val post = r.getPostMealList(MealPeriod.LUNCH).firstOrNull()?.value
            if (post != null && pre != null && post > 0 && pre > 0) post - pre else null
        }
        val dinner = records.mapNotNull { r ->
            val pre = r.preDinnerBG
            val post = r.getPostMealList(MealPeriod.DINNER).firstOrNull()?.value
            if (post != null && pre != null && post > 0 && pre > 0) post - pre else null
        }

        Triple(
            if (bf.isNotEmpty()) bf.map { it.toDouble() }.average().toFloat() else null,
            if (lunch.isNotEmpty()) lunch.map { it.toDouble() }.average().toFloat() else null,
            if (dinner.isNotEmpty()) dinner.map { it.toDouble() }.average().toFloat() else null
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppleCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appleCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🍽️ 餐后血糖漂移幅度 (PPGE)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.28).sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                Text(
                    text = "餐后减餐前增量",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = (-0.12).sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExcursionBarItem(title = "早餐后升幅", diff = bfDiff)
                ExcursionBarItem(title = "午餐后升幅", diff = lunchDiff)
                ExcursionBarItem(title = "晚餐后升幅", diff = dinnerDiff)
            }

            Surface(
                shape = AppleMdShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = appleCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                    Text(
                        text = "临床理想餐后升幅 ≤ 2.2 ~ 3.0 mmol/L。若升幅过大，提示当餐碳水比例偏高或餐前药物用药过迟。",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExcursionBarItem(title: String, diff: Float?) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.5.sp,
            modifier = Modifier.width(72.dp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            letterSpacing = (-0.12).sp,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(ApplePillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            if (diff != null) {
                val clamped = diff.coerceIn(0f, 6.0f)
                val fraction = clamped / 6.0f
                val color = when {
                    diff <= 2.5f -> Color(0xFF059669)
                    diff <= 4.0f -> if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)
                    else -> if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(14.dp)
                        .clip(ApplePillShape)
                        .background(color)
                )
            }
        }

        Text(
            text = if (diff != null) String.format(Locale.CHINA, "+%.1f", diff) else "--",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (diff != null && diff > 3.0f) (if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)) else TealPrimary,
            modifier = Modifier.width(48.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * 图表 5: 低血糖安全监控与时段预警卡片
 */
@Composable
private fun HypoglycemiaSafetyAlertCard(records: List<InsulinRecord>) {
    val isDark = isSystemInDarkTheme()
    val alertRed = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
    val lowIncidents = remember(records) {
        val list = mutableListOf<String>()
        records.forEach { r ->
            r.fastingBG?.let { if (it in 0.1f..<3.9f) list.add("${r.date.takeLast(5)} 晨起空腹 (${it} mmol/L)") }
            r.getPostMealList(MealPeriod.MORNING).forEach { entry ->
                if (entry.value in 0.1f..<3.9f) list.add("${r.date.takeLast(5)} 早餐后 (${entry.value} mmol/L)")
            }
            r.preLunchBG?.let { if (it in 0.1f..<3.9f) list.add("${r.date.takeLast(5)} 午餐前 (${it} mmol/L)") }
            r.getPostMealList(MealPeriod.LUNCH).forEach { entry ->
                if (entry.value in 0.1f..<3.9f) list.add("${r.date.takeLast(5)} 午餐后 (${entry.value} mmol/L)")
            }
            r.preDinnerBG?.let { if (it in 0.1f..<3.9f) list.add("${r.date.takeLast(5)} 晚餐前 (${it} mmol/L)") }
            r.getPostMealList(MealPeriod.DINNER).forEach { entry ->
                if (entry.value in 0.1f..<3.9f) list.add("${r.date.takeLast(5)} 晚餐后 (${entry.value} mmol/L)")
            }
            r.preNightBG?.let { if (it in 0.1f..<3.9f) list.add("${r.date.takeLast(5)} 睡前/夜间 (${it} mmol/L)") }
            r.getPostMealList(MealPeriod.NIGHT).forEach { entry ->
                if (entry.value in 0.1f..<3.9f) list.add("${r.date.takeLast(5)} 夜间加测 (${entry.value} mmol/L)")
            }
        }
        list
    }

    val isAlert = lowIncidents.isNotEmpty()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppleCardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) alertRed.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isAlert) BorderStroke(1.dp, alertRed.copy(alpha = 0.3f)) else appleCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (lowIncidents.isEmpty()) Icons.Default.Security else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (lowIncidents.isEmpty()) Color(0xFF059669) else alertRed,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "低血糖防线监控",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lowIncidents.isEmpty()) MaterialTheme.colorScheme.onSurface else alertRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.28).sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    Text(
                        text = "(<3.9)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        letterSpacing = (-0.12).sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }

                Surface(
                    shape = ApplePillShape,
                    color = if (lowIncidents.isEmpty()) Color(0xFF059669).copy(alpha = 0.12f) else alertRed.copy(alpha = 0.15f),
                    modifier = Modifier.wrapContentSize()
                ) {
                    Text(
                        text = if (lowIncidents.isEmpty()) "安全达标" else "${lowIncidents.size}次风险",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lowIncidents.isEmpty()) Color(0xFF059669) else alertRed,
                        maxLines = 1,
                        softWrap = false,
                        letterSpacing = (-0.12).sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }

            if (lowIncidents.isEmpty()) {
                Text(
                    text = "🛡️ 本周期内未监测到低血糖事件，用药及作息安全性极佳，请继续保持！",
                    fontSize = 11.5.sp,
                    color = Color(0xFF059669),
                    letterSpacing = (-0.12).sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "⚠️ 警惕低血糖风险！重点记录时段分布：",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = alertRed,
                        letterSpacing = (-0.12).sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    lowIncidents.take(4).forEach { item ->
                        Text(
                            text = "• $item",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.12).sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }

                Text(
                    text = "温馨提示：夜间及清晨低血糖可能无意识休克，睡前血糖若低于 6.0 mmol/L 建议适当进食苏打饼干，并在医生指导下调整降糖方案。",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = (-0.1).sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }
        }
    }
}

/**
 * 暂无数据空状态卡片
 */
@Composable
private fun EmptyStatsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppleCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appleCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "📊", fontSize = 40.sp)
            Text(
                text = "所选周期内暂无血糖记录",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.28).sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
            Text(
                text = "请点击右下角“+”或通过语音记录血糖与用药后，即可生成全方位深度统计图表。",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = (-0.12).sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
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
