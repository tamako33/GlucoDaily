package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import com.example.ui.ViewMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BGUtils
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.ui.ItemType
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

/**
 * 视图模式无缝滑动分段选择器（卡片 vs 统计 vs 表格）
 * 严格遵循 Apple 设计标准：全胶囊轨槽、平滑 spring 物理滑块与 applePressEffect
 */
@Composable
fun ViewModeSegmentedControl(
    currentMode: ViewMode,
    onModeChanged: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = remember {
        listOf(
            Triple(ViewMode.CARDS, Icons.AutoMirrored.Filled.List, "卡片"),
            Triple(ViewMode.STATS, Icons.Default.BarChart, "统计"),
            Triple(ViewMode.TABLE, Icons.Default.TableChart, "表格")
        )
    }
    val selectedIndex = modes.indexOfFirst { it.first == currentMode }.coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tab_indicator_slider"
    )

    val isDark = AppThemeColors.isDark
    val trackBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA).copy(alpha = 0.6f)
    val trackBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)

    BoxWithConstraints(
        modifier = modifier
            .width(192.dp)
            .height(32.dp)
            .clip(AppleSegmentTrackShape)
            .background(trackBg)
            .border(BorderStroke(1.dp, trackBorderColor), AppleSegmentTrackShape)
            .padding(2.5.dp)
    ) {
        val tabWidth = maxWidth / modes.size

        // 滑动的指示高亮背景（Teal 主题色块，与全局分段控制器圆角 100% 统一）
        Box(
            modifier = Modifier
                .offset(x = tabWidth * animatedIndex)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(AppleSegmentThumbShape)
                .background(TealPrimary)
        )

        // 三个选项卡的点击项与文字图标
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEachIndexed { index, (mode, icon, label) ->
                val isSelected = mode == currentMode
                val animTextColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "text_color_$index"
                )
                val animIconTint by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    animationSpec = tween(200),
                    label = "icon_color_$index"
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
                        ) { onModeChanged(mode) }
                        .testTag("view_toggle_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = animIconTint,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            letterSpacing = (-0.224).sp,
                            color = animTextColor,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * 视图模式切换小按钮（卡片 vs 表格）
 */
@Composable
fun ViewToggleButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .clip(ApplePillShape)
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .applePressEffect(0.95f)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.5.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = (-0.12).sp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * 无记录数据时的空状态展示与快捷引导按钮
 */
@Composable
fun EmptyRecordsView(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AppThemeColors.breakfastBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "暂无记录数据",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.28).sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "点击上方“记一笔”或悬浮按钮随时记录今天数据",
            style = MaterialTheme.typography.bodySmall,
            letterSpacing = (-0.224).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            shape = ApplePillShape,
            modifier = Modifier.applePressEffect(0.95f)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("添加第一笔记录", fontWeight = FontWeight.SemiBold, letterSpacing = (-0.224).sp)
        }
    }
}

