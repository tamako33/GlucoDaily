package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BGUtils
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.data.SystemTtsManager
import com.example.ui.CareFontSize
import com.example.ui.TableDateRange
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TealPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 关怀模式字号全局 CompositionLocal
 */
val LocalCareFontSize = compositionLocalOf { CareFontSize.EXTRA }

/**
 * 根据关怀模式所选字号倍率自适应放大 sp
 */
@Composable
fun careSp(baseSp: Float): TextUnit = (baseSp * LocalCareFontSize.current.scaleFactor).sp

/**
 * 关怀模式主视图子选项卡：
 * DASHBOARD: 每日健康红绿灯看板 + 四餐待办大卡片
 * TABLE: 放大对比表格（与外部表格统一，功能完全一致）
 */
enum class CareViewTab(val title: String, val icon: String) {
    DASHBOARD("看板", "📋"),
    TABLE("表格", "📊")
}

/**
 * 关怀模式专属适老化主界面 (CareHomeView)：
 * 1. 字体大号自由切换：支持「标准大字」、「特大字」、「超大字」三档无缝切换，界面全自适应抗截断；
 * 2. 状态看板一目了然：红绿灯大卡片直观呈现健康评分与大白话健康建议，自带 0KB 原生语音慢速播报；
 * 3. 药盒式四餐大卡片：仅展示实际记录条目，第一行项目名+时间+标签，第二行超大字数字；
 * 4. 表格直接复用外部放大表格 UI：支持近7天/14天/30天/全部筛选，具备全量时段、用药、饮食、运动与编辑删除功能。
 */
