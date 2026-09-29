package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BGUtils
import com.example.data.BackupParseResult
import com.example.data.DataBackupManager
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.data.InsulinRepository
import com.example.data.MedicationData
import com.example.data.ParsedVoiceRecord
import com.example.data.UserMedProfile
import com.example.data.VoiceRecognitionManager
import com.example.ui.components.TopTrendChartType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 界面视图展示模式：单天卡片滑动 vs 统计图表中心 vs 多日汇总表格
 */
enum class ViewMode {
    CARDS,
    STATS,
    TABLE
}

/**
 * 表格视图日期范围筛选维度
 */
enum class TableDateRange(val label: String, val days: Int?) {
    DAYS_7("近7天", 7),
    DAYS_14("近14天", 14),
    DAYS_30("近30天", 30),
    ALL("全部", null)
}

/**
 * 界面深浅色主题模式枚举
 */
enum class AppThemeMode(val title: String) {
    SYSTEM("跟随系统"),
    LIGHT("日间模式"),
    DARK("夜间模式")
}

/**
 * 记一笔单条记录条目类型
 */
enum class ItemType(val title: String, val icon: String) {
    PRE_MEAL_BG("餐前血糖", "🩸"),
    DIET("用餐情况", "🍽️"),
    MEDICATION("用药", "💊"),
    POST_MEAL_BG("餐后血糖", "🩸"),
    EXERCISE("运动记录", "🏃")
}

/**
 * 全局弹窗状态机 (DialogState)：
 * 严格控制屏幕层同时只能激活一个核心弹窗，避免状态重叠与层级竞态。
 */
sealed class DialogState {
    object None : DialogState()
    data class Edit(
        val initialRecord: InsulinRecord? = null,
        val initialDate: String? = null,
        val initialPeriod: MealPeriod? = null
    ) : DialogState()
    data class AddItem(
        val initialDate: String? = null,
        val initialPeriod: MealPeriod? = null,
        val initialItemType: ItemType? = null,
        val initialPostMealIndex: Int? = null
    ) : DialogState()
    data class ConfirmDelete(val record: InsulinRecord) : DialogState()
}

/**
 * 血糖胰岛素应用核心业务状态机 (InsulinTrackerViewModel)：
 *
 * 架构职责与边界隔离：
 * 1. 单一可信数据源 (SSOT)：通过 [InsulinRepository] 响应式连接 Room 数据库，
 *    向上层 UI 暴露不可变 [StateFlow]（包括全量记录、筛选后记录、走势图数据、表格数据）；
 * 2. 状态驱动分发：隔离 UI 渲染层与持久化数据层，所有写入操作均在 [Dispatchers.IO] 异步完成；
 * 3. 跨时段/跨日期安全更新原则：
 *    - 在 [savePeriodItems] 中执行严格的脏字段检测与局部合并更新，
 *    - 确保记录某一時段（如早餐）数据时，绝不覆盖或影响其他时段（午餐/晚餐/睡前）的既有数据；
 * 4. 边界契约与事件通知：通过不可重放的 [SharedFlow] 投递轻量级 UI 提示 (Toast) 与自动滚动事件 (scrollToDate)。
 */
class InsulinTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: InsulinRepository
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching {
            AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
        }.getOrDefault(AppThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _topTrendChartType = MutableStateFlow(
        runCatching {
            TopTrendChartType.valueOf(prefs.getString("top_trend_chart_type", TopTrendChartType.RECENT_7D.name) ?: TopTrendChartType.RECENT_7D.name)
        }.getOrDefault(TopTrendChartType.RECENT_7D)
    )
    val topTrendChartType: StateFlow<TopTrendChartType> = _topTrendChartType.asStateFlow()

    fun setTopTrendChartType(type: TopTrendChartType) {
        _topTrendChartType.value = type
        prefs.edit().putString("top_trend_chart_type", type.name).apply()
    }

    private val _isCareMode = MutableStateFlow(
        prefs.getBoolean("care_mode_enabled", false)
    )
    val isCareMode: StateFlow<Boolean> = _isCareMode.asStateFlow()

    private val _careFontSize = MutableStateFlow(
        runCatching {
            CareFontSize.valueOf(prefs.getString("care_font_size", CareFontSize.EXTRA.name) ?: CareFontSize.EXTRA.name)
        }.getOrDefault(CareFontSize.EXTRA)
    )
    val careFontSize: StateFlow<CareFontSize> = _careFontSize.asStateFlow()

    fun setCareFontSize(size: CareFontSize) {
        _careFontSize.value = size
        prefs.edit().putString("care_font_size", size.name).apply()
        viewModelScope.launch {
            _toastEvent.emit("已切换至「${size.title}」")
        }
    }

    fun toggleCareMode() {
        val next = !_isCareMode.value
        _isCareMode.value = next
        prefs.edit().putBoolean("care_mode_enabled", next).apply()
        val msg = if (next) "已开启关怀模式（大字版）" else "已退出关怀模式"
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = InsulinRepository(db.insulinDao())
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.getRecordCount() <= 7) {
                repository.insertAll(AppDatabase.INITIAL_MOCK_DATA)
            }
            repository.migrateLegacyDefaultMedNames()
            val today = BGUtils.getTodayString()
            repository.ensureTodayRecord(today)
        }
        viewModelScope.launch {
            repository.allRecords.collect { records ->
                val profile = MedicationData.getUserMedProfile(records)
                VoiceRecognitionManager.setUserMedProfile(profile)
            }
        }
    }

    val allRecords: StateFlow<List<InsulinRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userMedProfile: StateFlow<UserMedProfile> = allRecords
        .map { MedicationData.getUserMedProfile(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserMedProfile())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.CARDS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _tableDateRange = MutableStateFlow(TableDateRange.ALL)
    val tableDateRange: StateFlow<TableDateRange> = _tableDateRange.asStateFlow()

    private val _statsDateRange = MutableStateFlow(TableDateRange.DAYS_14)
    val statsDateRange: StateFlow<TableDateRange> = _statsDateRange.asStateFlow()

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.None)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    private val _pendingImport = MutableStateFlow<BackupParseResult.Success?>(null)
    val pendingImport: StateFlow<BackupParseResult.Success?> = _pendingImport.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val _scrollToDateEvent = MutableSharedFlow<String>()
    val scrollToDateEvent: SharedFlow<String> = _scrollToDateEvent.asSharedFlow()

    val filteredRecords: StateFlow<List<InsulinRecord>> = combine(allRecords, searchQuery) { records, query ->
        if (query.isBlank()) {
            records
        } else {
            val q = query.trim().lowercase()
            records.filter {
                it.date.contains(q) || it.notes.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTrendRecords: StateFlow<List<InsulinRecord>> = allRecords.combine(_searchQuery) { records, _ ->
        // Take up to 7 most recent records and sort chronologically ascending for the chart
        records.take(7).reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tableRecords: StateFlow<List<InsulinRecord>> = combine(filteredRecords, tableDateRange) { records, range ->
        if (range.days == null) {
            records
        } else {
            val cutoff = LocalDate.now().minusDays((range.days - 1).toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
            records.filter { it.date >= cutoff }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statsRecords: StateFlow<List<InsulinRecord>> = combine(filteredRecords, statsDateRange) { records, range ->
        if (range.days == null) {
            records
        } else {
            val cutoff = LocalDate.now().minusDays((range.days - 1).toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
            records.filter { it.date >= cutoff }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTableDateRange(range: TableDateRange) {
        _tableDateRange.value = range
    }

    fun setStatsDateRange(range: TableDateRange) {
        _statsDateRange.value = range
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun cycleThemeMode() {
        val next = when (_themeMode.value) {
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.SYSTEM
        }
        setThemeMode(next)
    }

    fun openAddItemDialog(
        date: String? = null,
        period: MealPeriod? = null,
        itemType: ItemType? = null,
        postMealIndex: Int? = null
    ) {
        val targetDate = date ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val targetPeriod = period ?: InsulinRecord.getPeriodForTime()
        _dialogState.value = DialogState.AddItem(
            initialDate = targetDate,
            initialPeriod = targetPeriod,
            initialItemType = itemType,
            initialPostMealIndex = postMealIndex
        )
    }

    fun openAddDialog(date: String? = null, period: MealPeriod? = null) {
        openAddItemDialog(date, period)
    }

    /**
     * 批量组合保存指定时段填写的多个条目（血糖、用药、饮食、运动等）
     */
    fun savePeriodItems(
        date: String,
        period: MealPeriod,
        preBgValue: Float? = null,
        isPreBgModified: Boolean = false,
        dietText: String = "",
        isDietModified: Boolean = false,
        medName: String = "",
        medDose: Float? = null,
        medTiming: String = "",
        isMedModified: Boolean = false,
        exerciseText: String = "",
        isExerciseModified: Boolean = false,
        postMealEntries: List<com.example.data.PostMealEntry>? = null,
        isPostMealModified: Boolean = false,
        recordTime: String = "",
        isTimeManuallyEdited: Boolean = false,
        keepDialogOpen: Boolean = false
    ) {
        viewModelScope.launch {
            val existing = allRecords.value.find { it.date == date } ?: InsulinRecord(date = date)
            var updated = existing
            val savedDescriptions = mutableListOf<String>()
            val deletedDescriptions = mutableListOf<String>()

            val finalRecordTime = recordTime.ifBlank {
                val now = java.time.LocalTime.now()
                String.format(java.util.Locale.getDefault(), "%02d:%02d", now.hour, now.minute)
            }

            // 1. 餐前血糖
            if (isPreBgModified) {
                if (preBgValue != null) {
                    val base = when (period) {
                        MealPeriod.MORNING -> updated.copy(fastingBG = preBgValue, preBfBG = preBgValue)
                        MealPeriod.LUNCH -> updated.copy(preLunchBG = preBgValue)
                        MealPeriod.DINNER -> updated.copy(preDinnerBG = preBgValue)
                        MealPeriod.NIGHT -> updated.copy(preNightBG = preBgValue)
                    }
                    val oldTime = existing.getItemTime(period, "preBG")
                    val targetTime = if (isTimeManuallyEdited) {
                        finalRecordTime
                    } else {
                        val hasPrevBg = when (period) {
                            MealPeriod.MORNING -> existing.fastingBG != null || existing.preBfBG != null
                            MealPeriod.LUNCH -> existing.preLunchBG != null
                            MealPeriod.DINNER -> existing.preDinnerBG != null
                            MealPeriod.NIGHT -> existing.preNightBG != null
                        }
                        if (hasPrevBg && oldTime.isNotBlank()) oldTime else finalRecordTime
                    }
                    updated = base.withItemTime(period, "preBG", targetTime)
                    savedDescriptions.add(if (period == MealPeriod.MORNING) "空腹血糖" else if (period == MealPeriod.NIGHT) "睡前血糖" else "餐前血糖")
                } else {
                    val base = when (period) {
                        MealPeriod.MORNING -> updated.copy(fastingBG = null, preBfBG = null)
                        MealPeriod.LUNCH -> updated.copy(preLunchBG = null)
                        MealPeriod.DINNER -> updated.copy(preDinnerBG = null)
                        MealPeriod.NIGHT -> updated.copy(preNightBG = null)
                    }
                    updated = base.withItemTime(period, "preBG", "")
                    deletedDescriptions.add(if (period == MealPeriod.MORNING) "空腹血糖" else if (period == MealPeriod.NIGHT) "睡前血糖" else "餐前血糖")
                }
            }

            // 2. 用餐情况
            if (isDietModified) {
                if (dietText.isNotBlank()) {
                    val base = when (period) {
                        MealPeriod.MORNING -> updated.copy(bfDiet = dietText.trim())
                        MealPeriod.LUNCH -> updated.copy(lunchDiet = dietText.trim())
                        MealPeriod.DINNER -> updated.copy(dinnerDiet = dietText.trim())
                        MealPeriod.NIGHT -> updated.copy(nightDiet = dietText.trim())
                    }
                    val oldTime = existing.getItemTime(period, "diet")
                    val targetTime = if (isTimeManuallyEdited) {
                        finalRecordTime
                    } else {
                        val hasPrevDiet = when (period) {
                            MealPeriod.MORNING -> existing.bfDiet.isNotBlank()
                            MealPeriod.LUNCH -> existing.lunchDiet.isNotBlank()
                            MealPeriod.DINNER -> existing.dinnerDiet.isNotBlank()
                            MealPeriod.NIGHT -> existing.nightDiet.isNotBlank()
                        }
                        if (hasPrevDiet && oldTime.isNotBlank()) oldTime else finalRecordTime
                    }
                    updated = base.withItemTime(period, "diet", targetTime)
                    savedDescriptions.add("用餐情况")
                } else {
                    val base = when (period) {
                        MealPeriod.MORNING -> updated.copy(bfDiet = "")
                        MealPeriod.LUNCH -> updated.copy(lunchDiet = "")
                        MealPeriod.DINNER -> updated.copy(dinnerDiet = "")
                        MealPeriod.NIGHT -> updated.copy(nightDiet = "")
                    }
                    updated = base.withItemTime(period, "diet", "")
                    deletedDescriptions.add("用餐情况")
                }
            }

            // 3. 用药
            if (isMedModified) {
                if (medDose != null && medDose > 0) {
                    val actualName = medName.trim().ifBlank { "胰岛素" }
                    val actualTiming = if (period == MealPeriod.NIGHT) "睡前" else medTiming.trim().ifBlank { "餐前" }
                    val base = when (period) {
                        MealPeriod.MORNING -> updated.copy(
                            bfMedName = actualName,
                            bfInsulin = medDose,
                            bfMedTiming = actualTiming
                        )
                        MealPeriod.LUNCH -> updated.copy(
                            lunchMedName = actualName,
                            lunchInsulin = medDose,
                            lunchMedTiming = actualTiming
                        )
                        MealPeriod.DINNER -> updated.copy(
                            dinnerMedName = actualName,
                            dinnerInsulin = medDose,
                            dinnerMedTiming = actualTiming
                        )
                        MealPeriod.NIGHT -> updated.copy(
                            nightMedName = actualName,
                            bedtimeInsulin = medDose,
                            nightMedTiming = actualTiming
                        )
                    }
                    val oldTime = existing.getItemTime(period, "med")
                    val targetTime = if (isTimeManuallyEdited) {
                        finalRecordTime
                    } else {
                        val hasPrevMed = when (period) {
                            MealPeriod.MORNING -> existing.bfInsulin != null && existing.bfInsulin > 0f
                            MealPeriod.LUNCH -> existing.lunchInsulin != null && existing.lunchInsulin > 0f
                            MealPeriod.DINNER -> existing.dinnerInsulin != null && existing.dinnerInsulin > 0f
                            MealPeriod.NIGHT -> existing.bedtimeInsulin != null && existing.bedtimeInsulin > 0f
                        }
                        if (hasPrevMed && oldTime.isNotBlank()) oldTime else finalRecordTime
                    }
                    updated = base.withItemTime(period, "med", targetTime)
                    savedDescriptions.add("用药")
                } else {
                    val base = when (period) {
                        MealPeriod.MORNING -> updated.copy(bfMedName = "", bfInsulin = null, bfMedTiming = "")
                        MealPeriod.LUNCH -> updated.copy(lunchMedName = "", lunchInsulin = null, lunchMedTiming = "")
                        MealPeriod.DINNER -> updated.copy(dinnerMedName = "", dinnerInsulin = null, dinnerMedTiming = "")
                        MealPeriod.NIGHT -> updated.copy(nightMedName = "", bedtimeInsulin = null, nightMedTiming = "")
                    }
                    updated = base.withItemTime(period, "med", "")
                    deletedDescriptions.add("用药")
                }
            }

            // 4. 运动
            if (isExerciseModified) {
                if (exerciseText.isNotBlank()) {
                    val base = when (period) {
                        MealPeriod.MORNING -> updated.copy(bfExercise = exerciseText.trim())
                        MealPeriod.LUNCH -> updated.copy(lunchExercise = exerciseText.trim())
                        MealPeriod.DINNER -> updated.copy(dinnerExercise = exerciseText.trim())
                        MealPeriod.NIGHT -> updated.copy(nightExercise = exerciseText.trim())
                    }
                    val oldTime = existing.getItemTime(period, "exercise")
                    val targetTime = if (isTimeManuallyEdited) {
                        finalRecordTime
                    } else {
                        val hasPrevEx = when (period) {
                            MealPeriod.MORNING -> existing.bfExercise.isNotBlank()
                            MealPeriod.LUNCH -> existing.lunchExercise.isNotBlank()
                            MealPeriod.DINNER -> existing.dinnerExercise.isNotBlank()
                            MealPeriod.NIGHT -> existing.nightExercise.isNotBlank()
                        }
                        if (hasPrevEx && oldTime.isNotBlank()) oldTime else finalRecordTime
                    }
                    updated = base.withItemTime(period, "exercise", targetTime)
                    savedDescriptions.add("运动记录")
                } else {
                    val base = when (period) {
                        MealPeriod.MORNING -> updated.copy(bfExercise = "")
                        MealPeriod.LUNCH -> updated.copy(lunchExercise = "")
                        MealPeriod.DINNER -> updated.copy(dinnerExercise = "")
                        MealPeriod.NIGHT -> updated.copy(nightExercise = "")
                    }
                    updated = base.withItemTime(period, "exercise", "")
                    deletedDescriptions.add("运动记录")
                }
            }

            // 5. 餐后血糖
            if (isPostMealModified && postMealEntries != null) {
                val sortedList = postMealEntries.sortedWith { a, b ->
                    fun parseHour(tag: String): Float {
                        if (tag.contains("半小时") || tag.contains("0.5")) return 0.5f
                        val m = Regex("""^餐后(\d+(?:\.\d+)?)(?:小时|h)$""").find(tag.trim())
                        if (m != null) return m.groupValues[1].toFloatOrNull() ?: 2.0f
                        return 99f
                    }
                    val hA = parseHour(a.tag)
                    val hB = parseHour(b.tag)
                    if (hA != hB) hA.compareTo(hB) else a.time.compareTo(b.time)
                }
                val newPrimary = sortedList.firstOrNull()?.value
                val serializedExtras = if (sortedList.isEmpty()) "" else com.example.data.PostMealUtils.serializeEntries(sortedList)
                updated = when (period) {
                    MealPeriod.MORNING -> updated.copy(postBfBG = newPrimary, postBfBGExtra = serializedExtras)
                    MealPeriod.LUNCH -> updated.copy(postLunchBG = newPrimary, postLunchBGExtra = serializedExtras)
                    MealPeriod.DINNER -> updated.copy(postDinnerBG = newPrimary, postDinnerBGExtra = serializedExtras)
                    MealPeriod.NIGHT -> updated.copy(postNightBG = newPrimary, postNightBGExtra = serializedExtras)
                }
                if (sortedList.isNotEmpty()) {
                    savedDescriptions.add("餐后血糖")
                } else if (existing.getPostMealList(period).isNotEmpty()) {
                    deletedDescriptions.add("餐后血糖")
                }
            }

            if (updated == existing) {
                if (!keepDialogOpen) {
                    dismissDialog()
                }
                return@launch
            }

            repository.insertRecord(updated)
            val msg = when {
                savedDescriptions.isNotEmpty() -> "已保存「${period.title} · ${savedDescriptions.joinToString("、")}」"
                deletedDescriptions.isNotEmpty() -> "已删除「${period.title} · ${deletedDescriptions.joinToString("、")}」"
                else -> "已更新「${period.title}」记录"
            }
            _toastEvent.emit(msg)
            _scrollToDateEvent.emit(date)
            if (!keepDialogOpen) {
                dismissDialog()
            }
        }
    }

    fun saveSingleItem(
        date: String,
        period: MealPeriod,
        itemType: ItemType,
        bgValue: Float? = null,
        dietText: String = "",
        medName: String = "",
        dose: Float? = null,
        medTiming: String = "",
        postMealTag: String = "",
        postMealTime: String = "",
        exerciseText: String = "",
        postMealIndex: Int? = null,
        keepDialogOpen: Boolean = false
    ) {
        viewModelScope.launch {
            val existing = allRecords.value.find { it.date == date } ?: InsulinRecord(date = date)
            val recordTime = postMealTime.ifBlank {
                val existingTime = when (itemType) {
                    ItemType.PRE_MEAL_BG -> existing.getItemTime(period, "preBG")
                    ItemType.DIET -> existing.getItemTime(period, "diet")
                    ItemType.EXERCISE -> existing.getItemTime(period, "exercise")
                    ItemType.MEDICATION -> existing.getItemTime(period, "med")
                    ItemType.POST_MEAL_BG -> ""
                }
                if (existingTime.isNotBlank()) existingTime else {
                    val now = java.time.LocalTime.now()
                    String.format(java.util.Locale.getDefault(), "%02d:%02d", now.hour, now.minute)
                }
            }
            var wasDeleted = false
            val updated = when (itemType) {
                ItemType.PRE_MEAL_BG -> {
                    if (bgValue != null) {
                        val base = when (period) {
                            MealPeriod.MORNING -> existing.copy(fastingBG = bgValue, preBfBG = bgValue)
                            MealPeriod.LUNCH -> existing.copy(preLunchBG = bgValue)
                            MealPeriod.DINNER -> existing.copy(preDinnerBG = bgValue)
                            MealPeriod.NIGHT -> existing.copy(preNightBG = bgValue)
                        }
                        base.withItemTime(period, "preBG", recordTime)
                    } else {
                        wasDeleted = true
                        val base = when (period) {
                            MealPeriod.MORNING -> existing.copy(fastingBG = null, preBfBG = null)
                            MealPeriod.LUNCH -> existing.copy(preLunchBG = null)
                            MealPeriod.DINNER -> existing.copy(preDinnerBG = null)
                            MealPeriod.NIGHT -> existing.copy(preNightBG = null)
                        }
                        base.withItemTime(period, "preBG", "")
                    }
                }
                ItemType.DIET -> {
                    if (dietText.isNotBlank()) {
                        val base = when (period) {
                            MealPeriod.MORNING -> existing.copy(bfDiet = dietText.trim())
                            MealPeriod.LUNCH -> existing.copy(lunchDiet = dietText.trim())
                            MealPeriod.DINNER -> existing.copy(dinnerDiet = dietText.trim())
                            MealPeriod.NIGHT -> existing.copy(nightDiet = dietText.trim())
                        }
                        base.withItemTime(period, "diet", recordTime)
                    } else {
                        wasDeleted = true
                        val base = when (period) {
                            MealPeriod.MORNING -> existing.copy(bfDiet = "")
                            MealPeriod.LUNCH -> existing.copy(lunchDiet = "")
                            MealPeriod.DINNER -> existing.copy(dinnerDiet = "")
                            MealPeriod.NIGHT -> existing.copy(nightDiet = "")
                        }
                        base.withItemTime(period, "diet", "")
                    }
                }
                ItemType.EXERCISE -> {
                    if (exerciseText.isNotBlank()) {
                        val base = when (period) {
                            MealPeriod.MORNING -> existing.copy(bfExercise = exerciseText.trim())
                            MealPeriod.LUNCH -> existing.copy(lunchExercise = exerciseText.trim())
                            MealPeriod.DINNER -> existing.copy(dinnerExercise = exerciseText.trim())
                            MealPeriod.NIGHT -> existing.copy(nightExercise = exerciseText.trim())
                        }
                        base.withItemTime(period, "exercise", recordTime)
                    } else {
                        wasDeleted = true
                        val base = when (period) {
                            MealPeriod.MORNING -> existing.copy(bfExercise = "")
                            MealPeriod.LUNCH -> existing.copy(lunchExercise = "")
                            MealPeriod.DINNER -> existing.copy(dinnerExercise = "")
                            MealPeriod.NIGHT -> existing.copy(nightExercise = "")
                        }
                        base.withItemTime(period, "exercise", "")
                    }
                }
                ItemType.MEDICATION -> {
                    if (dose != null && dose > 0) {
                        val actualName = medName.trim().ifBlank { "胰岛素" }
                        val actualTiming = if (period == MealPeriod.NIGHT) "睡前" else medTiming.trim().ifBlank { "餐前" }
                        val base = when (period) {
                            MealPeriod.MORNING -> existing.copy(
                                bfMedName = actualName,
                                bfInsulin = dose,
                                bfMedTiming = actualTiming
                            )
                            MealPeriod.LUNCH -> existing.copy(
                                lunchMedName = actualName,
                                lunchInsulin = dose,
                                lunchMedTiming = actualTiming
                            )
                            MealPeriod.DINNER -> existing.copy(
                                dinnerMedName = actualName,
                                dinnerInsulin = dose,
                                dinnerMedTiming = actualTiming
                            )
                            MealPeriod.NIGHT -> existing.copy(
                                nightMedName = actualName,
                                bedtimeInsulin = dose,
                                nightMedTiming = actualTiming
                            )
                        }
                        base.withItemTime(period, "med", recordTime)
                    } else {
                        wasDeleted = true
                        val base = when (period) {
                            MealPeriod.MORNING -> existing.copy(bfMedName = "", bfInsulin = null, bfMedTiming = "")
                            MealPeriod.LUNCH -> existing.copy(lunchMedName = "", lunchInsulin = null, lunchMedTiming = "")
                            MealPeriod.DINNER -> existing.copy(dinnerMedName = "", dinnerInsulin = null, dinnerMedTiming = "")
                            MealPeriod.NIGHT -> existing.copy(nightMedName = "", bedtimeInsulin = null, nightMedTiming = "")
                        }
                        base.withItemTime(period, "med", "")
                    }
                }
                ItemType.POST_MEAL_BG -> {
                    val currentList = existing.getPostMealList(period).toMutableList()
                    val targetIndex = if (postMealIndex != null && postMealIndex in currentList.indices) {
                        postMealIndex
                    } else {
                        val foundIdx = currentList.indexOfFirst {
                            com.example.data.PostMealUtils.isTagMatch(it.tag.ifBlank { "餐后2h" }, postMealTag)
                        }
                        if (foundIdx >= 0) foundIdx else null
                    }

                    if (targetIndex != null) {
                        if (bgValue != null) {
                            currentList[targetIndex] = com.example.data.PostMealEntry(bgValue, recordTime, postMealTag)
                        } else {
                            wasDeleted = true
                            currentList.removeAt(targetIndex)
                        }
                    } else {
                        if (bgValue != null) {
                            currentList.add(com.example.data.PostMealEntry(bgValue, recordTime, postMealTag))
                        } else {
                            wasDeleted = true
                        }
                    }
                    // 按阶段时间/小时自然排序（半小时 -> 1h -> 2h -> 3h...）
                    currentList.sortWith { a, b ->
                        fun parseHour(tag: String): Float {
                            if (tag.contains("半小时") || tag.contains("0.5")) return 0.5f
                            val m = Regex("""^餐后(\d+(?:\.\d+)?)(?:小时|h)$""").find(tag.trim())
                            if (m != null) return m.groupValues[1].toFloatOrNull() ?: 2.0f
                            return 99f
                        }
                        val hA = parseHour(a.tag)
                        val hB = parseHour(b.tag)
                        if (hA != hB) hA.compareTo(hB) else a.time.compareTo(b.time)
                    }
                    val newPrimary = currentList.firstOrNull()?.value
                    val serializedExtras = if (currentList.isEmpty()) "" else com.example.data.PostMealUtils.serializeEntries(currentList)
                    when (period) {
                        MealPeriod.MORNING -> existing.copy(postBfBG = newPrimary, postBfBGExtra = serializedExtras)
                        MealPeriod.LUNCH -> existing.copy(postLunchBG = newPrimary, postLunchBGExtra = serializedExtras)
                        MealPeriod.DINNER -> existing.copy(postDinnerBG = newPrimary, postDinnerBGExtra = serializedExtras)
                        MealPeriod.NIGHT -> existing.copy(postNightBG = newPrimary, postNightBGExtra = serializedExtras)
                    }
                }
            }
            if (updated == existing) {
                if (!keepDialogOpen) {
                    dismissDialog()
                }
                return@launch
            }
            repository.insertRecord(updated)
            val typeDesc = when (itemType) {
                ItemType.PRE_MEAL_BG -> if (period == MealPeriod.MORNING) "空腹血糖" else if (period == MealPeriod.NIGHT) "睡前血糖" else "餐前血糖"
                ItemType.DIET -> "用餐情况"
                ItemType.EXERCISE -> "运动记录"
                ItemType.MEDICATION -> "用药记录"
                ItemType.POST_MEAL_BG -> "餐后血糖"
            }
            val msg = if (wasDeleted) "已删除「${period.title} · $typeDesc」" else "已保存「${period.title} · $typeDesc」"
            _toastEvent.emit(msg)
            _scrollToDateEvent.emit(date)
            if (!keepDialogOpen) {
                dismissDialog()
            }
        }
    }

    fun deletePostMealEntry(date: String, period: MealPeriod, index: Int) {
        viewModelScope.launch {
            val record = allRecords.value.find { it.date == date } ?: return@launch
            val list = record.getPostMealList(period).toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
                val newPrimary = list.firstOrNull()?.value
                val serializedExtras = if (list.isEmpty()) "" else com.example.data.PostMealUtils.serializeEntries(list)
                val updated = when (period) {
                    MealPeriod.MORNING -> record.copy(postBfBG = newPrimary, postBfBGExtra = serializedExtras)
                    MealPeriod.LUNCH -> record.copy(postLunchBG = newPrimary, postLunchBGExtra = serializedExtras)
                    MealPeriod.DINNER -> record.copy(postDinnerBG = newPrimary, postDinnerBGExtra = serializedExtras)
                    MealPeriod.NIGHT -> record.copy(postNightBG = newPrimary, postNightBGExtra = serializedExtras)
                }
                repository.insertRecord(updated)
                _toastEvent.emit("已删除该条餐后血糖记录")
            }
        }
    }

    fun deleteSingleItem(
        date: String,
        period: MealPeriod,
        itemType: ItemType,
        postMealIndex: Int? = null
    ) {
        viewModelScope.launch {
            val record = allRecords.value.find { it.date == date } ?: return@launch
            val updated = when (itemType) {
                ItemType.PRE_MEAL_BG -> {
                    val base = when (period) {
                        MealPeriod.MORNING -> record.copy(fastingBG = null, preBfBG = null)
                        MealPeriod.LUNCH -> record.copy(preLunchBG = null)
                        MealPeriod.DINNER -> record.copy(preDinnerBG = null)
                        MealPeriod.NIGHT -> record.copy(preNightBG = null)
                    }
                    base.withItemTime(period, "preBG", "")
                }
                ItemType.MEDICATION -> {
                    val base = when (period) {
                        MealPeriod.MORNING -> record.copy(bfMedName = "", bfInsulin = null, bfMedTiming = "")
                        MealPeriod.LUNCH -> record.copy(lunchMedName = "", lunchInsulin = null, lunchMedTiming = "")
                        MealPeriod.DINNER -> record.copy(dinnerMedName = "", dinnerInsulin = null, dinnerMedTiming = "")
                        MealPeriod.NIGHT -> record.copy(nightMedName = "", bedtimeInsulin = null, nightMedTiming = "")
                    }
                    base.withItemTime(period, "med", "")
                }
                ItemType.DIET -> {
                    val base = when (period) {
                        MealPeriod.MORNING -> record.copy(bfDiet = "")
                        MealPeriod.LUNCH -> record.copy(lunchDiet = "")
                        MealPeriod.DINNER -> record.copy(dinnerDiet = "")
                        MealPeriod.NIGHT -> record.copy(nightDiet = "")
                    }
                    base.withItemTime(period, "diet", "")
                }
                ItemType.EXERCISE -> {
                    val base = when (period) {
                        MealPeriod.MORNING -> record.copy(bfExercise = "")
                        MealPeriod.LUNCH -> record.copy(lunchExercise = "")
                        MealPeriod.DINNER -> record.copy(dinnerExercise = "")
                        MealPeriod.NIGHT -> record.copy(nightExercise = "")
                    }
                    base.withItemTime(period, "exercise", "")
                }
                ItemType.POST_MEAL_BG -> {
                    val list = record.getPostMealList(period).toMutableList()
                    val idx = postMealIndex ?: (list.size - 1)
                    if (idx in list.indices) {
                        list.removeAt(idx)
                        val newPrimary = list.firstOrNull()?.value
                        val serializedExtras = if (list.isEmpty()) "" else com.example.data.PostMealUtils.serializeEntries(list)
                        when (period) {
                            MealPeriod.MORNING -> record.copy(postBfBG = newPrimary, postBfBGExtra = serializedExtras)
                            MealPeriod.LUNCH -> record.copy(postLunchBG = newPrimary, postLunchBGExtra = serializedExtras)
                            MealPeriod.DINNER -> record.copy(postDinnerBG = newPrimary, postDinnerBGExtra = serializedExtras)
                            MealPeriod.NIGHT -> record.copy(postNightBG = newPrimary, postNightBGExtra = serializedExtras)
                        }
                    } else {
                        record
                    }
                }
            }
            repository.insertRecord(updated)
            _toastEvent.emit("已删除该条记录")
        }
    }

    fun openEditDialog(record: InsulinRecord, period: MealPeriod? = null) {
        val targetPeriod = period ?: record.getNextRecommendedPeriod()
        _dialogState.value = DialogState.Edit(
            initialRecord = record,
            initialDate = record.date,
            initialPeriod = targetPeriod
        )
    }

    fun promptDelete(record: InsulinRecord) {
        _dialogState.value = DialogState.ConfirmDelete(record)
    }

    fun dismissDialog() {
        _dialogState.value = DialogState.None
    }

    fun saveRecord(record: InsulinRecord) {
        viewModelScope.launch {
            repository.insertRecord(record)
            _toastEvent.emit("${record.date} 记录已保存")
            _scrollToDateEvent.emit(record.date)
            dismissDialog()
        }
    }

    /**
     * 智能语音录入保存：增量更新用户提到的字段，未提及的字段完整保留
     */
    fun saveVoiceRecord(parsed: ParsedVoiceRecord) {
        viewModelScope.launch {
            val existing = allRecords.value.find { it.date == parsed.date }
            val merged = parsed.mergeInto(existing)
            repository.insertRecord(merged)
            val itemCount = parsed.getRecognizedItems().size
            _toastEvent.emit("已智能录入 $itemCount 项数据至 ${parsed.date} 记录")
            _scrollToDateEvent.emit(parsed.date)
        }
    }

    /**
     * 从语音识别直接跳转至完整编辑弹窗微调
     */
    fun openEditDialogFromVoice(parsed: ParsedVoiceRecord) {
        val existing = allRecords.value.find { it.date == parsed.date }
        val merged = parsed.mergeInto(existing)
        _dialogState.value = DialogState.Edit(
            initialRecord = merged,
            initialDate = merged.date,
            initialPeriod = parsed.targetPeriod ?: merged.getNextRecommendedPeriod()
        )
    }

    fun confirmDelete(record: InsulinRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
            _toastEvent.emit("${record.date} 记录已删除")
            dismissDialog()
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            repository.resetToInitialData()
            _toastEvent.emit("已恢复演示数据")
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            repository.clearAll()
            _toastEvent.emit("已清空所有记录")
        }
    }

    /**
     * 备份数据（ZIP 格式，包含所有字段完整数据，防丢失）
     */
    fun exportBackup(context: Context) {
        val records = allRecords.value
        if (records.isEmpty()) {
            viewModelScope.launch {
                _toastEvent.emit("暂无数据可备份")
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val zipFile = DataBackupManager.exportBackupZip(context, records)
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                DataBackupManager.shareBackupZip(context, zipFile, today)
            } catch (e: Exception) {
                _toastEvent.emit("备份失败: ${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    /**
     * 从用户选取的备份文件 URI 解析数据
     */
    fun importBackupFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = DataBackupManager.parseBackupFromUri(context, uri)) {
                is BackupParseResult.Success -> {
                    _pendingImport.value = result
                }
                is BackupParseResult.Error -> {
                    _toastEvent.emit(result.message)
                }
            }
        }
    }

    /**
     * 取消导入弹窗
     */
    fun dismissImportDialog() {
        _pendingImport.value = null
    }

    /**
     * 确认导入：
     * overwrite = false: 合并追加（推荐，保留已有日期，更新重合日期，无损安全）
     * overwrite = true: 全量覆盖（清空现有所有数据，完全恢复备份数据）
     */
    fun confirmImport(overwrite: Boolean) {
        val importData = _pendingImport.value ?: return
        _pendingImport.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (overwrite) {
                    repository.clearAll()
                }
                repository.insertAll(importData.records)
                val actionDesc = if (overwrite) "全量覆盖恢复" else "合并导入"
                _toastEvent.emit("已成功${actionDesc} ${importData.count} 条记录")
            } catch (e: Exception) {
                _toastEvent.emit("导入数据保存失败: ${e.localizedMessage}")
            }
        }
    }
}
