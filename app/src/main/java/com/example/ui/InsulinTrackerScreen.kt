package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.animation.SizeTransform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Elderly
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.widthIn
import com.example.ui.components.FrostedGlassDialogOverlay
import com.example.ui.components.CareModeToggleSwitch
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
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
import com.example.ui.components.CareHomeView
import com.example.ui.components.CareRecordDialog
import com.example.ui.components.CareRecordType
import com.example.ui.components.DailyCardPager
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyRecordsView
import com.example.ui.components.LandscapeTableView
import com.example.ui.components.RecordCard
import com.example.ui.components.RecordEditDialog
import com.example.ui.components.RecordTable
import com.example.ui.components.SiriVoiceBottomOverlay
import com.example.ui.components.SiriVoiceFabButton
import com.example.ui.components.StatsView
import com.example.ui.components.DateRangeSelectorCapsule
import com.example.ui.components.TrendChart
import com.example.ui.components.ApplePillShape
import com.example.ui.components.AppleSmShape
import com.example.ui.components.AppleSegmentThumbShape
import com.example.ui.components.applePressEffect
import com.example.ui.components.ViewModeSegmentedControl
import com.example.ui.components.ViewToggleButton
import com.example.ui.components.VoiceInputDialog
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealOnContainer
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import com.example.ui.theme.TealPrimaryLight
import com.example.ui.theme.LocalIsDarkTheme