@Composable
fun CareHomeView(
    allRecords: List<InsulinRecord>,
    selectedDate: String,
    onDateChanged: (String) -> Unit,
    todayStr: String,
    tableDateRange: TableDateRange,
    onTableDateRangeChanged: (TableDateRange) -> Unit,
    onEditRecord: (InsulinRecord) -> Unit,
    onDeleteRecord: (InsulinRecord) -> Unit,
    onOpenCareRecordDialog: (MealPeriod, CareRecordType) -> Unit,
    onOpenVoiceRecord: () -> Unit,
    onExitCareMode: () -> Unit,
    careFontSize: CareFontSize,
    onCareFontSizeChanged: (CareFontSize) -> Unit,
    onToggleTheme: () -> Unit = {},
    onOpenLandscapeTable: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalCareFontSize provides careFontSize) {
        val context = LocalContext.current
        val isDark = LocalIsDarkTheme.current
        val haptic = LocalHapticFeedback.current
        val isSpeaking by SystemTtsManager.isSpeaking.collectAsStateWithLifecycle()

        var activeTab by remember { mutableStateOf(CareViewTab.DASHBOARD) }

        // 获取当前选中日期的记录
        val currentRecord = remember(allRecords, selectedDate) {
            allRecords.find { it.date == selectedDate }
        }

        val statusBarInsets = WindowInsets.statusBars.asPaddingValues()

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. 关怀模式顶部常驻顶栏（集成看板/表格切换、字号选择、夜间白天模式、退出）
            CareTopBar(
                currentTab = activeTab,
                onTabSelected = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeTab = it
                },
                onExitCareMode = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    SystemTtsManager.stop()
                    onExitCareMode()
                },
                onToggleTheme = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleTheme()
                },
                careFontSize = careFontSize,
                onCareFontSizeChanged = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCareFontSizeChanged(it)
                },
                isDark = isDark,
                paddingTop = statusBarInsets.calculateTopPadding()
            )

            // 2. 主体内容根据选项卡分发展示
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    CareViewTab.DASHBOARD -> {
                        // 看板模式：日期翻页 + 红绿灯看板 + 四餐生活化药盒卡
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 限制无法向未来天数翻页
                            val canGoNextDay = remember(selectedDate, todayStr) {
                                runCatching {
                                    val cur = LocalDate.parse(selectedDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                    val today = LocalDate.parse(todayStr, DateTimeFormatter.ISO_LOCAL_DATE)
                                    cur.isBefore(today)
                                }.getOrDefault(false)
                            }

                            CareDateNavigator(
                                currentDate = selectedDate,
                                canGoNext = canGoNextDay,
                                onPrevDay = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val prev = runCatching {
                                        val d = LocalDate.parse(selectedDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                        d.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                                    }.getOrDefault(selectedDate)
                                    onDateChanged(prev)
                                },
                                onNextDay = {
                                    if (!canGoNextDay) return@CareDateNavigator
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val next = runCatching {
                                        val d = LocalDate.parse(selectedDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                        val today = LocalDate.parse(todayStr, DateTimeFormatter.ISO_LOCAL_DATE)
                                        if (d.isBefore(today)) {
                                            d.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        } else selectedDate
                                    }.getOrDefault(selectedDate)
                                    onDateChanged(next)
                                }
                            )

                            val activePeriods = remember(currentRecord) {
                                if (currentRecord == null) emptyList()
                                else buildList {
                                    if (currentRecord.hasMorningData) add(MealPeriod.MORNING)
                                    if (currentRecord.hasLunchData) add(MealPeriod.LUNCH)
                                    if (currentRecord.hasDinnerData) add(MealPeriod.DINNER)
                                    if (currentRecord.hasNightData) add(MealPeriod.NIGHT)
                                }
                            }
                            val hasAnyData = activePeriods.isNotEmpty()

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                if (!hasAnyData) {
                                    // 暂无记录时的单个大气清爽空状态卡片（带4餐快捷大按钮）
                                    item {
                                        CareEmptyDayHeroCard(
                                            selectedDate = selectedDate,
                                            onOpenRecord = { p, type -> onOpenCareRecordDialog(p, type) }
                                        )
                                    }
                                } else {
                                    // A. 健康状态看板（展示当日最新血糖状态与精练叮嘱）
                                    item {
                                        CareStatusHeroCard(
                                            record = currentRecord,
                                            selectedDate = selectedDate,
                                            isDark = isDark,
                                            isSpeaking = isSpeaking,
                                            onToggleSpeak = { textToSpeak ->
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (isSpeaking) {
                                                    SystemTtsManager.stop()
                                                } else {
                                                    val ok = SystemTtsManager.speak(textToSpeak)
                                                    if (!ok) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "当前手机暂未开启系统语音引擎，建议在系统设置中启用「文字转语音」",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    // B. 各已记录餐段大卡片（独立卡片，卡片间距拉大，呼吸感充裕）
                                    items(activePeriods) { period ->
                                        CareMealCard(
                                            record = currentRecord!!,
                                            period = period,
                                            isDark = isDark,
                                            onRecordMeal = { onOpenCareRecordDialog(period, CareRecordType.FASTING_OR_PRE) },
                                            onRecordItem = { type -> onOpenCareRecordDialog(period, type) }
                                        )
                                    }
                                }

                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }
                    }

                    CareViewTab.TABLE -> {
                        // 表格模式：直接使用外部放大表格 UI，保持功能完全一致
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 表格日期范围快捷筛选：近7天、近14天、近30天、全部显示 + 放大横屏展示按钮
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DateRangeSelectorCapsule(
                                    selectedRange = tableDateRange,
                                    onRangeSelected = onTableDateRangeChanged,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TealPrimary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.2.dp, TealPrimary),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onOpenLandscapeTable()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = "放大横屏展示表格",
                                            tint = TealPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "放大横屏",
                                            fontSize = careSp(13f),
                                            fontWeight = FontWeight.Bold,
                                            color = TealPrimary
                                        )
                                    }
                                }
                            }

                            val tableRecords: List<InsulinRecord> = remember(allRecords, tableDateRange) {
                                if (tableDateRange.days != null) allRecords.takeLast(tableDateRange.days) else allRecords
                            }

                            if (tableRecords.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "所选范围内暂无记录",
                                        fontSize = careSp(16f),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    RecordTable(
                                        records = tableRecords,
                                        allRecords = allRecords,
                                        todayStr = todayStr,
                                        onEdit = onEditRecord,
                                        onDelete = onDeleteRecord,
                                        isEnlarged = true,
                                        zoomScale = careFontSize.scaleFactor,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // 右下角放大横屏浮动大按钮（与标准模式横屏逻辑保持一致）
                                    FloatingActionButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onOpenLandscapeTable()
                                        },
                                        containerColor = TealPrimary,
                                        contentColor = Color.White,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(bottom = 20.dp, end = 20.dp)
                                            .size(56.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = "放大横屏展示表格",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. 底部大号常驻双按键（语音与大字录入，仅在看板模式展示；表格模式顶天立地浏览）
            if (activeTab == CareViewTab.DASHBOARD) {
                CareBottomActionButtons(
                    onOpenVoiceRecord = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenVoiceRecord()
                    },
                    onOpenManualRecord = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val period = InsulinRecord.getPeriodForTime()
                        onOpenCareRecordDialog(period, CareRecordType.FASTING_OR_PRE)
                    }
                )
            }
        }
    }
}