/**
 * 日期卡片翻页组件 (DailyCardPager)：
 * - 顶部翻页导航栏：包含左翻页按键、日历图标、年月日显示、周几、当天/回今天按钮、右翻页按键。
 * - 卡片主体：HorizontalPager 实现全天记录滑动，配备丝滑缩放与呼吸过渡动画。
 * - 支持外部流式精准定位到特定日期 (scrollToDateFlow)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCardPager(
    records: List<InsulinRecord>,
    allRecords: List<InsulinRecord>,
    todayStr: String,
    scrollToDateFlow: SharedFlow<String>?,
    onDateChanged: ((String) -> Unit)? = null,
    onShowMessage: (String) -> Unit,
    onEdit: (InsulinRecord) -> Unit,
    onDelete: (InsulinRecord) -> Unit,
    onEditPeriod: ((InsulinRecord, MealPeriod) -> Unit)? = null,
    onAddItem: ((InsulinRecord, MealPeriod, ItemType?, Int?) -> Unit)? = null,
    onDeletePostMeal: ((InsulinRecord, MealPeriod, Int) -> Unit)? = null,
    onDeleteSingleItem: ((InsulinRecord, MealPeriod, ItemType, Int?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    // 按日期从小到大排序（时间轴顺序：左侧为较早过往，右侧为最新/今天）。支持左右翻看全部记录，无任何天数限制
    val chronologicalRecords = remember(records) { records.sortedBy { it.date } }
    val pageCount = chronologicalRecords.size

    val defaultTargetPage = remember(chronologicalRecords, todayStr) {
        val todayIdx = chronologicalRecords.indexOfFirst { it.date == todayStr }
        if (todayIdx >= 0) todayIdx else (pageCount - 1).coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = defaultTargetPage,
        pageCount = { pageCount }
    )

    var hasScrolledToToday by remember { mutableStateOf(false) }
    LaunchedEffect(chronologicalRecords, todayStr) {
        if (chronologicalRecords.isNotEmpty()) {
            val todayIdx = chronologicalRecords.indexOfFirst { it.date == todayStr }
            if (todayIdx >= 0 && !hasScrolledToToday) {
                pagerState.scrollToPage(todayIdx)
                hasScrolledToToday = true
            } else if (!hasScrolledToToday) {
                val targetIdx = (chronologicalRecords.size - 1).coerceAtLeast(0)
                pagerState.scrollToPage(targetIdx)
            }
        }
    }

    if (scrollToDateFlow != null) {
        LaunchedEffect(scrollToDateFlow, chronologicalRecords) {
            scrollToDateFlow.collect { date ->
                val targetIdx = chronologicalRecords.indexOfFirst { it.date == date }
                if (targetIdx >= 0) {
                    pagerState.animateScrollToPage(targetIdx)
                }
            }
        }
    }

    val currentPage = pagerState.currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val currentRecord = chronologicalRecords.getOrNull(currentPage)
    val hasPrev = currentPage > 0
    val hasNext = currentPage < pageCount - 1

    // 联动外部走势图：当卡片翻页时通知当前卡片对应的日期
    LaunchedEffect(currentRecord?.date) {
        currentRecord?.date?.let { date ->
            onDateChanged?.invoke(date)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    // 点击日历按钮弹出的交互式日历视图，用户可以点击日期精准跳转
    if (showDatePicker) {
        val initialEpochMillis = remember(currentRecord?.date) {
            try {
                val d = currentRecord?.date ?: todayStr
                LocalDate.parse(d).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocalDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            val chosenDate = selectedLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val targetIdx = chronologicalRecords.indexOfFirst { it.date == chosenDate }
                            if (targetIdx >= 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetIdx)
                                }
                            } else {
                                val nearestIdx = findNearestDateIndex(chronologicalRecords, chosenDate)
                                if (nearestIdx >= 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(nearestIdx)
                                    }
                                }
                                onShowMessage("${chosenDate} 暂无记录，已为您定位至相近日期")
                            }
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = ApplePillShape,
                    modifier = Modifier
                        .applePressEffect(0.95f)
                        .testTag("date_picker_confirm_button")
                ) {
                    Text("查看", fontWeight = FontWeight.SemiBold, letterSpacing = (-0.224).sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier
                        .applePressEffect(0.95f)
                        .testTag("date_picker_cancel_button")
                ) {
                    Text("取消", letterSpacing = (-0.224).sp)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "选择跳转日期",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.224).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    )
                },
                headline = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    val headlineText = if (selectedMillis != null) {
                        val d = Instant.ofEpochMilli(selectedMillis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        val dayOfWeekStr = when (d.dayOfWeek) {
                            java.time.DayOfWeek.MONDAY -> "周一"
                            java.time.DayOfWeek.TUESDAY -> "周二"
                            java.time.DayOfWeek.WEDNESDAY -> "周三"
                            java.time.DayOfWeek.THURSDAY -> "周四"
                            java.time.DayOfWeek.FRIDAY -> "周五"
                            java.time.DayOfWeek.SATURDAY -> "周六"
                            java.time.DayOfWeek.SUNDAY -> "周日"
                            null -> ""
                        }
                        "${d.year}年${d.monthValue}月${d.dayOfMonth}日 $dayOfWeekStr"
                    } else {
                        "请选择日期"
                    }
                    Text(
                        text = headlineText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.28).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)
                    )
                },
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = TealPrimary,
                    todayDateBorderColor = TealPrimary
                )
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 顶部日期翻页导航栏：遵循 Apple 极简纯白卡片与发丝边框，空间舒展大方
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppleCardShape,
            color = MaterialTheme.colorScheme.surface,
            border = appleCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 左翻页（圆润三角形向左，搭载 Apple 签名微交互）
                RoundedTriangleButton(
                    direction = TriangleDirection.LEFT,
                    enabled = hasPrev,
                    onClick = {
                        if (hasPrev) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    currentPage - 1,
                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    },
                    modifier = Modifier.testTag("pager_btn_prev")
                )

                // 中间信息组：图标，日期年月日形式，周几，当天/回今天按钮
                val dateDisplay = currentRecord?.date ?: todayStr
                val ymdDisplay = remember(dateDisplay) { formatChineseYMD(dateDisplay) }
                val weekdayDisplay = currentRecord?.weekdayText ?: getWeekdayString(dateDisplay)
                val isCurrentDateToday = dateDisplay == todayStr

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    // 2. 图标（日历图标，点击弹出日历）
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(ApplePillShape)
                            .background(TealPrimary.copy(alpha = 0.12f))
                            .applePressEffect(0.92f)
                            .clickable { showDatePicker = true }
                            .testTag("pager_calendar_icon"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "打开日历查看",
                            tint = TealPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // 3. 日期年月日形式（如：2026年9月15日，点击亦可弹出日历）
                    Text(
                        text = ymdDisplay,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.28).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        modifier = Modifier
                            .clickable { showDatePicker = true }
                            .testTag("pager_date_text")
                    )

                    // 4. 周几（如：周二）
                    if (weekdayDisplay.isNotBlank()) {
                        Text(
                            text = weekdayDisplay,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = (-0.12).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }

                    // 5. 当天和回今天按钮（Apple Pill 语法）
                    if (isCurrentDateToday) {
                        // 是当天：显示“当天”药丸标签
                        Box(
                            modifier = Modifier
                                .clip(ApplePillShape)
                                .background(TealPrimary)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("pager_today_badge")
                        ) {
                            Text(
                                text = "当天",
                                fontSize = 10.5.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.12).sp,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    } else {
                        // 不是当天：显示“回今天”按钮，点击平滑滚动回到当天
                        Surface(
                            shape = ApplePillShape,
                            color = TealPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .applePressEffect(0.94f)
                                .clickable {
                                    val todayIdx = chronologicalRecords.indexOfFirst { it.date == todayStr }
                                    val target = if (todayIdx >= 0) todayIdx else (pageCount - 1).coerceAtLeast(0)
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            target,
                                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                        )
                                    }
                                }
                                .testTag("pager_back_to_today_button")
                        ) {
                            Text(
                                text = "回今天",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.12).sp,
                                color = TealPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // 6. 右翻页（圆润三角形向右，搭载 Apple 签名微交互）
                RoundedTriangleButton(
                    direction = TriangleDirection.RIGHT,
                    enabled = hasNext,
                    onClick = {
                        if (hasNext) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    currentPage + 1,
                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    },
                    modifier = Modifier.testTag("pager_btn_next")
                )
            }
        }

        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val minPagerHeight = remember(screenHeight) { (screenHeight - 220.dp).coerceAtLeast(520.dp) }

        // 单天卡片 HorizontalPager：带流畅过渡动画的左右翻页，空白区域全屏覆盖支持横向滑动翻页
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minPagerHeight)
                .testTag("daily_records_pager"),
            verticalAlignment = Alignment.Top
        ) { page ->
            val record = chronologicalRecords[page]
            val prevNight = remember(record.date, allRecords) {
                BGUtils.getPrevNightInsulin(record.date, allRecords)
            }
            // 流畅丝滑的翻页缩放与透明度呼吸过渡动效
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = minPagerHeight)
                    .graphicsLayer {
                        val clampedOffset = pageOffset.coerceIn(0f, 1f)
                        val scale = 0.93f + 0.07f * (1f - clampedOffset)
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.45f + 0.55f * (1f - clampedOffset)
                    }
            ) {
                RecordCard(
                    record = record,
                    prevNightInfo = prevNight,
                    isToday = record.date == todayStr,
                    onEdit = { onEdit(record) },
                    onDelete = { onDelete(record) },
                    onEditPeriod = { period -> onEditPeriod?.invoke(record, period) ?: onEdit(record) },
                    onAddItem = { period, itemType, postMealIdx -> onAddItem?.invoke(record, period, itemType, postMealIdx) },
                    onDeletePostMeal = { period, idx -> onDeletePostMeal?.invoke(record, period, idx) },
                    onDeleteSingleItem = { period, itemType, idx -> onDeleteSingleItem?.invoke(record, period, itemType, idx) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private enum class TriangleDirection { LEFT, RIGHT }

/**
 * 具有圆润边角的三角形翻页按钮
 */
