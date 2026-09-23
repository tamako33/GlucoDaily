package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BGUtils
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.ui.TableDateRange
import com.example.ui.ItemType
import com.example.ui.components.AddItemDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.LandscapeTableView
import com.example.ui.components.RecordCard
import com.example.ui.components.RecordEditDialog
import com.example.ui.components.RecordTable
import com.example.ui.components.SiriVoiceBottomOverlay
import com.example.ui.components.SiriVoiceFabButton
import com.example.ui.components.TrendChart
import com.example.ui.components.VoiceInputDialog
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealOnContainer
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.ui.theme.TealPrimaryLight

@Composable
fun InsulinTrackerScreen(
    viewModel: InsulinTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val recentTrendRecords by viewModel.recentTrendRecords.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val tableDateRange by viewModel.tableDateRange.collectAsStateWithLifecycle()
    val tableRecords by viewModel.tableRecords.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBackupFromUri(context, it) }
    }

    var showLandscapeDialog by rememberSaveable { mutableStateOf(false) }
    var showVoiceDialog by rememberSaveable { mutableStateOf(false) }
    var isHoldingVoice by remember { mutableStateOf(false) }
    var showSiriVoiceOverlay by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var isTrendExpanded by rememberSaveable { mutableStateOf(true) }
    val todayStr = remember { BGUtils.getTodayString() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 12.dp, end = 4.dp)
            ) {
                // 在表格页面：在绿色的加号上方增加一个按钮，将表格放大并横屏显示数据
                if (viewMode == ViewMode.TABLE) {
                    FloatingActionButton(
                        onClick = { showLandscapeDialog = true },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = TealPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("fab_landscape_expand")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "放大横屏显示表格",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // 语音智能录入按钮（在加号正上方垂直严格对齐）：仿Siri长按说话，下方动效实时识别，松开即自动填入数值变化
                SiriVoiceFabButton(
                    isHolding = isHoldingVoice,
                    onPressStart = {
                        isHoldingVoice = true
                        showSiriVoiceOverlay = true
                    },
                    onPressEnd = {
                        isHoldingVoice = false
                    },
                    onClick = {
                        isHoldingVoice = false
                        showSiriVoiceOverlay = true
                    }
                )

                FloatingActionButton(
                    onClick = { viewModel.openAddDialog() },
                    containerColor = TealPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("fab_add_record")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "记一笔",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 2.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 0. Top Header Component (跟随整个页面一起滑动，不固定在顶部)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    color = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(TealPrimaryLight, TealPrimaryDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Text(
                                text = "每日胰岛血糖",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${filteredRecords.size}条",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // View mode switcher (Cards vs Table)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                    .padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ViewToggleButton(
                                    selected = viewMode == ViewMode.CARDS,
                                    icon = Icons.Default.FormatListBulleted,
                                    label = "卡片",
                                    onClick = { viewModel.setViewMode(ViewMode.CARDS) },
                                    tag = "view_toggle_cards"
                                )

                                ViewToggleButton(
                                    selected = viewMode == ViewMode.TABLE,
                                    icon = Icons.Default.TableChart,
                                    label = "表格",
                                    onClick = { viewModel.setViewMode(ViewMode.TABLE) },
                                    tag = "view_toggle_table"
                                )
                            }

                            // 1. Manual Theme Switcher Button
                            val (themeIcon, themeDesc) = when (themeMode) {
                                AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to "当前跟随系统（点击切换）"
                                AppThemeMode.LIGHT -> Icons.Default.LightMode to "当前日间模式（点击切换）"
                                AppThemeMode.DARK -> Icons.Default.DarkMode to "当前夜间模式（点击切换）"
                            }
                            IconButton(
                                onClick = { viewModel.cycleThemeMode() },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = themeIcon,
                                    contentDescription = themeDesc,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("overflow_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "更多选项",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    // 主题模式选择
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("跟随系统")
                                                if (themeMode == AppThemeMode.SYSTEM) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = TealPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.BrightnessAuto,
                                                contentDescription = null,
                                                tint = if (themeMode == AppThemeMode.SYSTEM) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.setThemeMode(AppThemeMode.SYSTEM)
                                        },
                                        modifier = Modifier.testTag("menu_theme_system")
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("日间模式")
                                                if (themeMode == AppThemeMode.LIGHT) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = TealPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.LightMode,
                                                contentDescription = null,
                                                tint = if (themeMode == AppThemeMode.LIGHT) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.setThemeMode(AppThemeMode.LIGHT)
                                        },
                                        modifier = Modifier.testTag("menu_theme_light")
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("夜间模式")
                                                if (themeMode == AppThemeMode.DARK) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = TealPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.DarkMode,
                                                contentDescription = null,
                                                tint = if (themeMode == AppThemeMode.DARK) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.setThemeMode(AppThemeMode.DARK)
                                        },
                                        modifier = Modifier.testTag("menu_theme_dark")
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                    DropdownMenuItem(
                                        text = { Text("备份数据 (ZIP)") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                tint = TealPrimary
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.exportBackup(context)
                                        },
                                        modifier = Modifier.testTag("menu_export_backup")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("恢复 / 导入数据") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                tint = TealPrimary
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/json", "*/*"))
                                        },
                                        modifier = Modifier.testTag("menu_import_backup")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("恢复演示数据") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.RestartAlt,
                                                contentDescription = null,
                                                tint = TealPrimary
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.resetToDemoData()
                                        },
                                        modifier = Modifier.testTag("menu_reset_demo")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("清空所有记录", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.DeleteSweep,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.clearAllRecords()
                                        },
                                        modifier = Modifier.testTag("menu_clear_all")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. Trend Chart (紧凑瘦身 & 支持一键折叠/展开)
            item {
                TrendChart(
                    records = recentTrendRecords,
                    isExpanded = isTrendExpanded,
                    onToggleExpanded = { isTrendExpanded = !isTrendExpanded }
                )
            }

            // 2. Animated Records Section (流畅切换过渡动画)
            item {
                AnimatedContent(
                    targetState = viewMode,
                    transitionSpec = {
                        if (targetState == ViewMode.TABLE) {
                            (fadeIn(animationSpec = tween(280)) + slideInHorizontally(animationSpec = tween(280)) { width -> width / 4 })
                                .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(180)) { width -> -width / 4 })
                        } else {
                            (fadeIn(animationSpec = tween(280)) + slideInHorizontally(animationSpec = tween(280)) { width -> -width / 4 })
                                .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(180)) { width -> width / 4 })
                        }
                    },
                    label = "view_mode_transition"
                ) { targetMode ->
                    if (filteredRecords.isEmpty()) {
                        EmptyRecordsView(onAdd = { viewModel.openAddItemDialog() })
                    } else if (targetMode == ViewMode.CARDS) {
                        DailyCardPager(
                            records = filteredRecords,
                            allRecords = allRecords,
                            todayStr = todayStr,
                            scrollToDateFlow = viewModel.scrollToDateEvent,
                            onShowMessage = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onEdit = { viewModel.openEditDialog(it) },
                            onDelete = { viewModel.promptDelete(it) },
                            onEditPeriod = { record, period -> viewModel.openEditDialog(record, period) },
                            onAddItem = { record, period, itemType -> viewModel.openAddItemDialog(record.date, period, itemType) },
                            onDeletePostMeal = { record, period, idx -> viewModel.deletePostMealEntry(record.date, period, idx) },
                            onDeleteSingleItem = { record, period, itemType, idx ->
                                viewModel.deleteSingleItem(record.date, period, itemType, idx)
                            }
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 表格日期范围快捷筛选：近7天、近14天、近30天、全部显示
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "显示条目范围",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TableDateRange.entries.forEach { range ->
                                        val isSelected = range == tableDateRange
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                            modifier = Modifier
                                                .clickable { viewModel.setTableDateRange(range) }
                                                .testTag("table_range_${range.name}")
                                        ) {
                                            Text(
                                                text = range.label,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (tableRecords.isEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "所选范围内暂无记录",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                RecordTable(
                                    records = tableRecords,
                                    allRecords = allRecords,
                                    todayStr = todayStr,
                                    onEdit = { viewModel.openEditDialog(it) },
                                    onDelete = { viewModel.promptDelete(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Active Dialogs
    when (val state = dialogState) {
        is DialogState.None -> Unit
        is DialogState.AddItem -> {
            AddItemDialog(
                initialDate = state.initialDate,
                initialPeriod = state.initialPeriod,
                initialItemType = state.initialItemType,
                allRecords = allRecords,
                onDismiss = { viewModel.dismissDialog() },
                onSaveItem = { date, period, itemType, bgVal, dietText, medName, dose, medTiming, postMealTag, postMealTime, exerciseText, keepOpen ->
                    viewModel.saveSingleItem(
                        date = date,
                        period = period,
                        itemType = itemType,
                        bgValue = bgVal,
                        dietText = dietText,
                        medName = medName,
                        dose = dose,
                        medTiming = medTiming,
                        postMealTag = postMealTag,
                        postMealTime = postMealTime,
                        exerciseText = exerciseText,
                        keepDialogOpen = keepOpen
                    )
                }
            )
        }
        is DialogState.Edit -> {
            RecordEditDialog(
                initialRecord = state.initialRecord,
                initialDate = state.initialDate,
                initialPeriod = state.initialPeriod,
                allRecords = allRecords,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { record -> viewModel.saveRecord(record) }
            )
        }
        is DialogState.ConfirmDelete -> {
            DeleteConfirmDialog(
                record = state.record,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.confirmDelete(state.record) }
            )
        }
    }

    // Fullscreen Landscape Table View (纯表格，无折线图，稳定横屏显示)
    if (showLandscapeDialog) {
        LandscapeTableView(
            records = tableRecords,
            allRecords = allRecords,
            todayStr = todayStr,
            selectedRange = tableDateRange,
            onRangeSelect = { viewModel.setTableDateRange(it) },
            onEdit = { viewModel.openEditDialog(it) },
            onDelete = { viewModel.promptDelete(it) },
            onDismiss = { showLandscapeDialog = false }
        )
    }

    // 智能语音录入弹窗（可用于手动微调与详细编辑）
    if (showVoiceDialog) {
        VoiceInputDialog(
            onDismiss = { showVoiceDialog = false },
            onConfirmSave = { parsed ->
                viewModel.saveVoiceRecord(parsed)
            },
            onFineTuneInEditDialog = { parsed ->
                viewModel.openEditDialogFromVoice(parsed)
            }
        )
    }

    // Siri 仿生实时语音动效与长按自动填入浮层
    SiriVoiceBottomOverlay(
        isVisible = showSiriVoiceOverlay,
        isHolding = isHoldingVoice,
        onSaveRecord = { parsed ->
            viewModel.saveVoiceRecord(parsed)
        },
        onDismiss = {
            showSiriVoiceOverlay = false
            isHoldingVoice = false
        }
    )

    // 备份数据导入确认弹窗（支持应用内导入及外部用其他应用打开时还原确认）
    pendingImport?.let { importData ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "发现备份数据包",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!importData.fileName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TealPrimary.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, TealPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = importData.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Text(
                        text = "已识别到 ${importData.count} 条记录\n数据日期：${importData.dateRange}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "请确认是否还原备份并选择恢复方式：",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• 合并导入：保留当前记录，自动更新重合日期的记录（推荐，安全无损）\n• 全量覆盖：清空现有全部数据，完全恢复为该备份中的记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmImport(overwrite = false) },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("btn_confirm_import_merge")
                ) {
                    Text("合并导入 (推荐)")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = { viewModel.dismissImportDialog() }
                    ) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = { viewModel.confirmImport(overwrite = true) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("btn_confirm_import_overwrite")
                    ) {
                        Text("全量覆盖")
                    }
                }
            }
        )
    }
}
}

@Composable
private fun ViewToggleButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
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
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyRecordsView(onAdd: () -> Unit) {
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "点击上方“记一笔”或悬浮按钮随时记录今天数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("添加第一笔记录")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyCardPager(
    records: List<InsulinRecord>,
    allRecords: List<InsulinRecord>,
    todayStr: String,
    scrollToDateFlow: SharedFlow<String>?,
    onShowMessage: (String) -> Unit,
    onEdit: (InsulinRecord) -> Unit,
    onDelete: (InsulinRecord) -> Unit,
    onEditPeriod: ((InsulinRecord, MealPeriod) -> Unit)? = null,
    onAddItem: ((InsulinRecord, MealPeriod, ItemType?) -> Unit)? = null,
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
                    modifier = Modifier.testTag("date_picker_confirm_button")
                ) {
                    Text("查看")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.testTag("date_picker_cancel_button")
                ) {
                    Text("取消")
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
                        fontWeight = FontWeight.Bold,
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
        // 顶部日期翻页导航栏：按照（左翻页，图标，日期年月日，周几，当天/回今天按钮，右翻页）排列，空间舒展大方
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            color = if (AppThemeColors.isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                   else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 左翻页（圆润三角形向左）
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
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(TealPrimary.copy(alpha = 0.12f))
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
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clickable { showDatePicker = true }
                            .testTag("pager_date_text")
                    )

                    // 4. 周几（如：周二）
                    if (weekdayDisplay.isNotBlank()) {
                        Text(
                            text = weekdayDisplay,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 5. 当天和回今天按钮
                    if (isCurrentDateToday) {
                        // 是当天：显示“当天”标签
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TealPrimary)
                                .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                .testTag("pager_today_badge")
                        ) {
                            Text(
                                text = "当天",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // 不是当天：显示“回今天”按钮，点击平滑滚动回到当天
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TealPrimary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }
                }

                // 6. 右翻页（圆润三角形向右）
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

        // 单天卡片 HorizontalPager：带流畅过渡动画的左右翻页
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_records_pager"),
            verticalAlignment = Alignment.Top
        ) { page ->
            val record = chronologicalRecords[page]
            val prevNight = remember(record.date, allRecords) {
                BGUtils.getPrevNightInsulin(record.date, allRecords)
            }
            // 流畅丝滑的翻页缩放与透明度呼吸过渡动效
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            RecordCard(
                record = record,
                prevNightInfo = prevNight,
                isToday = record.date == todayStr,
                onEdit = { onEdit(record) },
                onDelete = { onDelete(record) },
                onEditPeriod = { period -> onEditPeriod?.invoke(record, period) ?: onEdit(record) },
                onAddItem = { period, itemType -> onAddItem?.invoke(record, period, itemType) },
                onDeletePostMeal = { period, idx -> onDeletePostMeal?.invoke(record, period, idx) },
                onDeleteSingleItem = { period, itemType, idx -> onDeleteSingleItem?.invoke(record, period, itemType, idx) },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val clampedOffset = pageOffset.coerceIn(0f, 1f)
                        val scale = 0.93f + 0.07f * (1f - clampedOffset)
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.45f + 0.55f * (1f - clampedOffset)
                    }
            )
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
            .size(38.dp)
            .clip(CircleShape)
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