/**
 * 顶部关怀模式常驻顶栏
 */
/**
 * 顶部关怀模式常驻顶栏（集成 看板/表格 切换药丸、字号选择、夜间白天模式、退出）
 */
@Composable
private fun CareTopBar(
    currentTab: CareViewTab,
    onTabSelected: (CareViewTab) -> Unit,
    onExitCareMode: () -> Unit,
    onToggleTheme: () -> Unit,
    careFontSize: CareFontSize,
    onCareFontSizeChanged: (CareFontSize) -> Unit,
    isDark: Boolean,
    paddingTop: androidx.compose.ui.unit.Dp
) {
    var showFontSizeDialog by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = paddingTop)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：看板 / 表格 高对比度大药丸切换
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CareViewTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = if (isSelected) TealPrimary else Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .clickable { onTabSelected(tab) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(tab.icon, fontSize = 15.sp)
                            Text(
                                text = tab.title,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 右侧按键组：字号切换 + 黑白模式 + 退出按键
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 字号调节快捷药丸按钮
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.2.dp, TealPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable { showFontSizeDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "选择字号大小",
                            tint = TealPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = careFontSize.shortLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    }
                }

                // 黑夜白天模式快捷开关
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable(onClick = onToggleTheme)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDark) "切换白天模式" else "切换夜间模式",
                            tint = if (isDark) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 关怀模式图标开关（替代原单字“退出”按钮，彻底消除退出程序的歧义）
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = TealPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, TealPrimary),
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable(onClick = onExitCareMode)
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "关怀模式开关（当前开启，点击切回标准版）",
                            tint = TealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "关怀",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TealPrimary
                        ) {
                            Text(
                                text = "开",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFontSizeDialog) {
        CareFontSizeDialog(
            currentSize = careFontSize,
            onSelect = {
                onCareFontSizeChanged(it)
                showFontSizeDialog = false
            },
            onDismiss = { showFontSizeDialog = false }
        )
    }
}

/**
 * 专为长辈定制的大字字号选择对话框
 */