/**
 * 血糖胰岛素应用主屏幕 (InsulinTrackerScreen)：
 *
 * 架构职责与边界隔离：
 * 1. 顶层脚手架调度：负责整个画面的 Scaffold、状态栏内边距、悬浮操作按钮组 (FABs) 及菜单动作。
 * 2. 状态驱动呈现：纯状态消费组件，严格依赖 [InsulinTrackerViewModel] 暴露的不可变 StateFlow，
 *    通过 collectAsStateWithLifecycle 进行生命周期感知的响应式渲染。
 * 3. 视图路由与分发：
 *    - 卡片视图模式 (CARDS)：委托给独立的 [DailyCardPager] 组件进行单天卡片滑动与日期跳转；
 *    - 表格视图模式 (TABLE)：委托给 [RecordTable] 与 [LandscapeTableView] 进行多日数据对比；
 * 4. 弹窗中枢挂载：负责全局层级的弹窗挂载（记一笔、编辑、删除确认、Siri仿生语音、数据备份还原），
 *    各弹窗内部状态自治，不污染主界面渲染树。
 *
 * 变更防牵连机制：
 * - 针对各视图与弹窗的交互全部通过 Lambda 回调与 ViewModel 事件通信，杜绝直接状态跨层污染；
 * - 历史遗留的大型翻页组件与通用小部件已全部解耦至单独的 components 文件中维护。
 */
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
    val topTrendChartType by viewModel.topTrendChartType.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val tableDateRange by viewModel.tableDateRange.collectAsStateWithLifecycle()
    val tableRecords by viewModel.tableRecords.collectAsStateWithLifecycle()
    val isCareMode by viewModel.isCareMode.collectAsStateWithLifecycle()
    val careFontSize by viewModel.careFontSize.collectAsStateWithLifecycle()
    val statsDateRange by viewModel.statsDateRange.collectAsStateWithLifecycle()
    val statsRecords by viewModel.statsRecords.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val isDark = LocalIsDarkTheme.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBackupFromUri(context, it) }
    }

    var showLandscapeDialog by rememberSaveable { mutableStateOf(false) }
    var showVoiceDialog by rememberSaveable { mutableStateOf(false) }
    var isHoldingVoice by remember { mutableStateOf(false) }
    var showSiriVoiceOverlay by remember { mutableStateOf(false) }
    val requestShowVoice by com.example.data.VoiceRecognitionManager.requestShowOverlay.collectAsStateWithLifecycle()
    LaunchedEffect(requestShowVoice) {
        if (requestShowVoice) {
            showSiriVoiceOverlay = true
            com.example.data.VoiceRecognitionManager.clearShowOverlayRequest()
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var isTrendExpanded by rememberSaveable { mutableStateOf(true) }
    val todayStr = remember { BGUtils.getTodayString() }
    var currentSelectedDate by remember { mutableStateOf(todayStr) }
    var showCareRecordDialog by remember { mutableStateOf(false) }
    var careRecordInitialPeriod by remember { mutableStateOf(MealPeriod.MORNING) }
    var careRecordInitialType by remember { mutableStateOf(CareRecordType.FASTING_OR_PRE) }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    val isAnyDialogOpen = dialogState !is DialogState.None ||
            showSiriVoiceOverlay ||
            showVoiceDialog ||
            pendingImport != null ||
            showLandscapeDialog ||
            showCareRecordDialog

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = isCareMode,
            transitionSpec = {
                if (targetState) {
                    // 进入关怀模式：自内向外放大淡入，伴随弹性呼吸感
                    (fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                     scaleIn(initialScale = 0.93f, animationSpec = tween(350, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 1.05f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                        )
                } else {
                    // 退出关怀模式：缩小淡回常规专业视图
                    (fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                     scaleIn(initialScale = 1.05f, animationSpec = tween(350, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 0.93f, animationSpec = tween(240, easing = FastOutSlowInEasing))
                        )
                }
            },
            label = "CareModeMotionTransition",
            modifier = Modifier.fillMaxSize()
        ) { careModeActive ->
            if (careModeActive) {
                CareHomeView(
                    allRecords = allRecords,
                    selectedDate = currentSelectedDate,
                    onDateChanged = { currentSelectedDate = it },
                    todayStr = todayStr,
                    tableDateRange = tableDateRange,
                    onTableDateRangeChanged = { viewModel.setTableDateRange(it) },
                    onEditRecord = { viewModel.openEditDialog(it) },
                    onDeleteRecord = { viewModel.promptDelete(it) },
                    onOpenCareRecordDialog = { period, type ->
                        careRecordInitialPeriod = period
                        careRecordInitialType = type
                        showCareRecordDialog = true
                    },
                    onOpenVoiceRecord = {
                        if (!showSiriVoiceOverlay) {
                            showSiriVoiceOverlay = true
                            com.example.data.VoiceRecognitionManager.startListening(context)
                        }
                    },
                    onExitCareMode = { viewModel.toggleCareMode() },
                    careFontSize = careFontSize,
                    onCareFontSizeChanged = { viewModel.setCareFontSize(it) },
                    onToggleTheme = {
                        val willBeDark = !isDark
                        viewModel.setThemeMode(if (willBeDark) AppThemeMode.DARK else AppThemeMode.LIGHT)
                        Toast.makeText(
                            context,
                            if (willBeDark) "已切换至夜间模式" else "已切换至白天模式",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onOpenLandscapeTable = {
                        showLandscapeDialog = true
                    }
                )
            } else {
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

                // 语音智能录入按钮（在加号正上方垂直严格对齐）：按下开始识别，再次按下停止识别（告别长按）
                val isVoiceListening by com.example.data.VoiceRecognitionManager.isListening.collectAsStateWithLifecycle()
                SiriVoiceFabButton(
                    isListening = isVoiceListening,
                    onClick = {
                        if (!showSiriVoiceOverlay) {
                            showSiriVoiceOverlay = true
                        } else {
                            if (isVoiceListening) {
                                com.example.data.VoiceRecognitionManager.stopListening()
                            } else {
                                com.example.data.VoiceRecognitionManager.startListening(context)
                            }
                        }
                    }
                )

                FloatingActionButton(
                    onClick = { viewModel.openAddDialog() },
                    containerColor = TealPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .applePressEffect(0.92f)
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
                        .clip(AppleSmShape),
                    color = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "App Icon",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(AppleSmShape)
                            )

                            Text(
                                text = "每日胰岛血糖",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.38).sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 切换至关怀版大字模式按钮
                            Surface(
                                shape = ApplePillShape,
                                color = TealPrimary.copy(alpha = 0.14f),
                                border = BorderStroke(1.2.dp, TealPrimary.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(ApplePillShape)
                                    .clickable {
                                        viewModel.toggleCareMode()
                                    }
                                    .testTag("care_mode_toggle_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Elderly,
                                        contentDescription = "切换至关怀版",
                                        tint = TealPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "关怀版",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                }
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
                                    .clip(CircleShape)
                                    .applePressEffect(0.92f)
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
                                        .clip(CircleShape)
                                        .applePressEffect(0.92f)
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

            // 1. 顶部走势图（置顶于切换器上方；与下方翻页严格同频：300ms 同步折叠至顶端/同步展开）
            item {
                val syncDuration = 300
                val syncEase = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                AnimatedVisibility(
                    visible = viewMode == ViewMode.CARDS,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(syncDuration, easing = syncEase)
                    ) + fadeIn(animationSpec = tween(syncDuration, easing = LinearOutSlowInEasing)),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(syncDuration, easing = syncEase)
                    ) + fadeOut(animationSpec = tween(syncDuration * 2 / 3, easing = FastOutLinearInEasing))
                ) {
                    TrendChart(
                        records = recentTrendRecords,
                        allRecords = allRecords,
                        selectedDate = currentSelectedDate,
                        isExpanded = isTrendExpanded,
                        onToggleExpanded = { isTrendExpanded = !isTrendExpanded },
                        selectedChartType = topTrendChartType,
                        onSelectChartType = { viewModel.setTopTrendChartType(it) }
                    )
                }
            }

            // 2. Records Header & Mode Toggle (位于走势图正下方，切到统计/表格时与下方切页在 300ms 内严格同频上升至最顶)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(
                            targetState = viewMode,
                            transitionSpec = {
                                fadeIn(tween(200, easing = LinearOutSlowInEasing)) togetherWith fadeOut(tween(140, easing = FastOutLinearInEasing))
                            },
                            label = "header_title_text"
                        ) { mode ->
                            Text(
                                text = when (mode) {
                                    ViewMode.CARDS -> "记录明细"
                                    ViewMode.STATS -> "图表分析"
                                    ViewMode.TABLE -> "汇总表格"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        // 仅在卡片与表格视图展示记录计数胶囊；在统计视图删除“统计7天”气泡，彻底避免左侧拥挤与文字遮挡
                        if (viewMode != ViewMode.STATS) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(AppleSegmentThumbShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (viewMode) {
                                        ViewMode.CARDS -> "共 ${filteredRecords.size} 条"
                                        ViewMode.TABLE -> "共 ${tableRecords.size} 天"
                                        else -> ""
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // View mode switcher (Cards vs Stats vs Table) 紧凑 192dp 宽度，平滑物理滑块（即时响应，零延迟同频启动）
                    ViewModeSegmentedControl(
                        currentMode = viewMode,
                        onModeChanged = { newMode ->
                            if (newMode == viewMode) return@ViewModeSegmentedControl
                            viewModel.setViewMode(newMode)
                        }
                    )
                }
            }

            // 3. Animated Records Section (与上方折叠至顶端严格同频：300ms 同时翻页与渐变，顶端折叠完成时正好切换到目标页)
            item {
                AnimatedContent(
                    targetState = viewMode,
                    transitionSpec = {
                        val order = mapOf(ViewMode.CARDS to 0, ViewMode.STATS to 1, ViewMode.TABLE to 2)
                        val fromOrder = order[initialState] ?: 0
                        val toOrder = order[targetState] ?: 0
                        val duration = 300
                        val syncEase = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                        val slideDist = { width: Int -> (width * 0.22f).toInt() }

                        if (toOrder > fromOrder) {
                            (fadeIn(animationSpec = tween(duration, easing = LinearOutSlowInEasing)) +
                                    slideInHorizontally(animationSpec = tween(duration, easing = syncEase)) { slideDist(it) })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(duration * 2 / 3, easing = FastOutLinearInEasing)) +
                                            slideOutHorizontally(animationSpec = tween(duration, easing = syncEase)) { -slideDist(it) }
                                )
                        } else {
                            (fadeIn(animationSpec = tween(duration, easing = LinearOutSlowInEasing)) +
                                    slideInHorizontally(animationSpec = tween(duration, easing = syncEase)) { -slideDist(it) })
                                .togetherWith(
                                    fadeOut(animationSpec = tween(duration * 2 / 3, easing = FastOutLinearInEasing)) +
                                            slideOutHorizontally(animationSpec = tween(duration, easing = syncEase)) { slideDist(it) }
                                )
                        }.using(SizeTransform(clip = false) { _, _ -> tween(duration, easing = syncEase) })
                    },
                    label = "view_mode_transition"
                ) { targetMode ->
                    when (targetMode) {
                        ViewMode.CARDS -> {
                            if (filteredRecords.isEmpty()) {
                                EmptyRecordsView(onAdd = { viewModel.openAddItemDialog() })
                            } else {
                                DailyCardPager(
                                    records = filteredRecords,
                                    allRecords = allRecords,
                                    todayStr = todayStr,
                                    scrollToDateFlow = viewModel.scrollToDateEvent,
                                    onDateChanged = { currentSelectedDate = it },
                                    onShowMessage = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    onEdit = { viewModel.openEditDialog(it) },
                                    onDelete = { viewModel.promptDelete(it) },
                                    onEditPeriod = { record, period -> viewModel.openEditDialog(record, period) },
                                    onAddItem = { record, period, itemType, postMealIdx -> viewModel.openAddItemDialog(record.date, period, itemType, postMealIdx) },
                                    onDeletePostMeal = { record, period, idx -> viewModel.deletePostMealEntry(record.date, period, idx) },
                                    onDeleteSingleItem = { record, period, itemType, idx ->
                                        viewModel.deleteSingleItem(record.date, period, itemType, idx)
                                    }
                                )
                            }
                        }
                        ViewMode.STATS -> {
                            StatsView(
                                records = statsRecords,
                                allRecords = allRecords,
                                selectedRange = statsDateRange,
                                onRangeSelected = { viewModel.setStatsDateRange(it) }
                            )
                        }
                        ViewMode.TABLE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 表格日期范围快捷筛选：近7天、近14天、近30天、全部显示（与统计看板完全统一分段控制器）
                                DateRangeSelectorCapsule(
                                    selectedRange = tableDateRange,
                                    onRangeSelected = { viewModel.setTableDateRange(it) }
                                )

                                AnimatedContent(
                                    targetState = tableDateRange,
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
                                    label = "table_range_slide_transition"
                                ) { _ ->
                                    if (tableRecords.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 36.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "所选范围内暂无记录",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
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
        }
    }
}
}


    // 关怀模式专属大字实体键盘录入弹窗
    if (showCareRecordDialog) {
        val existingRec = allRecords.find { it.date == currentSelectedDate }
        CareRecordDialog(
            initialDate = currentSelectedDate,
            initialPeriod = careRecordInitialPeriod,
            initialType = careRecordInitialType,
            existingRecord = existingRec,
            allRecords = allRecords,
            onDismiss = { showCareRecordDialog = false },
            onSaveRecord = { updated, ttsText ->
                viewModel.saveRecord(updated)
                com.example.data.SystemTtsManager.speak(ttsText)
                showCareRecordDialog = false
            }
        )
    }

    // Active Dialogs
    when (val state = dialogState) {
        is DialogState.None -> Unit
        is DialogState.AddItem -> {
            AddItemDialog(
                initialDate = state.initialDate,
                initialPeriod = state.initialPeriod,
                initialItemType = state.initialItemType,
                initialPostMealIndex = state.initialPostMealIndex,
                allRecords = allRecords,
                onDismiss = { viewModel.dismissDialog() },
                onSaveItem = { date, period, preBgVal, isPreBgMod, dietText, isDietMod, medName, dose, medTiming, isMedMod, exerciseText, isExMod, postMealEntries, isPostMealMod, recordTime, isTimeManuallyEdited, keepOpen ->
                    viewModel.savePeriodItems(
                        date = date,
                        period = period,
                        preBgValue = preBgVal,
                        isPreBgModified = isPreBgMod,
                        dietText = dietText,
                        isDietModified = isDietMod,
                        medName = medName,
                        medDose = dose,
                        medTiming = medTiming,
                        isMedModified = isMedMod,
                        exerciseText = exerciseText,
                        isExerciseModified = isExMod,
                        postMealEntries = postMealEntries,
                        isPostMealModified = isPostMealMod,
                        recordTime = recordTime,
                        isTimeManuallyEdited = isTimeManuallyEdited,
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

    // Siri 仿生实时语音动效与点击启停自动填入浮层
    SiriVoiceBottomOverlay(
        isVisible = showSiriVoiceOverlay,
        isCareMode = isCareMode,
        onSaveRecord = { parsed ->
            viewModel.saveVoiceRecord(parsed)
            if (isCareMode) {
                val sb = StringBuilder("已为您记录：")
                parsed.fastingBG?.let { sb.append("空腹血糖 ${it}，") }
                parsed.postBfBG?.let { sb.append("早餐后血糖 ${it}，") }
                parsed.preLunchBG?.let { sb.append("午餐前血糖 ${it}，") }
                parsed.postLunchBG?.let { sb.append("午餐后血糖 ${it}，") }
                parsed.preDinnerBG?.let { sb.append("晚餐前血糖 ${it}，") }
                parsed.postDinnerBG?.let { sb.append("晚餐后血糖 ${it}，") }
                parsed.preNightBG?.let { sb.append("睡前血糖 ${it}，") }
                parsed.bfInsulin?.let { sb.append("早饭用药 ${it.toInt()}单位，") }
                parsed.lunchInsulin?.let { sb.append("午饭用药 ${it.toInt()}单位，") }
                parsed.dinnerInsulin?.let { sb.append("晚饭用药 ${it.toInt()}单位，") }
                parsed.bedtimeInsulin?.let { sb.append("睡前用药 ${it.toInt()}单位，") }
                sb.append("数据已成功保存。")
                com.example.data.SystemTtsManager.speak(sb.toString())
            }
        },
        onDismiss = {
            showSiriVoiceOverlay = false
            isHoldingVoice = false
        }
    )

    // 备份数据导入确认弹窗（支持应用内导入及外部用其他应用打开时还原确认）
    pendingImport?.let { importData ->
        FrostedGlassDialogOverlay(
            onDismissRequest = { viewModel.dismissImportDialog() }
        ) {
            val isDark = isSystemInDarkTheme()
            val cardBg = if (isDark) {
                Color(0xFF1E293B).copy(alpha = 0.88f)
            } else {
                Color.White.copy(alpha = 0.92f)
            }
            val cardBorder = if (isDark) {
                Color.White.copy(alpha = 0.12f)
            } else {
                Color.White.copy(alpha = 0.85f)
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TealPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            text = "发现备份数据包",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "请确认是否还原备份并选择恢复方式：",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• 合并导入：保留当前记录，自动更新重合日期的记录（推荐，安全无损）\n• 全量覆盖：清空现有全部数据，完全恢复为该备份中的记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.confirmImport(overwrite = true) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("btn_confirm_import_overwrite")
                        ) {
                            Text("全量覆盖")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = { viewModel.dismissImportDialog() }
                        ) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { viewModel.confirmImport(overwrite = false) },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            modifier = Modifier.testTag("btn_confirm_import_merge")
                        ) {
                            Text("合并导入")
                        }
                    }
                }
            }
        }
    }
    }
}