@Composable
private fun RoundedTriangleButton(
    direction: TriangleDirection,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = TealPrimary
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val color = if (enabled) activeColor else disabledColor

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (AppThemeColors.isDark) Color(0xFF2C2C2E) else Color(0xFFF5F5F7))
            .applePressEffect(0.92f)
    ) {
        Canvas(modifier = Modifier.size(15.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                val r = 2.2f.dp.toPx()
                if (direction == TriangleDirection.LEFT) {
                    val tipX = 1f.dp.toPx()
                    val baseX = w - 1f.dp.toPx()
                    val topY = 1f.dp.toPx()
                    val bottomY = h - 1f.dp.toPx()
                    val midY = h / 2f

                    moveTo(tipX + r * 1.5f, midY - r * 0.8f)
                    quadraticBezierTo(tipX, midY, tipX + r * 1.5f, midY + r * 0.8f)
                    lineTo(baseX - r, bottomY)
                    quadraticBezierTo(baseX, bottomY, baseX, bottomY - r * 1.2f)
                    lineTo(baseX, topY + r * 1.2f)
                    quadraticBezierTo(baseX, topY, baseX - r, topY)
                    close()
                } else {
                    val tipX = w - 1f.dp.toPx()
                    val baseX = 1f.dp.toPx()
                    val topY = 1f.dp.toPx()
                    val bottomY = h - 1f.dp.toPx()
                    val midY = h / 2f

                    moveTo(tipX - r * 1.5f, midY - r * 0.8f)
                    quadraticBezierTo(tipX, midY, tipX - r * 1.5f, midY + r * 0.8f)
                    lineTo(baseX + r, bottomY)
                    quadraticBezierTo(baseX, bottomY, baseX, bottomY - r * 1.2f)
                    lineTo(baseX, topY + r * 1.2f)
                    quadraticBezierTo(baseX, topY, baseX + r, topY)
                    close()
                }
            }
            drawPath(path = path, color = color)
        }
    }
}

/**
 * 将 yyyy-MM-dd 格式化为 中文年月日（如 2026年9月15日）
 */
private fun formatChineseYMD(dateStr: String): String {
    return try {
        val localDate = LocalDate.parse(dateStr)
        "${localDate.year}年${localDate.monthValue}月${localDate.dayOfMonth}日"
    } catch (_: Exception) {
        dateStr
    }
}

private fun getWeekdayString(dateStr: String): String {
    return try {
        when (LocalDate.parse(dateStr).dayOfWeek.value) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> ""
        }
    } catch (_: Exception) {
        ""
    }
}

private fun findNearestDateIndex(records: List<InsulinRecord>, targetDate: String): Int {
    if (records.isEmpty()) return -1
    val targetDay = try {
        LocalDate.parse(targetDate).toEpochDay()
    } catch (_: Exception) {
        return 0
    }
    return records.indices.minByOrNull { idx ->
        val day = try {
            LocalDate.parse(records[idx].date).toEpochDay()
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
        kotlin.math.abs(day - targetDay)
    } ?: 0
}