@Composable
private fun CareFontSizeDialog(
    currentSize: CareFontSize,
    onSelect: (CareFontSize) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "👓 选择长辈字号大小",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CareFontSize.entries.forEach { size ->
                        val isSelected = size == currentSize
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) TealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(size) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = size.title,
                                            fontSize = (18 * size.scaleFactor).coerceAtMost(24f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (size == CareFontSize.EXTRA) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = TealPrimary
                                            ) {
                                                Text("推荐", fontSize = 11.sp, color = Color.White, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                            }
                                        }
                                    }
                                    Text(
                                        text = size.desc,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelect(size) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = TealPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("完成", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 大字日期切换导航器
 */
@Composable
private fun CareDateNavigator(
    currentDate: String,
    canGoNext: Boolean,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val todayStr = remember { BGUtils.getTodayString() }
    val isToday = currentDate == todayStr

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPrevDay,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "前一天", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("前一天", fontSize = careSp(15f), fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentDate,
                    fontSize = careSp(18.5f),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isToday) "【今天】" else "历史记录",
                    fontSize = careSp(13.5f),
                    color = if (isToday) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }

            Button(
                onClick = onNextDay,
                enabled = canGoNext,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canGoNext) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    contentColor = if (canGoNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("后一天", fontSize = careSp(15f), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "后一天", modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * 大字红绿灯自然语言健康看板卡片 (CareStatusHeroCard)
 */
@Composable
private fun CareStatusHeroCard(
    record: InsulinRecord?,
    selectedDate: String,
    isDark: Boolean,
    isSpeaking: Boolean,
    onToggleSpeak: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // 计算当日最新一条血糖数据
    val latestBGInfo = remember(record) {
        if (record == null) return@remember null
        when {
            record.postNightBG != null && record.postNightBG > 0f -> "夜间加测" to record.postNightBG
            record.preNightBG != null && record.preNightBG > 0f -> "睡前" to record.preNightBG
            record.postDinnerBG != null && record.postDinnerBG > 0f -> "晚餐后" to record.postDinnerBG
            record.preDinnerBG != null && record.preDinnerBG > 0f -> "晚餐前" to record.preDinnerBG
            record.postLunchBG != null && record.postLunchBG > 0f -> "午餐后" to record.postLunchBG
            record.preLunchBG != null && record.preLunchBG > 0f -> "午餐前" to record.preLunchBG
            record.postBfBG != null && record.postBfBG > 0f -> "早餐后" to record.postBfBG
            record.fastingBG != null && record.fastingBG > 0f -> "晨起空腹" to record.fastingBG
            else -> null
        }
    }

    val (statusColor, statusTitle, adviceText) = remember(latestBGInfo, selectedDate) {
        if (latestBGInfo == null) {
            val isToday = selectedDate == BGUtils.getTodayString()
            Triple(
                TealPrimary,
                "今日待测",
                if (isToday) {
                    "今天暂未测量血糖。点击下方时段或按住绿色麦克风开始记。"
                } else {
                    "该日暂无血糖记录。"
                }
            )
        } else {
            val bg = latestBGInfo.second
            when {
                bg < 3.9f -> Triple(
                    if (isDark) Color(0xFFF87171) else Color(0xFFEF4444),
                    "⚠️ 偏低",
                    "当前血糖为 $bg，偏低了！请长辈立即吃2-3块饼干或喝半杯温糖水。"
                )
                bg in 3.9f..10.0f -> Triple(
                    Color(0xFF059669),
                    "🟢 达标",
                    "太棒了！最新血糖 $bg 处于理想范围，请继续保持清淡饮食与好心情！"
                )
                bg in 10.1f..13.9f -> Triple(
                    if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B),
                    "🟡 偏高",
                    "最新血糖 $bg 稍微偏高，下一顿饭主食少吃两口，多喝温水、适当散散步。"
                )
                else -> Triple(
                    if (isDark) Color(0xFFF87171) else Color(0xFFEF4444),
                    "🔴 偏高",
                    "当前血糖 $bg 偏高较多，请务必遵医嘱打胰岛素或服药，多喝温水。"
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 顶行：标题与状态胶囊
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🌟", fontSize = careSp(20f))
                    Text(
                        text = "今日最新状况",
                        fontSize = careSp(19f),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = statusTitle,
                        fontSize = careSp(15.5f),
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 中间行：超大数值 + 单位 + 测得时段 + 右侧圆形语音朗读按键
            if (latestBGInfo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.1f", latestBGInfo.second),
                            fontSize = careSp(44f),
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(
                                text = "mmol/L",
                                fontSize = careSp(17f),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${latestBGInfo.first}测得",
                                fontSize = careSp(14f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // 语音朗读大圆形按键（不占用整行，视觉清爽醒目）
                    Surface(
                        shape = CircleShape,
                        color = if (isSpeaking) statusColor else statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.5.dp, statusColor),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable { onToggleSpeak(adviceText) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isSpeaking) "停止朗读" else "朗读提醒",
                                tint = if (isSpeaking) Color.White else statusColor,
                                modifier = Modifier
                                    .size(26.dp)
                                    .then(if (isSpeaking) Modifier.scale(pulseScale) else Modifier)
                            )
                        }
                    }
                }
            }

            // 底部：简短大字医生叮嘱
            Text(
                text = adviceText,
                fontSize = careSp(16.5f),
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = careSp(24f)
            )
        }
    }
}

/**
 * 药盒式餐段大卡片 (早 / 午 / 晚 / 睡前)
 * - 卡片大尺寸、留白舒展、高对比度
 * - 彻底去掉条目上“卡片套小卡片”的紧窄边框，去除“注射”、“服药”、“用餐”、“活动”等形式主义废标
 * - 第一行大字标题+时段标签+红绿灯达标徽章，第二行超大字高对比度数值
 */
@Composable
private fun CareMealCard(
    record: InsulinRecord,
    period: MealPeriod,
    isDark: Boolean,
    onRecordMeal: () -> Unit,
    onRecordItem: (CareRecordType) -> Unit
) {
    val title = when (period) {
        MealPeriod.MORNING -> "🌅 早餐"
        MealPeriod.LUNCH -> "☀️ 午餐"
        MealPeriod.DINNER -> "🌆 晚餐"
        MealPeriod.NIGHT -> "🌙 睡前"
    }

    val (preMealLabel, preMealValue) = when (period) {
        MealPeriod.MORNING -> "晨起空腹" to record.fastingBG
        MealPeriod.LUNCH -> "午餐前血糖" to record.preLunchBG
        MealPeriod.DINNER -> "晚餐前血糖" to record.preDinnerBG
        MealPeriod.NIGHT -> "睡前血糖" to record.preNightBG
    }
    val preMealTime = record.getItemTime(period, "pre_bg")

    val postMealList = record.getPostMealList(period)

    val (insulinDose, medName, medTiming) = when (period) {
        MealPeriod.MORNING -> Triple(record.bfInsulin, record.bfMedName, record.bfMedTiming)
        MealPeriod.LUNCH -> Triple(record.lunchInsulin, record.lunchMedName, record.lunchMedTiming)
        MealPeriod.DINNER -> Triple(record.dinnerInsulin, record.dinnerMedName, record.dinnerMedTiming)
        MealPeriod.NIGHT -> Triple(record.bedtimeInsulin, record.nightMedName, record.nightMedTiming)
    }
    val medTime = record.getItemTime(period, "med")

    val dietValue = when (period) {
        MealPeriod.MORNING -> record.bfDiet
        MealPeriod.LUNCH -> record.lunchDiet
        MealPeriod.DINNER -> record.dinnerDiet
        MealPeriod.NIGHT -> record.nightDiet
    }
    val dietTime = record.getItemTime(period, "diet")

    val exerciseValue = when (period) {
        MealPeriod.MORNING -> record.bfExercise
        MealPeriod.LUNCH -> record.lunchExercise
        MealPeriod.DINNER -> record.dinnerExercise
        MealPeriod.NIGHT -> record.nightExercise
    }
    val exerciseTime = record.getItemTime(period, "exercise")

    val hasPreBG = preMealValue != null && preMealValue > 0f
    val hasPostBG = postMealList.isNotEmpty()
    val hasInsulin = insulinDose != null && insulinDose > 0f
    val hasOralMed = medName.isNotBlank() && medName != "胰岛素" && (insulinDose == null || insulinDose == 0f)
    val hasDiet = dietValue.isNotBlank()
    val hasExercise = exerciseValue.isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onRecordMeal),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 卡片大头部：时段大标题（点击卡片即可编辑或录入此餐）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = careSp(22f),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 内部条目列表：通透大布局，条目之间由清爽微弱细线分隔
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                var isFirst = true

                // 1. 餐前 / 晨起空腹血糖
                if (hasPreBG) {
                    val status = getBGStatus(preMealValue!!, isFasting = period == MealPeriod.MORNING)
                    CareItemRow(
                        icon = "🩸",
                        title = preMealLabel,
                        timeText = preMealTime.ifBlank { null },
                        statusBadge = status.label,
                        statusBadgeColor = status.color,
                        onClick = { onRecordItem(CareRecordType.FASTING_OR_PRE) }
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.1f", preMealValue),
                                fontSize = careSp(36f),
                                fontWeight = FontWeight.ExtraBold,
                                color = status.color
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "mmol/L",
                                fontSize = careSp(17f),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                    }
                    isFirst = false
                }

                // 2. 餐后血糖 (支持多餐后点)
                if (hasPostBG) {
                    postMealList.forEach { postMeal ->
                        if (!isFirst) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        }
                        val postLabel = if (period == MealPeriod.NIGHT) "加测血糖" else (if (postMeal.tag.isNotBlank()) postMeal.tag else "餐后血糖")
                        val status = getBGStatus(postMeal.value, isFasting = false)
                        val entryTime = postMeal.time.ifBlank { null }
                        CareItemRow(
                            icon = "🩸",
                            title = postLabel,
                            timeText = entryTime,
                            statusBadge = status.label,
                            statusBadgeColor = status.color,
                            onClick = { onRecordItem(CareRecordType.POST_MEAL) }
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", postMeal.value),
                                    fontSize = careSp(36f),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = status.color
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "mmol/L",
                                    fontSize = careSp(17f),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                        }
                        isFirst = false
                    }
                }

                // 3. 胰岛素注射
                if (hasInsulin) {
                    if (!isFirst) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    }
                    val doseStr = if (insulinDose!! % 1f == 0f) "${insulinDose.toInt()}" else "$insulinDose"
                    val medColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    CareItemRow(
                        icon = "💉",
                        title = if (medName.isNotBlank()) medName else "胰岛素注射",
                        timeText = medTime.ifBlank { null },
                        statusBadge = null, // 去掉毫无意义的“注射”废标
                        onClick = { onRecordItem(CareRecordType.MEDICATION) }
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = doseStr,
                                fontSize = careSp(34f),
                                fontWeight = FontWeight.ExtraBold,
                                color = medColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "单位 (U)",
                                fontSize = careSp(17f),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            if (medTiming.isNotBlank()) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "· $medTiming",
                                    fontSize = careSp(16f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                    isFirst = false
                }

                // 4. 口服降糖药
                if (hasOralMed) {
                    if (!isFirst) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    }
                    val medColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    CareItemRow(
                        icon = "💊",
                        title = "口服降糖药",
                        timeText = medTime.ifBlank { null },
                        statusBadge = null, // 去掉无意义的“服药”废标
                        onClick = { onRecordItem(CareRecordType.MEDICATION) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = medName,
                                fontSize = careSp(22f),
                                fontWeight = FontWeight.Bold,
                                color = medColor
                            )
                            if (medTiming.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "· $medTiming",
                                    fontSize = careSp(16f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    isFirst = false
                }

                // 5. 饮食记录
                if (hasDiet) {
                    if (!isFirst) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    }
                    CareItemRow(
                        icon = "🥣",
                        title = "饮食情况",
                        timeText = dietTime.ifBlank { null },
                        statusBadge = null, // 去掉无意义的“用餐”废标
                        onClick = { onRecordItem(CareRecordType.DIET) }
                    ) {
                        Text(
                            text = dietValue,
                            fontSize = careSp(20f),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = careSp(28f)
                        )
                    }
                    isFirst = false
                }

                // 6. 运动情况
                if (hasExercise) {
                    if (!isFirst) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    }
                    val exColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                    CareItemRow(
                        icon = "🏃",
                        title = "运动情况",
                        timeText = exerciseTime.ifBlank { null },
                        statusBadge = null, // 去掉无意义的“活动”废标
                        onClick = { onRecordItem(CareRecordType.EXERCISE) }
                    ) {
                        Text(
                            text = exerciseValue,
                            fontSize = careSp(20f),
                            fontWeight = FontWeight.SemiBold,
                            color = exColor,
                            lineHeight = careSp(28f)
                        )
                    }
                    isFirst = false
                }
            }
        }
    }
}

