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
import com.example.data.ParsedVoiceRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ViewMode {
    CARDS,
    TABLE
}

enum class TableDateRange(val label: String, val days: Int?) {
    DAYS_7("近7天", 7),
    DAYS_14("近14天", 14),
    DAYS_30("近30天", 30),
    ALL("全部", null)
}

enum class AppThemeMode(val title: String) {
    SYSTEM("跟随系统"),
    LIGHT("日间模式"),
    DARK("夜间模式")
}

enum class ItemType(val title: String, val icon: String) {
    PRE_MEAL_BG("餐前血糖", "🩸"),
    DIET("用餐情况", "🍽️"),
    MEDICATION("用药", "💊"),
    POST_MEAL_BG("餐后血糖", "🩸"),
    EXERCISE("运动记录", "🏃")
}

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

class InsulinTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: InsulinRepository
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching {
            AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
        }.getOrDefault(AppThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

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
    }

    val allRecords: StateFlow<List<InsulinRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.CARDS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _tableDateRange = MutableStateFlow(TableDateRange.ALL)
    val tableDateRange: StateFlow<TableDateRange> = _tableDateRange.asStateFlow()

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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTableDateRange(range: TableDateRange) {
        _tableDateRange.value = range
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
