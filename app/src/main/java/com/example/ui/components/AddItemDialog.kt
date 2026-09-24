package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.data.MedCategory
import com.example.data.MedicationData
import com.example.ui.ItemType
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 极简敏捷单条记录录入弹窗：
 * - 紧凑轻盈，去除冗余大边框与说明文字
 * - 1 行 4 段时段胶囊切换，默认根据当前系统时间自动匹配
 * - 1 行 4 项条目类型选择（血糖、用餐、用药、餐后）
 * - 聚焦输入，支持即时保存与连续记多笔
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItemDialog(
    initialDate: String?,
    initialPeriod: MealPeriod? = null,
    initialItemType: ItemType? = null,
    initialPostMealIndex: Int? = null,
    allRecords: List<InsulinRecord>,
    onDismiss: () -> Unit,
    onSaveItem: (
        date: String,
        period: MealPeriod,
        itemType: ItemType,
        bgValue: Float?,
        dietText: String,
        medName: String,
        dose: Float?,
        medTiming: String,
        postMealTag: String,
        postMealTime: String,
        exerciseText: String,
        postMealIndex: Int?,
        keepDialogOpen: Boolean
    ) -> Unit
) {
    val today = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate ?: today) }

    // 系统时间自动识别当前时段
    val nowTime = remember { LocalTime.now() }
    val nowTimeStr = remember { String.format(Locale.getDefault(), "%02d:%02d", nowTime.hour, nowTime.minute) }
    val autoIdentifiedPeriod = remember { InsulinRecord.getPeriodForTime(nowTime) }

    var selectedPeriod by remember(initialPeriod) {
        mutableStateOf(initialPeriod ?: autoIdentifiedPeriod)
    }

    var selectedItemType by remember(initialItemType) {
        mutableStateOf(initialItemType ?: ItemType.PRE_MEAL_BG)
    }

    var currentPostMealIndex by remember(initialPostMealIndex) {
        mutableStateOf(initialPostMealIndex)
    }

    // 表单状态
    var bgInputText by remember { mutableStateOf("") }
    var dietInputText by remember { mutableStateOf("") }
    var exerciseNameInputText by remember { mutableStateOf("") }
    var exerciseDurationInputText by remember { mutableStateOf("") }
    var medNameInputText by remember { mutableStateOf(if (selectedPeriod == MealPeriod.NIGHT) "甘精胰岛素" else "门冬胰岛素") }
    var medDoseInputText by remember { mutableStateOf("") }
    var medTimingChoice by remember { mutableStateOf(if (selectedPeriod == MealPeriod.NIGHT) "睡前" else "餐前") }
    var selectedMedCategory by remember { mutableStateOf(if (selectedPeriod == MealPeriod.NIGHT) MedCategory.INSULIN else MedicationData.inferCategory(medNameInputText)) }
    var postMealTagChoice by remember { mutableStateOf("餐后2h") }
    var postMealTimeInputText by remember { mutableStateOf(nowTimeStr) }
    val extraDynamicPostMealTabs = remember { mutableStateListOf<String>() }

    // 历史常用药物使用频次统计（用于置顶用户最常用的药物）
    val medFrequencyMap = remember(allRecords) {
        allRecords.flatMap { record ->
            listOf(record.bfMedName, record.lunchMedName, record.dinnerMedName, record.nightMedName)
        }.filter { it.isNotBlank() && it != "胰岛素" && it != "口服药" }
        .groupingBy { it }
        .eachCount()
    }

    // 原相机拍照识别药物逻辑
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRecognizing by remember { mutableStateOf(false) }
    var recognitionMessage by remember { mutableStateOf<String?>(null) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            isRecognizing = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val image = InputImage.fromFilePath(context, currentPhotoUri!!)
                    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            isRecognizing = false
                            val result = MedicationData.matchMedicationFromOcr(visionText.text)
                            if (result.matchedName != null) {
                                selectedMedCategory = result.category
                                medNameInputText = result.matchedName
                                if (result.suggestedDose != null) {
                                    val doseStr = if (result.suggestedDose % 1f == 0f) {
                                        result.suggestedDose.toInt().toString()
                                    } else {
                                        result.suggestedDose.toString()
                                    }
                                    medDoseInputText = doseStr
                                }
                                recognitionMessage = "已识别：${result.matchedName}（${result.category.label}）"
                            } else {
                                recognitionMessage = "未匹配到列表内的药物，请手动输入药物名称"
                            }
                        }
                        .addOnFailureListener {
                            isRecognizing = false
                            recognitionMessage = "未识别到文字，请手动输入药物名称"
                        }
                } catch (_: Exception) {
                    isRecognizing = false
                    recognitionMessage = "识别失败，请手动输入药物名称"
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val newFile = File(context.cacheDir, "med_scan_${System.currentTimeMillis()}.jpg")
                val newUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)
                currentPhotoUri = newUri
                cameraLauncher.launch(newUri)
            } catch (_: Exception) {
                recognitionMessage = "启动相机失败，请手动填写"
            }
        } else {
            recognitionMessage = "需要相机权限以拍照识别药物"
        }
    }

    fun triggerCamera() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                val newFile = File(context.cacheDir, "med_scan_${System.currentTimeMillis()}.jpg")
                val newUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)
                currentPhotoUri = newUri
                cameraLauncher.launch(newUri)
            } catch (_: Exception) {
                recognitionMessage = "启动相机失败，请手动填写"
            }
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // 动态回显与载入已有数据
    fun loadExistingData(date: String, period: MealPeriod, itemType: ItemType, postMealIdx: Int?) {
        val currentRecord = allRecords.find { it.date == date }
        when (itemType) {
            ItemType.PRE_MEAL_BG -> {
                val bg = when (period) {
                    MealPeriod.MORNING -> currentRecord?.fastingBG ?: currentRecord?.preBfBG
                    MealPeriod.LUNCH -> currentRecord?.preLunchBG
                    MealPeriod.DINNER -> currentRecord?.preDinnerBG
                    MealPeriod.NIGHT -> currentRecord?.preNightBG
                }
                bgInputText = bg?.let { String.format(Locale.US, "%.1f", it) } ?: ""
            }
            ItemType.POST_MEAL_BG -> {
                val list = currentRecord?.getPostMealList(period) ?: emptyList()
                val targetEntry = if (postMealIdx != null && postMealIdx in list.indices) {
                    list[postMealIdx]
                } else {
                    list.find { com.example.data.PostMealUtils.isTagMatch(it.tag.ifBlank { "餐后2h" }, postMealTagChoice) }
                }
                if (targetEntry != null) {
                    val actualIdx = list.indexOf(targetEntry)
                    currentPostMealIndex = if (actualIdx >= 0) actualIdx else postMealIdx
                    bgInputText = String.format(Locale.US, "%.1f", targetEntry.value)
                    postMealTagChoice = com.example.data.PostMealUtils.normalizeTag(targetEntry.tag.ifBlank { "餐后2h" })
                    postMealTimeInputText = targetEntry.time.ifBlank { nowTimeStr }
                } else {
                    currentPostMealIndex = null
                    bgInputText = ""
                    postMealTimeInputText = nowTimeStr
                }
            }
            ItemType.DIET -> {
                val d = when (period) {
                    MealPeriod.MORNING -> currentRecord?.bfDiet
                    MealPeriod.LUNCH -> currentRecord?.lunchDiet
                    MealPeriod.DINNER -> currentRecord?.dinnerDiet
                    MealPeriod.NIGHT -> currentRecord?.nightDiet
                } ?: ""
                dietInputText = d
            }
            ItemType.EXERCISE -> {
                val ex = when (period) {
                    MealPeriod.MORNING -> currentRecord?.bfExercise
                    MealPeriod.LUNCH -> currentRecord?.lunchExercise
                    MealPeriod.DINNER -> currentRecord?.dinnerExercise
                    MealPeriod.NIGHT -> currentRecord?.nightExercise
                } ?: ""
                val parsed = com.example.data.parseExercise(ex)
                exerciseNameInputText = parsed.name
                exerciseDurationInputText = parsed.duration ?: ""
            }
            ItemType.MEDICATION -> {
                val (name, dose, timing) = when (period) {
                    MealPeriod.MORNING -> Triple(currentRecord?.bfMedName ?: "", currentRecord?.bfInsulin, currentRecord?.bfMedTiming ?: "")
                    MealPeriod.LUNCH -> Triple(currentRecord?.lunchMedName ?: "", currentRecord?.lunchInsulin, currentRecord?.lunchMedTiming ?: "")
                    MealPeriod.DINNER -> Triple(currentRecord?.dinnerMedName ?: "", currentRecord?.dinnerInsulin, currentRecord?.dinnerMedTiming ?: "")
                    MealPeriod.NIGHT -> Triple(currentRecord?.nightMedName ?: "", currentRecord?.bedtimeInsulin, currentRecord?.nightMedTiming ?: "")
                }
                if (dose != null && dose > 0) {
                    medDoseInputText = if (dose % 1f == 0f) dose.toInt().toString() else dose.toString()
                    medNameInputText = name.ifBlank { if (period == MealPeriod.NIGHT) "甘精胰岛素" else "门冬胰岛素" }
                    medTimingChoice = if (period == MealPeriod.NIGHT) "睡前" else if (timing.isBlank() || timing == "睡前") "餐前" else timing
                    selectedMedCategory = MedicationData.inferCategory(medNameInputText)
                } else {
                    medDoseInputText = ""
                    medNameInputText = if (period == MealPeriod.NIGHT) "甘精胰岛素" else "门冬胰岛素"
                    medTimingChoice = if (period == MealPeriod.NIGHT) "睡前" else "餐前"
                    selectedMedCategory = if (period == MealPeriod.NIGHT) MedCategory.INSULIN else MedicationData.inferCategory(medNameInputText)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadExistingData(selectedDate, selectedPeriod, selectedItemType, initialPostMealIndex)
    }

    // 切换时段联动默认药名与时机及回显已有数据
    fun onPeriodChanged(period: MealPeriod) {
        selectedPeriod = period
        if (period == MealPeriod.NIGHT) {
            if (medNameInputText.contains("门冬") || medNameInputText == "胰岛素") {
                medNameInputText = "甘精胰岛素"
            }
            medTimingChoice = "睡前"
        } else {
            if (medTimingChoice == "睡前") {
                medTimingChoice = "餐前"
            }
        }
        currentPostMealIndex = null
        extraDynamicPostMealTabs.removeAll { dynTab ->
            val list = allRecords.find { it.date == selectedDate }?.getPostMealList(period) ?: emptyList()
            list.none { com.example.data.PostMealUtils.isTagMatch(it.tag, dynTab) }
        }
        loadExistingData(selectedDate, period, selectedItemType, null)
    }

    fun onItemTypeChanged(type: ItemType) {
        selectedItemType = type
        currentPostMealIndex = null
        extraDynamicPostMealTabs.removeAll { dynTab ->
            val list = allRecords.find { it.date == selectedDate }?.getPostMealList(selectedPeriod) ?: emptyList()
            list.none { com.example.data.PostMealUtils.isTagMatch(it.tag, dynTab) }
        }
        loadExistingData(selectedDate, selectedPeriod, type, null)
    }

    // 物理返回键优先关闭日期选择器
    if (showDatePicker) {
        BackHandler {
            showDatePicker = false
        }
    }

    // 日期选择弹窗
    if (showDatePicker) {
        val initialEpoch = remember(selectedDate) {
            try {
                LocalDate.parse(selectedDate).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpoch)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val ld = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            selectedDate = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            currentPostMealIndex = null
                            loadExistingData(selectedDate, selectedPeriod, selectedItemType, null)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val currentRecord = remember(allRecords, selectedDate) { allRecords.find { it.date == selectedDate } }
    val postMealList = remember(currentRecord, selectedPeriod) { currentRecord?.getPostMealList(selectedPeriod) ?: emptyList() }

    val allPostMealTabs = remember(postMealList, extraDynamicPostMealTabs.toList()) {
        val base = mutableListOf("餐后半小时", "餐后1h", "餐后2h")
        postMealList.forEach { entry ->
            val norm = com.example.data.PostMealUtils.normalizeTag(entry.tag.ifBlank { "餐后2h" })
            if (norm != "加餐后" && base.none { com.example.data.PostMealUtils.isTagMatch(it, norm) }) {
                base.add(norm)
            }
        }
        extraDynamicPostMealTabs.forEach { dyn ->
            if (dyn != "加餐后" && base.none { com.example.data.PostMealUtils.isTagMatch(it, dyn) }) {
                base.add(dyn)
            }
        }
        base.sortWith { a, b ->
            fun parseHour(t: String): Float {
                if (t.contains("半小时") || t.contains("0.5")) return 0.5f
                val m = Regex("""^餐后(\d+(?:\.\d+)?)(?:小时|h)$""").find(t.trim())
                if (m != null) return m.groupValues[1].toFloatOrNull() ?: 2.0f
                return 99f
            }
            parseHour(a).compareTo(parseHour(b))
        }
        base
    }

    fun findEntryForTab(tab: String, list: List<com.example.data.PostMealEntry>): Pair<Int, com.example.data.PostMealEntry>? {
        val idx = list.indexOfFirst { com.example.data.PostMealUtils.isTagMatch(it.tag.ifBlank { "餐后2h" }, tab) }
        return if (idx >= 0) idx to list[idx] else null
    }

    val hasExistingData = when (selectedItemType) {
        ItemType.PRE_MEAL_BG -> {
            val v = when (selectedPeriod) {
                MealPeriod.MORNING -> currentRecord?.fastingBG ?: currentRecord?.preBfBG
                MealPeriod.LUNCH -> currentRecord?.preLunchBG
                MealPeriod.DINNER -> currentRecord?.preDinnerBG
                MealPeriod.NIGHT -> currentRecord?.preNightBG
            }
            v != null
        }
        ItemType.POST_MEAL_BG -> {
            currentPostMealIndex != null && currentPostMealIndex in postMealList.indices
        }
        ItemType.DIET -> {
            val d = when (selectedPeriod) {
                MealPeriod.MORNING -> currentRecord?.bfDiet
                MealPeriod.LUNCH -> currentRecord?.lunchDiet
                MealPeriod.DINNER -> currentRecord?.dinnerDiet
                MealPeriod.NIGHT -> currentRecord?.nightDiet
            }
            !d.isNullOrBlank()
        }
        ItemType.EXERCISE -> {
            val e = when (selectedPeriod) {
                MealPeriod.MORNING -> currentRecord?.bfExercise
                MealPeriod.LUNCH -> currentRecord?.lunchExercise
                MealPeriod.DINNER -> currentRecord?.dinnerExercise
                MealPeriod.NIGHT -> currentRecord?.nightExercise
            }
            !e.isNullOrBlank()
        }
        ItemType.MEDICATION -> {
            val dose = when (selectedPeriod) {
                MealPeriod.MORNING -> currentRecord?.bfInsulin
                MealPeriod.LUNCH -> currentRecord?.lunchInsulin
                MealPeriod.DINNER -> currentRecord?.dinnerInsulin
                MealPeriod.NIGHT -> currentRecord?.bedtimeInsulin
            }
            dose != null && dose > 0
        }
    }

    val isInputEmpty = when (selectedItemType) {
        ItemType.PRE_MEAL_BG, ItemType.POST_MEAL_BG -> bgInputText.trim().isEmpty()
        ItemType.DIET -> dietInputText.trim().isEmpty()
        ItemType.EXERCISE -> exerciseNameInputText.trim().isEmpty() && exerciseDurationInputText.trim().isEmpty()
        ItemType.MEDICATION -> medDoseInputText.trim().isEmpty()
    }

    val isInputValid = when (selectedItemType) {
        ItemType.PRE_MEAL_BG -> {
            val v = bgInputText.trim().toFloatOrNull()
            v != null && v in 0.5f..35.0f
        }
        ItemType.POST_MEAL_BG -> {
            val v = bgInputText.trim().toFloatOrNull()
            v != null && v in 0.5f..35.0f
        }
        ItemType.MEDICATION -> {
            val d = medDoseInputText.trim().toFloatOrNull()
            d != null && d > 0f
        }
        ItemType.DIET -> {
            dietInputText.trim().isNotBlank()
        }
        ItemType.EXERCISE -> {
            exerciseNameInputText.trim().isNotBlank() || exerciseDurationInputText.trim().isNotBlank()
        }
    }

    val canSubmit = isInputValid || (hasExistingData && isInputEmpty)

    fun submit(keepOpen: Boolean) {
        if (!canSubmit) return
        val formattedExercise = when {
            exerciseNameInputText.isNotBlank() && exerciseDurationInputText.isNotBlank() ->
                "${exerciseNameInputText.trim()} ${exerciseDurationInputText.trim()}分钟"
            exerciseNameInputText.isNotBlank() -> exerciseNameInputText.trim()
            exerciseDurationInputText.isNotBlank() -> "${exerciseDurationInputText.trim()}分钟"
            else -> ""
        }
        val recordTime = if (selectedItemType == ItemType.POST_MEAL_BG) {
            postMealTimeInputText.trim().ifBlank { nowTimeStr }
        } else nowTimeStr
        val targetIdx = if (selectedItemType == ItemType.POST_MEAL_BG) {
            currentPostMealIndex
        } else null

        val actualTiming = if (selectedPeriod == MealPeriod.NIGHT) "睡前" else medTimingChoice

        onSaveItem(
            selectedDate,
            selectedPeriod,
            selectedItemType,
            bgInputText.trim().toFloatOrNull(),
            dietInputText.trim(),
            medNameInputText.trim(),
            medDoseInputText.trim().toFloatOrNull(),
            actualTiming,
            postMealTagChoice,
            recordTime,
            formattedExercise,
            targetIdx,
            keepOpen
        )
        if (keepOpen) {
            bgInputText = ""
            dietInputText = ""
            medDoseInputText = ""
            exerciseNameInputText = ""
            exerciseDurationInputText = ""
            currentPostMealIndex = null
            extraDynamicPostMealTabs.clear()
        }
    }

    FrostedGlassDialogOverlay(
        onDismissRequest = onDismiss,
        dismissOnBackPress = !showDatePicker
    ) { dismissWithAnimation ->
        val isDark = AppThemeColors.isDark
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {}
                .testTag("add_item_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF131D2A).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.96f)
            ),
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 紧凑顶栏：标题 + 日期轻标签 + 关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "记一笔",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // 可点击更换日期的轻量胶囊
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { showDatePicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (selectedDate == today) "今天 ($nowTimeStr)" else selectedDate.substring(5),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { dismissWithAnimation() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. 时段切换分段胶囊（1 行极简设计，默认匹配当前系统时间）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MealPeriod.entries.forEach { period ->
                        val isSelected = period == selectedPeriod
                        val periodTheme = when (period) {
                            MealPeriod.MORNING -> AppThemeColors.breakfastColor
                            MealPeriod.LUNCH -> AppThemeColors.lunchColor
                            MealPeriod.DINNER -> AppThemeColors.dinnerColor
                            MealPeriod.NIGHT -> AppThemeColors.bedtimeColor
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) periodTheme else Color.Transparent)
                                .clickable { onPeriodChanged(period) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${period.iconText} ${period.title}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. 条目类型切换分段胶囊（1 行 5 项极简设计）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ItemType.entries.forEach { itemType ->
                        val isSelected = itemType == selectedItemType
                        val (icon, label) = when (itemType) {
                            ItemType.PRE_MEAL_BG -> "🩸" to if (selectedPeriod == MealPeriod.MORNING) "空腹" else if (selectedPeriod == MealPeriod.NIGHT) "睡前" else "餐前"
                            ItemType.DIET -> "🍽️" to "用餐"
                            ItemType.MEDICATION -> "💊" to "用药"
                            ItemType.POST_MEAL_BG -> "📈" to "餐后"
                            ItemType.EXERCISE -> "🏃" to "运动"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) TealPrimary else Color.Transparent)
                                .clickable { onItemTypeChanged(itemType) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$icon $label",
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 4. 内容表单区（简洁直观，无冗余说明）
                when (selectedItemType) {
                    ItemType.PRE_MEAL_BG -> {
                        val bgTitle = if (selectedPeriod == MealPeriod.MORNING) "空腹血糖" else if (selectedPeriod == MealPeriod.NIGHT) "睡前血糖" else "餐前血糖"
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = bgInputText,
                                    onValueChange = { bgInputText = it },
                                    label = { Text(bgTitle) },
                                    placeholder = { Text("例: 6.0") },
                                    trailingIcon = { Text("mmol/L", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                // 微调步长按钮
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.clickable {
                                             val cur = bgInputText.toFloatOrNull() ?: 6.0f
                                            bgInputText = String.format(Locale.US, "%.1f", (cur - 0.1f).coerceAtLeast(0.5f))
                                        }
                                    ) {
                                        Text("-0.1", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.clickable {
                                            val cur = bgInputText.toFloatOrNull() ?: 6.0f
                                            bgInputText = String.format(Locale.US, "%.1f", cur + 0.1f)
                                        }
                                    ) {
                                        Text("+0.1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
                                    }
                                }
                            }
                        }
                    }

                    ItemType.POST_MEAL_BG -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 动态餐后阶段选项卡（餐后半小时、餐后1h、餐后2h 及 + 新增按钮）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                allPostMealTabs.forEach { tab ->
                                    val isSelected = com.example.data.PostMealUtils.isTagMatch(postMealTagChoice, tab)
                                    val match = findEntryForTab(tab, postMealList)
                                    val hasData = match != null

                                    val bgColor = when {
                                        isSelected -> TealPrimary
                                        hasData -> if (AppThemeColors.isDark) Color(0xFF134E4A).copy(alpha = 0.65f) else Color(0xFFE6F4EA)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                    val border = when {
                                        isSelected -> null
                                        hasData -> BorderStroke(1.dp, if (AppThemeColors.isDark) Color(0xFF2DD4BF).copy(alpha = 0.55f) else TealPrimary.copy(alpha = 0.5f))
                                        else -> null
                                    }
                                    val textColor = when {
                                        isSelected -> Color.White
                                        hasData -> if (AppThemeColors.isDark) Color(0xFF2DD4BF) else TealPrimary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = bgColor,
                                        border = border,
                                        modifier = Modifier.clickable {
                                            val prevChoice = postMealTagChoice
                                            postMealTagChoice = tab
                                            if (match != null) {
                                                currentPostMealIndex = match.first
                                                bgInputText = String.format(Locale.US, "%.1f", match.second.value)
                                                postMealTimeInputText = match.second.time.ifBlank { nowTimeStr }
                                            } else {
                                                currentPostMealIndex = null
                                                bgInputText = ""
                                                postMealTimeInputText = nowTimeStr
                                            }
                                            // 若离开的上一个选项卡是本次临时新增且未保存数据的，切换后立即清除
                                            if (prevChoice != tab && extraDynamicPostMealTabs.contains(prevChoice) &&
                                                postMealList.none { com.example.data.PostMealUtils.isTagMatch(it.tag, prevChoice) }) {
                                                extraDynamicPostMealTabs.remove(prevChoice)
                                            }
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            if (hasData && !isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (AppThemeColors.isDark) Color(0xFF2DD4BF) else TealPrimary)
                                                )
                                            }
                                            Text(
                                                text = tab,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected || hasData) FontWeight.Bold else FontWeight.Medium,
                                                color = textColor
                                            )
                                        }
                                    }
                                }

                                // "+ 新增" 按钮（默认向后推延 1h）
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        val hours = allPostMealTabs.mapNotNull { t ->
                                            val m = Regex("""^餐后(\d+)h$""").find(com.example.data.PostMealUtils.normalizeTag(t))
                                            m?.groupValues?.get(1)?.toIntOrNull()
                                        }
                                        val maxHour = (hours.maxOrNull() ?: 2).coerceAtLeast(2)
                                        val nextTab = "餐后${maxHour + 1}h"
                                        if (!extraDynamicPostMealTabs.contains(nextTab)) {
                                            extraDynamicPostMealTabs.add(nextTab)
                                        }
                                        postMealTagChoice = nextTab
                                        currentPostMealIndex = null
                                        bgInputText = ""
                                        postMealTimeInputText = nowTimeStr
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "新增",
                                            tint = TealPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "新增",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TealPrimary
                                        )
                                    }
                                }
                            }

                            // 数值输入框
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = bgInputText,
                                    onValueChange = { bgInputText = it },
                                    label = { Text("餐后血糖数值") },
                                    placeholder = { Text("例: 7.8") },
                                    trailingIcon = { Text("mmol/L", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.clickable {
                                            val cur = bgInputText.toFloatOrNull() ?: 7.5f
                                            bgInputText = String.format(Locale.US, "%.1f", (cur - 0.1f).coerceAtLeast(0.5f))
                                        }
                                    ) {
                                        Text("-0.1", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.clickable {
                                            val cur = bgInputText.toFloatOrNull() ?: 7.5f
                                            bgInputText = String.format(Locale.US, "%.1f", cur + 0.1f)
                                        }
                                    ) {
                                        Text("+0.1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
                                    }
                                }
                            }

                            // 标准参考提示（测量时间已由系统根据记录点自动标记）
                            Text(
                                text = "💡 标准参考: 餐后2h ≤ 10.0 mmol/L",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                            )
                        }
                    }

                    ItemType.DIET -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dietInputText,
                                onValueChange = { dietInputText = it },
                                label = { Text("吃了什么？") },
                                placeholder = { Text("如：全麦面包、水煮蛋1个、纯牛奶") },
                                minLines = 2,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    ItemType.EXERCISE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = exerciseNameInputText,
                                onValueChange = { exerciseNameInputText = it },
                                label = { Text("运动项目") },
                                placeholder = { Text("例: 散步、慢跑、太极拳") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = exerciseDurationInputText,
                                onValueChange = { exerciseDurationInputText = it },
                                label = { Text("运动时长") },
                                placeholder = { Text("例: 30") },
                                trailingIcon = {
                                    Text(
                                        text = "分钟",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    ItemType.MEDICATION -> {
                        var medDropdownExpanded by remember { mutableStateOf(false) }
                        var medNameFocused by remember { mutableStateOf(false) }
                        var medDoseFocused by remember { mutableStateOf(false) }

                        val unit = MedicationData.detectUnit(medNameInputText, selectedMedCategory)

                        // 综合候选药物列表：优先按历史频次排序置顶常用药，同时包含该类别基础药，通用占位放末尾
                        val currentMedList = remember(selectedMedCategory, allRecords, medFrequencyMap) {
                            val baseList = if (selectedMedCategory == MedCategory.INSULIN) {
                                MedicationData.commonInsulinMeds
                            } else {
                                MedicationData.commonOralMeds
                            }
                            val historyMeds = allRecords.flatMap { record ->
                                listOf(record.bfMedName, record.lunchMedName, record.dinnerMedName, record.nightMedName)
                            }.filter { it.isNotBlank() && it != "胰岛素" && it != "口服药" }
                            .distinct()
                            .filter { med ->
                                MedicationData.inferCategory(med) == selectedMedCategory && med !in baseList
                            }

                            val combined = (baseList + historyMeds).distinct()
                            combined.sortedWith(
                                compareByDescending<String> { med ->
                                    if (med == "胰岛素" || med == "口服药") -1 else (medFrequencyMap[med] ?: 0)
                                }.thenBy { med ->
                                    val idx = baseList.indexOf(med)
                                    if (idx >= 0) idx else 999
                                }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 1. 药物大类选择：胰岛素 vs 口服药 两个按钮
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(MedCategory.INSULIN, MedCategory.ORAL).forEach { cat ->
                                    val isSel = selectedMedCategory == cat
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) TealPrimary else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedMedCategory = cat
                                                if (cat == MedCategory.INSULIN && (medNameInputText.isBlank() || medNameInputText in MedicationData.commonOralMeds)) {
                                                    medNameInputText = if (selectedPeriod == MealPeriod.NIGHT) "甘精胰岛素" else "门冬胰岛素"
                                                } else if (cat == MedCategory.ORAL && (medNameInputText.isBlank() || medNameInputText in MedicationData.commonInsulinMeds)) {
                                                    medNameInputText = "二甲双胍"
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 7.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = cat.icon, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = cat.label,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. 药名输入框（向左收缩） + 拍照识药按钮（右侧平齐）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 药名输入框（支持下拉与自由输入）
                                Box(modifier = Modifier.weight(1f)) {
                                    BasicTextField(
                                        value = medNameInputText,
                                        onValueChange = {
                                            medNameInputText = it
                                            selectedMedCategory = MedicationData.inferCategory(it)
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .onFocusChanged { medNameFocused = it.isFocused },
                                        decorationBox = { innerTextField ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .border(
                                                        width = if (medNameFocused) 1.5.dp else 1.dp,
                                                        color = if (medNameFocused) TealPrimary else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .background(
                                                        color = if (AppThemeColors.isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(start = 12.dp, end = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    if (medNameInputText.isEmpty()) {
                                                        Text(
                                                            text = if (selectedMedCategory == MedCategory.INSULIN) "例: 门冬胰岛素" else "例: 二甲双胍",
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                                IconButton(
                                                    onClick = { medDropdownExpanded = true },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDropDown,
                                                        contentDescription = "选择药物",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    // 现代化优化下拉菜单：毛玻璃质感、圆角优雅、常用药置顶标注
                                    DropdownMenu(
                                        expanded = medDropdownExpanded,
                                        onDismissRequest = { medDropdownExpanded = false },
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = if (AppThemeColors.isDark) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .widthIn(min = 220.dp, max = 290.dp)
                                            .heightIn(max = 320.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (selectedMedCategory == MedCategory.INSULIN) "💉 常用胰岛素" else "💊 常用口服药",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "高频置顶",
                                                fontSize = 10.sp,
                                                color = TealPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            thickness = 0.5.dp
                                        )
                                        currentMedList.forEach { med ->
                                            val isCurrent = medNameInputText == med
                                            val freq = medFrequencyMap[med] ?: 0
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = med,
                                                                fontSize = 13.sp,
                                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isCurrent) TealPrimary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (freq > 0) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = TealPrimary.copy(alpha = 0.12f)
                                                                ) {
                                                                    Text(
                                                                        text = "常用 · ${freq}次",
                                                                        fontSize = 9.5.sp,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = TealPrimary,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        if (isCurrent) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "已选",
                                                                tint = TealPrimary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    medNameInputText = med
                                                    selectedMedCategory = MedicationData.inferCategory(med)
                                                    medDropdownExpanded = false
                                                },
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isCurrent) TealPrimary.copy(alpha = 0.08f) else Color.Transparent)
                                            )
                                        }
                                    }
                                }

                                // 右侧：拍照识药功能按钮（与药名输入框 50.dp 严格等高）
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TealPrimary.copy(alpha = if (AppThemeColors.isDark) 0.18f else 0.1f),
                                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .clickable { triggerCamera() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "拍照识药",
                                            tint = TealPrimary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "拍照识药",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TealPrimary
                                        )
                                    }
                                }
                            }

                            // OCR 识别状态或提示
                            if (isRecognizing) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TealPrimary.copy(alpha = 0.08f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "🔍 正在拍照识别药盒文字...",
                                        fontSize = 11.5.sp,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (!recognitionMessage.isNullOrBlank()) {
                                val isSuccess = recognitionMessage!!.startsWith("已识别")
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSuccess) TealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = recognitionMessage!!,
                                            fontSize = 11.5.sp,
                                            color = if (isSuccess) TealPrimary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "关闭",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { recognitionMessage = null }
                                        )
                                    }
                                }
                            }

                            // 3. 常见/高频药物快速点选胶囊（根据用户使用频次降序置顶排列）
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(currentMedList.take(6)) { med ->
                                    val isCurrent = medNameInputText == med
                                    val freq = medFrequencyMap[med] ?: 0
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = if (isCurrent) BorderStroke(1.dp, TealPrimary) else null,
                                        modifier = Modifier.clickable {
                                            medNameInputText = med
                                            selectedMedCategory = MedicationData.inferCategory(med)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            if (freq > 0) {
                                                Text("⭐", fontSize = 8.5.sp)
                                            }
                                            Text(
                                                text = med,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. 剂量与时机（严格等高 50.dp 平齐，且睡前时段彻底隐藏无意义的餐前/餐中/餐后）
                            if (selectedPeriod != MealPeriod.NIGHT) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 用药剂量输入框（高度 50.dp，圆角 12.dp）
                                    BasicTextField(
                                        value = medDoseInputText,
                                        onValueChange = { medDoseInputText = it },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        textStyle = TextStyle(
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .onFocusChanged { medDoseFocused = it.isFocused },
                                        decorationBox = { innerTextField ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .border(
                                                        width = if (medDoseFocused) 1.5.dp else 1.dp,
                                                        color = if (medDoseFocused) TealPrimary else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .background(
                                                        color = if (AppThemeColors.isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    if (medDoseInputText.isEmpty()) {
                                                        Text(
                                                            text = "用药剂量",
                                                            fontSize = 13.5.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                                Text(
                                                    text = unit,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TealPrimary
                                                )
                                            }
                                        }
                                    )

                                    // 时机选择（餐前、餐中、餐后，高度严格 50.dp，与左侧剂量框上下完全齐平）
                                    Row(
                                        modifier = Modifier
                                            .height(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf("餐前", "餐中", "餐后").forEach { timing ->
                                            val isSel = medTimingChoice == timing
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(if (isSel) TealPrimary else Color.Transparent)
                                                    .clickable { medTimingChoice = timing }
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = timing,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // 睡前时段：无餐食，无须餐前/中/后时机，剂量框直接通栏铺满
                                BasicTextField(
                                    value = medDoseInputText,
                                    onValueChange = { medDoseInputText = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = TextStyle(
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .onFocusChanged { medDoseFocused = it.isFocused },
                                    decorationBox = { innerTextField ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .border(
                                                    width = if (medDoseFocused) 1.5.dp else 1.dp,
                                                    color = if (medDoseFocused) TealPrimary else MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .background(
                                                    color = if (AppThemeColors.isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (medDoseInputText.isEmpty()) {
                                                    Text(
                                                        text = "睡前用药剂量",
                                                        fontSize = 13.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                }
                                                innerTextField()
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = AppThemeColors.bedtimeColor.copy(alpha = 0.12f),
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Text(
                                                    text = "🌙 睡前",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AppThemeColors.bedtimeColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = unit,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TealPrimary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 5. 底部操作栏（极简单按钮）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val submitButtonText = if (hasExistingData) "保存修改" else "保存条目"
                    Button(
                        onClick = { submit(keepOpen = false) },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(submitButtonText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