/**
 * 专为长辈定制的无嵌套框通透大条目：
 * 第一行：大图标 + 大字项目名 + 时间 + 状态标签（仅保留医疗状态红绿灯）
 * 第二行：超大数值或舒展大字内容（呼吸感充裕，无盒中盒压迫）
 */
@Composable
private fun CareItemRow(
    icon: String,
    title: String,
    timeText: String? = null,
    statusBadge: String? = null,
    statusBadgeColor: Color = Color.Unspecified,
    onClick: () -> Unit,
    valueContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 第一行：项目名 + 时间 + 状态标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(icon, fontSize = careSp(20f))
                Text(
                    text = title,
                    fontSize = careSp(18f),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!timeText.isNullOrBlank()) {
                    Text(
                        text = timeText,
                        fontSize = careSp(14f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = 1
                    )
                }
            }

            if (!statusBadge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusBadgeColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = statusBadge,
                        fontSize = careSp(14f),
                        fontWeight = FontWeight.ExtraBold,
                        color = statusBadgeColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // 第二行：超大字数值 / 内容（缩进微调对齐文字）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            valueContent()
        }
    }
}

/**
 * 暂无记录时的单个大气清爽空状态卡片（带4餐快捷大按钮）
 */
@Composable
private fun CareEmptyDayHeroCard(
    selectedDate: String,
    onOpenRecord: (MealPeriod, CareRecordType) -> Unit
) {
    val todayStr = remember { BGUtils.getTodayString() }
    val isToday = selectedDate == todayStr

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("📅", fontSize = careSp(44f))

            Text(
                text = if (isToday) "今日暂无健康记录" else "该日暂无健康记录",
                fontSize = careSp(22f),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )

            Text(
                text = "点击下方餐段开始记，或轻触下方的「语音记一笔」直接说",
                fontSize = careSp(16f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = careSp(24f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 4个大号餐段快速记录按键
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MealPeriod.entries.forEach { p ->
                    val periodLabel = when (p) {
                        MealPeriod.MORNING -> "晨间"
                        MealPeriod.LUNCH -> "午餐"
                        MealPeriod.DINNER -> "晚餐"
                        MealPeriod.NIGHT -> "睡前"
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = TealPrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.2.dp, TealPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenRecord(p, CareRecordType.FASTING_OR_PRE) }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(p.iconText, fontSize = careSp(19f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = periodLabel,
                                fontSize = careSp(14.5f),
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 底部常驻双按键（离线语音记一笔与大字手动记一笔）
 */
@Composable
private fun CareBottomActionButtons(
    onOpenVoiceRecord: () -> Unit,
    onOpenManualRecord: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 绿色适老化语音记一笔按键
            Button(
                onClick = onOpenVoiceRecord,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(58.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "语音记一笔", modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "语音记一笔",
                    fontSize = careSp(18f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // 手动记一笔按键
            OutlinedButton(
                onClick = onOpenManualRecord,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, TealPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier
                    .weight(0.9f)
                    .height(58.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "记一笔", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "记一笔",
                    fontSize = careSp(18f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

data class BGStatusInfo(val label: String, val color: Color)

private fun getBGStatus(bg: Float, isFasting: Boolean): BGStatusInfo {
    return when {
        bg < 3.9f -> BGStatusInfo("🔴 偏低", Color(0xFFEF4444))
        isFasting && bg in 3.9f..7.2f -> BGStatusInfo("🟢 达标", Color(0xFF059669))
        !isFasting && bg in 3.9f..10.0f -> BGStatusInfo("🟢 达标", Color(0xFF059669))
        bg <= 13.9f -> BGStatusInfo("🟡 偏高", Color(0xFFF59E0B))
        else -> BGStatusInfo("🔴 偏高", Color(0xFFEF4444))
    }
}

private fun getBGColor(bg: Float?, isDark: Boolean): Color {
    if (bg == null || bg <= 0f) return Color.Unspecified
    return when {
        bg < 3.9f -> if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
        bg in 3.9f..10.0f -> Color(0xFF059669)
        bg <= 13.9f -> if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)
        else -> if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
    }
}
