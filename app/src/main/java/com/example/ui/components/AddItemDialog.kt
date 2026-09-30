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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
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
 * 极简敏捷单条记录录入弹窗 (AddItemDialog)：
 *
 * 架构职责与边界隔离：
 * 1. 敏捷快速记一笔：聚焦单餐段特定条目（血糖、餐食、用药、运动、餐后），提供即时记录与即时保存；
 * 2. 状态自治隔离：
 *    - 弹窗内拥有独立的输入草稿状态与修改脏标记快照（isPreBgModified、isDietModified、isMedModified 等），
 *    - 避免未保存的草稿对全局数据产生任何脏写入或副作用；
 * 3. 契约化输出回调：
 *    - 用户确认保存时，通过单一高内聚的回调 [onSaveItem] 将结构化更新项原子回传至 ViewModel；
 *    - 外部通过参数契约隔离，内部实现完全黑盒化，避免改动内部逻辑牵连上层或其他界面；
 * 4. 共享组件复用：
 *    - 复用 [DateTimePickerDialog]、[MealPeriodSelectorCapsule]、[MedCategorySelectorCapsule]、
 *      [QuickMedChips]、[OcrStatusBanner]、[StepAdjustButtons]，彻底解除与编辑弹窗间的双重冗余维护。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItemDialog(
    initialDate: String?,
    initialPeriod: MealPeriod? = null,
    initialItemType: ItemType? = null,
    initialPostMealIndex: Int? = null,
    initialDietIndex: Int? = null,
    allRecords: List<InsulinRecord>,
    onDismiss: () -> Unit,
    onSaveItem: (
        date: String,
        period: MealPeriod,
        preBgValue: Float?,
        isPreBgModified: Boolean,
        dietText: String,
        isDietModified: Boolean,
        medName: String,
        dose: Float?,
        medTiming: String,
        isMedModified: Boolean,
        exerciseText: String,
        isExerciseModified: Boolean,
        postMealEntries: List<com.example.data.PostMealEntry>?,
        isPostMealModified: Boolean,
        recordTime: String,
        isTimeManuallyEdited: Boolean,
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

    var currentDietIndex by remember(initialDietIndex) {
        mutableStateOf(initialDietIndex)
    }

    // 表单状态：解耦餐前与餐后血糖，记忆每个选项卡数据
    var preBgInputText by remember { mutableStateOf("") }
    var postBgInputText by remember { mutableStateOf("") }
    val postMealInputs = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    val postMealTimes = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    var dietInputText by remember { mutableStateOf("") }
    var dietTagChoice by remember { mutableStateOf("正餐") }
    val dietInputs = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    val dietTimes = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    val extraDynamicDietTabs = remember { mutableStateListOf<String>() }
    var exerciseNameInputText by remember { mutableStateOf("") }
    var exerciseDurationInputText by remember { mutableStateOf("") }
    var medNameInputText by remember { mutableStateOf(if (selectedPeriod == MealPeriod.NIGHT) "甘精胰岛素" else "门冬胰岛素") }
    var medDoseInputText by remember { mutableStateOf("") }
    var medTimingChoice by remember { mutableStateOf(if (selectedPeriod == MealPeriod.NIGHT) "睡前" else "餐前") }
    var selectedMedCategory by remember { mutableStateOf(if (selectedPeriod == MealPeriod.NIGHT) MedCategory.INSULIN else MedicationData.inferCategory(medNameInputText)) }
    var postMealTagChoice by remember { mutableStateOf("餐后2h") }
    var postMealTimeInputText by remember { mutableStateOf(nowTimeStr) }
    var currentItemTime by remember { mutableStateOf(nowTimeStr) }
    var isTimeManuallyEdited by remember { mutableStateOf(false) }
    val extraDynamicPostMealTabs = remember { mutableStateListOf<String>() }

    // 初始状态快照，用于精确检测哪些项目被修改/新增/删除
    var initialPreBg by remember { mutableStateOf<Float?>(null) }
    var initialDiet by remember { mutableStateOf("") }
    val initialDietMap = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    var initialExercise by remember { mutableStateOf("") }
    var initialMedDose by remember { mutableStateOf<Float?>(null) }
    var initialMedName by remember { mutableStateOf("") }
    var initialMedTiming by remember { mutableStateOf("") }
    val initialPostMealMap = remember { androidx.compose.runtime.mutableStateMapOf<String, Float>() }
    val periodDrafts = remember(selectedDate) { mutableMapOf<MealPeriod, PeriodDraftState>() }

    // 历史真实用药频次统计（精确统计有效记录，包含用户真实使用的“胰岛素”或具体药名）
    val medFrequencyMap = remember(allRecords) {
        val usedMeds = mutableListOf<String>()
        allRecords.forEach { record ->
            if (record.bfInsulin != null && record.bfInsulin > 0f && record.bfMedName.isNotBlank()) {
                usedMeds.add(record.bfMedName.trim())
            }
            if (record.lunchInsulin != null && record.lunchInsulin > 0f && record.lunchMedName.isNotBlank()) {
                usedMeds.add(record.lunchMedName.trim())
            }
            if (record.dinnerInsulin != null && record.dinnerInsulin > 0f && record.dinnerMedName.isNotBlank()) {
                usedMeds.add(record.dinnerMedName.trim())
            }
            if (record.bedtimeInsulin != null && record.bedtimeInsulin > 0f && record.nightMedName.isNotBlank()) {
                usedMeds.add(record.nightMedName.trim())
            }
        }
        usedMeds.groupingBy { it }.eachCount()
    }

    val topInsulinMed = remember(medFrequencyMap) {
        medFrequencyMap.filterKeys {
            MedicationData.inferCategory(it) == MedCategory.INSULIN
        }.maxByOrNull { it.value }?.key
    }

    // 原相机拍照识别药物逻辑
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRecognizing by remember { mutableStateOf(false) }
    var recognitionMessage by remember { mutableStateOf<String?>(null) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourcePicker by remember { mutableStateOf(false) }

    fun processImageForMedication(uri: Uri) {
        isRecognizing = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val image = InputImage.fromFilePath(context, uri)
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
                            if (result.suggestedTiming != null && selectedPeriod != MealPeriod.NIGHT) {
                                medTimingChoice = result.suggestedTiming
                            }
                            val timingPart = if (result.suggestedTiming != null) "·${result.suggestedTiming}" else ""
                            val unitStr = MedicationData.detectUnit(result.matchedName, result.category)
                            val dosePart = if (result.suggestedDose != null) " ${if (result.suggestedDose % 1f == 0f) result.suggestedDose.toInt() else result.suggestedDose}$unitStr" else ""
                            val bgPart = if (result.detectedBG != null) " (检测到血糖 ${result.detectedBG} mmol/L)" else ""
                            recognitionMessage = "已识别：${result.matchedName}$dosePart$timingPart（${result.category.label}）$bgPart"
                        } else {
                            if (result.detectedBG != null) {
                                recognitionMessage = "未匹配到药物，但检测到血糖：${result.detectedBG} mmol/L"
                            } else {
                                recognitionMessage = "未匹配到列表内的药物，请手动输入药物名称"
                            }
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

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            processImageForMedication(currentPhotoUri!!)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            processImageForMedication(uri)
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

    // 动态回显与载入指定餐段的全部已有数据
    fun loadExistingDataForPeriod(date: String, period: MealPeriod, postMealIdx: Int? = null, dietIdx: Int? = null) {
        val currentRecord = allRecords.find { it.date == date }

        // 1. 餐前血糖
        val bg = when (period) {
            MealPeriod.MORNING -> currentRecord?.fastingBG ?: currentRecord?.preBfBG
            MealPeriod.LUNCH -> currentRecord?.preLunchBG
            MealPeriod.DINNER -> currentRecord?.preDinnerBG
            MealPeriod.NIGHT -> currentRecord?.preNightBG
        }
        preBgInputText = bg?.let { String.format(Locale.US, "%.1f", it) } ?: ""
        initialPreBg = bg

        // 2. 餐后血糖列表
        val list = currentRecord?.getPostMealList(period) ?: emptyList()
        postMealInputs.clear()
        postMealTimes.clear()
        initialPostMealMap.clear()
        list.forEach { entry ->
            val normTag = com.example.data.PostMealUtils.normalizeTag(entry.tag.ifBlank { "餐后2h" })
            val valStr = String.format(Locale.US, "%.1f", entry.value)
            postMealInputs[normTag] = valStr
            postMealTimes[normTag] = entry.time.ifBlank { nowTimeStr }
            initialPostMealMap[normTag] = entry.value
        }

        val targetEntry = if (postMealIdx != null && postMealIdx in list.indices) {
            list[postMealIdx]
        } else {
            list.find { com.example.data.PostMealUtils.isTagMatch(it.tag.ifBlank { "餐后2h" }, postMealTagChoice) }
        }
        if (targetEntry != null) {
            val actualIdx = list.indexOf(targetEntry)
            currentPostMealIndex = if (actualIdx >= 0) actualIdx else postMealIdx
            postMealTagChoice = com.example.data.PostMealUtils.normalizeTag(targetEntry.tag.ifBlank { "餐后2h" })
            postBgInputText = String.format(Locale.US, "%.1f", targetEntry.value)
            postMealTimeInputText = targetEntry.time.ifBlank { nowTimeStr }
        } else {
            currentPostMealIndex = null
            postBgInputText = postMealInputs[postMealTagChoice] ?: ""
            postMealTimeInputText = postMealTimes[postMealTagChoice] ?: nowTimeStr
        }

        // 3. 用餐（多条目及加餐支持）
        val d = when (period) {
            MealPeriod.MORNING -> currentRecord?.bfDiet
            MealPeriod.LUNCH -> currentRecord?.lunchDiet
            MealPeriod.DINNER -> currentRecord?.dinnerDiet
            MealPeriod.NIGHT -> currentRecord?.nightDiet
        } ?: ""
        initialDiet = d
        val currentDietList = currentRecord?.getDietList(period) ?: emptyList()
        dietInputs.clear()
        dietTimes.clear()
        initialDietMap.clear()
        currentDietList.forEach { entry ->
            val tag = entry.tag.ifBlank { "正餐" }
            dietInputs[tag] = entry.content
            dietTimes[tag] = entry.time.ifBlank { nowTimeStr }
            initialDietMap[tag] = entry.content
        }
        val targetDietEntry = if (dietIdx != null && dietIdx in currentDietList.indices) {
            currentDietList[dietIdx]
        } else {
            currentDietList.find { it.tag == dietTagChoice } ?: currentDietList.firstOrNull()
        }
        if (targetDietEntry != null) {
            val actualIdx = currentDietList.indexOf(targetDietEntry)
            currentDietIndex = if (actualIdx >= 0) actualIdx else dietIdx
            dietTagChoice = targetDietEntry.tag.ifBlank { "正餐" }
            dietInputText = targetDietEntry.content
        } else {
            currentDietIndex = null
            dietTagChoice = "正餐"
            dietInputText = dietInputs["正餐"] ?: ""
        }

        // 4. 运动
        val ex = when (period) {
            MealPeriod.MORNING -> currentRecord?.bfExercise
            MealPeriod.LUNCH -> currentRecord?.lunchExercise
            MealPeriod.DINNER -> currentRecord?.dinnerExercise
            MealPeriod.NIGHT -> currentRecord?.nightExercise
        } ?: ""
        val parsed = com.example.data.parseExercise(ex)
        exerciseNameInputText = parsed.name
        exerciseDurationInputText = parsed.duration ?: ""
        initialExercise = ex

        // 5. 用药
        val (name, dose, timing) = when (period) {
            MealPeriod.MORNING -> Triple(currentRecord?.bfMedName ?: "", currentRecord?.bfInsulin, currentRecord?.bfMedTiming ?: "")
            MealPeriod.LUNCH -> Triple(currentRecord?.lunchMedName ?: "", currentRecord?.lunchInsulin, currentRecord?.lunchMedTiming ?: "")
            MealPeriod.DINNER -> Triple(currentRecord?.dinnerMedName ?: "", currentRecord?.dinnerInsulin, currentRecord?.dinnerMedTiming ?: "")
            MealPeriod.NIGHT -> Triple(currentRecord?.nightMedName ?: "", currentRecord?.bedtimeInsulin, currentRecord?.nightMedTiming ?: "")
        }
        initialMedName = name
        initialMedDose = dose
        initialMedTiming = timing
        val defaultMedName = if (period == MealPeriod.NIGHT) "甘精胰岛素" else (topInsulinMed ?: "门冬胰岛素")
        if (dose != null && dose > 0) {
            medDoseInputText = if (dose % 1f == 0f) dose.toInt().toString() else dose.toString()
            medNameInputText = name.ifBlank { defaultMedName }
            medTimingChoice = if (period == MealPeriod.NIGHT) "睡前" else if (timing.isBlank() || timing == "睡前") "餐前" else timing
        } else {
            medDoseInputText = ""
            medNameInputText = defaultMedName
            medTimingChoice = if (period == MealPeriod.NIGHT) "睡前" else "餐前"
        }
        selectedMedCategory = if (period == MealPeriod.NIGHT) MedCategory.INSULIN else MedicationData.inferCategory(medNameInputText)

        if (!isTimeManuallyEdited) {
            val key = when (selectedItemType) {
                ItemType.PRE_MEAL_BG -> "preBG"
                ItemType.DIET -> "diet"
                ItemType.EXERCISE -> "exercise"
                ItemType.MEDICATION -> "med"
                ItemType.POST_MEAL_BG -> null
            }
            val existingTime = if (key != null) currentRecord?.getItemTime(period, key) else targetEntry?.time
            currentItemTime = existingTime?.ifBlank { nowTimeStr } ?: nowTimeStr
        }
    }

    fun flushCurrentToDraft(period: MealPeriod) {
        val draft = periodDrafts.getOrPut(period) { PeriodDraftState() }
        draft.preBgInputText = preBgInputText
        draft.postBgInputText = postBgInputText
        draft.postMealInputs.clear()
        draft.postMealInputs.putAll(postMealInputs)
        draft.postMealTimes.clear()
        draft.postMealTimes.putAll(postMealTimes)
        draft.dietInputText = dietInputText
        draft.dietTagChoice = dietTagChoice
        draft.dietInputs.clear()
        draft.dietInputs.putAll(dietInputs)
        draft.dietTimes.clear()
        draft.dietTimes.putAll(dietTimes)
        draft.currentDietIndex = currentDietIndex
        draft.extraDynamicDietTabs.clear()
        draft.extraDynamicDietTabs.addAll(extraDynamicDietTabs)
        draft.initialDietMap.clear()
        draft.initialDietMap.putAll(initialDietMap)
        draft.exerciseNameInputText = exerciseNameInputText
        draft.exerciseDurationInputText = exerciseDurationInputText
        draft.medNameInputText = medNameInputText
        draft.medDoseInputText = medDoseInputText
        draft.medTimingChoice = medTimingChoice
        draft.selectedMedCategory = selectedMedCategory
        draft.postMealTagChoice = postMealTagChoice
        draft.postMealTimeInputText = postMealTimeInputText
        draft.currentItemTime = currentItemTime
        draft.isTimeManuallyEdited = isTimeManuallyEdited
        draft.currentPostMealIndex = currentPostMealIndex
        draft.extraDynamicPostMealTabs.clear()
        draft.extraDynamicPostMealTabs.addAll(extraDynamicPostMealTabs)
        draft.initialPreBg = initialPreBg
        draft.initialDiet = initialDiet
        draft.initialExercise = initialExercise
        draft.initialMedDose = initialMedDose
        draft.initialMedName = initialMedName
        draft.initialMedTiming = initialMedTiming
        draft.initialPostMealMap.clear()
        draft.initialPostMealMap.putAll(initialPostMealMap)
        draft.isLoaded = true
    }

    fun restoreFromDraft(draft: PeriodDraftState) {
        preBgInputText = draft.preBgInputText
        postBgInputText = draft.postBgInputText
        postMealInputs.clear()
        postMealInputs.putAll(draft.postMealInputs)
        postMealTimes.clear()
        postMealTimes.putAll(draft.postMealTimes)
        dietInputText = draft.dietInputText
        dietTagChoice = draft.dietTagChoice
        dietInputs.clear()
        dietInputs.putAll(draft.dietInputs)
        dietTimes.clear()
        dietTimes.putAll(draft.dietTimes)
        currentDietIndex = draft.currentDietIndex
        extraDynamicDietTabs.clear()
        extraDynamicDietTabs.addAll(draft.extraDynamicDietTabs)
        initialDietMap.clear()
        initialDietMap.putAll(draft.initialDietMap)
        exerciseNameInputText = draft.exerciseNameInputText
        exerciseDurationInputText = draft.exerciseDurationInputText
        medNameInputText = draft.medNameInputText
        medDoseInputText = draft.medDoseInputText
        medTimingChoice = draft.medTimingChoice
        selectedMedCategory = draft.selectedMedCategory
        postMealTagChoice = draft.postMealTagChoice
        postMealTimeInputText = draft.postMealTimeInputText
        currentItemTime = draft.currentItemTime
        isTimeManuallyEdited = draft.isTimeManuallyEdited
        currentPostMealIndex = draft.currentPostMealIndex
        extraDynamicPostMealTabs.clear()
        extraDynamicPostMealTabs.addAll(draft.extraDynamicPostMealTabs)
        initialPreBg = draft.initialPreBg
        initialDiet = draft.initialDiet
        initialExercise = draft.initialExercise
        initialMedDose = draft.initialMedDose
        initialMedName = draft.initialMedName
        initialMedTiming = draft.initialMedTiming
        initialPostMealMap.clear()
        initialPostMealMap.putAll(draft.initialPostMealMap)
    }

    fun loadPeriod(period: MealPeriod, postMealIdx: Int? = null, dietIdx: Int? = null) {
        val draft = periodDrafts[period]
        if (draft != null && draft.isLoaded) {
            restoreFromDraft(draft)
        } else {
            loadExistingDataForPeriod(selectedDate, period, postMealIdx, dietIdx)
            flushCurrentToDraft(period)
        }
    }

    LaunchedEffect(Unit) {
        loadPeriod(selectedPeriod, initialPostMealIndex, initialDietIndex)
    }

    // 切换时段：先保存当前时段的全部临时输入与状态，再无缝恢复或载入新时段数据（绝对保留已填数值）
    fun onPeriodChanged(period: MealPeriod) {
        if (period == selectedPeriod) return
        flushCurrentToDraft(selectedPeriod)
        selectedPeriod = period
        loadPeriod(period, null, null)
    }

    fun onItemTypeChanged(type: ItemType) {
        selectedItemType = type
        if (type == ItemType.POST_MEAL_BG) {
            postBgInputText = postMealInputs[postMealTagChoice] ?: ""
        } else if (type == ItemType.DIET) {
            dietInputText = dietInputs[dietTagChoice] ?: ""
        }
    }

    // 物理返回键优先关闭日期选择器
    if (showDatePicker) {
        BackHandler {
            showDatePicker = false
        }
    }

    // 日期与时间选择弹窗（一体化高颜值卡片，彻底消除底部白条，支持自由调整日期与时间）
    if (showDatePicker) {
        DateTimePickerDialog(
            currentDate = selectedDate,
            currentTime = currentItemTime,
            onDismissRequest = { showDatePicker = false },
            onConfirm = { chosenDate, chosenTime ->
                if (chosenDate != selectedDate) {
                    selectedDate = chosenDate
                    periodDrafts.clear()
                    loadPeriod(selectedPeriod, null)
                }
                currentItemTime = chosenTime
                isTimeManuallyEdited = true
                showDatePicker = false
            },
            onSetToNow = { chosenDate, chosenTime ->
                if (chosenDate != selectedDate) {
                    selectedDate = chosenDate
                    periodDrafts.clear()
                    loadPeriod(selectedPeriod, null)
                }
                currentItemTime = chosenTime
                isTimeManuallyEdited = false
                showDatePicker = false
            }
        )
    }

    val currentRecord = remember(allRecords, selectedDate) { allRecords.find { it.date == selectedDate } }
    val postMealList = remember(currentRecord, selectedPeriod) { currentRecord?.getPostMealList(selectedPeriod) ?: emptyList() }
    val dietList = remember(currentRecord, selectedPeriod) { currentRecord?.getDietList(selectedPeriod) ?: emptyList() }

    val allDietTabs = remember(dietList, extraDynamicDietTabs.toList()) {
        val base = mutableListOf("正餐")
        dietList.forEach { entry ->
            val norm = entry.tag.ifBlank { "正餐" }
            if (norm != "正餐" && !base.contains(norm)) {
                base.add(norm)
            }
        }
        extraDynamicDietTabs.forEach { dyn ->
            if (dyn != "正餐" && !base.contains(dyn)) {
                base.add(dyn)
            }
        }
        base.sortedWith { a, b ->
            if (a == "正餐") -1
            else if (b == "正餐") 1
            else {
                val numA = Regex("""\d+""").find(a)?.value?.toIntOrNull() ?: 999
                val numB = Regex("""\d+""").find(b)?.value?.toIntOrNull() ?: 999
                numA.compareTo(numB)
            }
        }
    }

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

    val currentPreBgFloat = preBgInputText.trim().toFloatOrNull()
    val isPreBgModified = if (initialPreBg != null) {
        currentPreBgFloat != initialPreBg
    } else {
        currentPreBgFloat != null && currentPreBgFloat in 0.5f..35.0f
    }

    val isDietModified = run {
        val curEntries = mutableListOf<com.example.data.DietEntry>()
        val sortedDietTags = dietInputs.keys.sortedWith { a, b ->
            if (a == "正餐") -1 else if (b == "正餐") 1 else (Regex("""\d+""").find(a)?.value?.toIntOrNull() ?: 999).compareTo(Regex("""\d+""").find(b)?.value?.toIntOrNull() ?: 999)
        }
        val currentKeys = (sortedDietTags + if (selectedItemType == ItemType.DIET) listOf(dietTagChoice) else emptyList()).distinct()
        currentKeys.forEach { tag ->
            val text = if (selectedItemType == ItemType.DIET && tag == dietTagChoice) dietInputText.trim() else (dietInputs[tag]?.trim().orEmpty())
            if (text.isNotEmpty()) {
                val t = dietTimes[tag]?.ifBlank { currentItemTime } ?: currentItemTime
                curEntries.add(com.example.data.DietEntry(content = text, time = t, tag = tag))
            }
        }
        val currentSerialized = com.example.data.DietUtils.serializeEntries(curEntries)
        currentSerialized.trim() != initialDiet.trim()
    }

    val currentMedDoseFloat = medDoseInputText.trim().toFloatOrNull()
    val isMedModified = if (initialMedDose != null) {
        currentMedDoseFloat != initialMedDose ||
                (currentMedDoseFloat != null && currentMedDoseFloat > 0f && (medNameInputText.trim() != initialMedName.trim() || medTimingChoice != initialMedTiming))
    } else {
        currentMedDoseFloat != null && currentMedDoseFloat > 0f
    }

    val formattedExercise = when {
        exerciseNameInputText.isNotBlank() && exerciseDurationInputText.isNotBlank() ->
            "${exerciseNameInputText.trim()} ${exerciseDurationInputText.trim()}分钟"
        exerciseNameInputText.isNotBlank() -> exerciseNameInputText.trim()
        exerciseDurationInputText.isNotBlank() -> "${exerciseDurationInputText.trim()}分钟"
        else -> ""
    }
    val isExerciseModified = formattedExercise != initialExercise.trim()

    val isPostMealModified = run {
        val currentValidPostMeals = postMealInputs.filter { (_, v) ->
            v.trim().isNotEmpty() && (v.trim().toFloatOrNull()?.let { it in 0.5f..35.0f } == true)
        }
        if (currentValidPostMeals.keys != initialPostMealMap.keys) {
            true
        } else {
            currentValidPostMeals.any { (tag, vStr) ->
                vStr.trim().toFloatOrNull() != initialPostMealMap[tag]
            }
        }
    }

    val isPreBgValid = preBgInputText.trim().isEmpty() || (currentPreBgFloat != null && currentPreBgFloat in 0.5f..35.0f)
    val isMedValid = medDoseInputText.trim().isEmpty() || (currentMedDoseFloat != null && currentMedDoseFloat > 0f)
    val isPostMealValid = postMealInputs.values.all { it.trim().isEmpty() || (it.trim().toFloatOrNull()?.let { v -> v in 0.5f..35.0f } == true) }

    val hasAnyModification = isPreBgModified || isDietModified || isMedModified || isExerciseModified || isPostMealModified
    val currentPeriodCanSubmit = hasAnyModification && isPreBgValid && isMedValid && isPostMealValid
    val canSubmit = currentPeriodCanSubmit || periodDrafts.values.any { it.isModified() }

    val hasExistingDataForPeriod = remember(currentRecord, selectedPeriod) {
        currentRecord?.let { rec ->
            when (selectedPeriod) {
                MealPeriod.MORNING -> rec.hasMorningData
                MealPeriod.LUNCH -> rec.hasLunchData
                MealPeriod.DINNER -> rec.hasDinnerData
                MealPeriod.NIGHT -> rec.hasNightData
            }
        } ?: false
    }

    val modifiedItemsCount = listOf(
        isPreBgModified,
        isDietModified,
        isMedModified,
        isExerciseModified,
        isPostMealModified
    ).count { it }

    val totalModifiedCount = run {
        val otherModified = periodDrafts.filterKeys { it != selectedPeriod }.values.count { it.isModified() }
        modifiedItemsCount + otherModified
    }

    val submitButtonText = when {
        hasExistingDataForPeriod -> {
            if (totalModifiedCount > 1) "保存修改 (${totalModifiedCount}项)" else "保存修改"
        }
        else -> {
            if (totalModifiedCount > 1) "保存 (${totalModifiedCount}项)" else "保存条目"
        }
    }

    fun doesTabHaveData(itemType: ItemType): Boolean {
        return when (itemType) {
            ItemType.PRE_MEAL_BG -> preBgInputText.trim().isNotEmpty()
            ItemType.DIET -> dietInputText.trim().isNotEmpty() || dietInputs.values.any { it.trim().isNotEmpty() }
            ItemType.MEDICATION -> medDoseInputText.trim().isNotEmpty()
            ItemType.POST_MEAL_BG -> postBgInputText.trim().isNotEmpty() || postMealInputs.values.any { it.trim().isNotEmpty() }
            ItemType.EXERCISE -> exerciseNameInputText.trim().isNotEmpty() || exerciseDurationInputText.trim().isNotEmpty()
        }
    }

    fun submit(keepOpen: Boolean) {
        if (!canSubmit) return
        flushCurrentToDraft(selectedPeriod)

        val periodsToSave = periodDrafts.filter { (p, draft) ->
            p == selectedPeriod || draft.isModified()
        }
        if (periodsToSave.isEmpty()) return

        val entries = periodsToSave.entries.toList()
        entries.forEachIndexed { index, (p, draft) ->
            val isLast = index == entries.lastIndex
            val recordTime = draft.currentItemTime.trim().ifBlank { nowTimeStr }
            val actualTiming = if (p == MealPeriod.NIGHT) "睡前" else draft.medTimingChoice

            if (p == selectedPeriod && selectedItemType == ItemType.POST_MEAL_BG) {
                draft.postMealInputs[draft.postMealTagChoice] = postBgInputText
                draft.postMealTimes[draft.postMealTagChoice] = recordTime
            }

            if (p == selectedPeriod && selectedItemType == ItemType.DIET) {
                draft.dietInputs[draft.dietTagChoice] = dietInputText
                draft.dietTimes[draft.dietTagChoice] = recordTime
            }

            val postMealEntriesList = mutableListOf<com.example.data.PostMealEntry>()
            draft.postMealInputs.forEach { (tag, valStr) ->
                val v = valStr.trim().toFloatOrNull()
                if (v != null && v in 0.5f..35.0f) {
                    val t = draft.postMealTimes[tag]?.ifBlank { recordTime } ?: recordTime
                    postMealEntriesList.add(com.example.data.PostMealEntry(v, t, tag))
                }
            }

            val dietEntriesList = mutableListOf<com.example.data.DietEntry>()
            val sortedDietTags = draft.dietInputs.keys.sortedWith { a, b ->
                if (a == "正餐") -1 else if (b == "正餐") 1 else (Regex("""\d+""").find(a)?.value?.toIntOrNull() ?: 999).compareTo(Regex("""\d+""").find(b)?.value?.toIntOrNull() ?: 999)
            }
            sortedDietTags.forEach { tag ->
                val text = draft.dietInputs[tag]?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    val t = draft.dietTimes[tag]?.ifBlank { recordTime } ?: recordTime
                    dietEntriesList.add(com.example.data.DietEntry(content = text, time = t, tag = tag))
                }
            }
            val serializedDiet = com.example.data.DietUtils.serializeEntries(dietEntriesList)

            val curPreBgFloat = draft.preBgInputText.trim().toFloatOrNull()
            val preBgMod = if (draft.initialPreBg != null) {
                curPreBgFloat != draft.initialPreBg
            } else {
                curPreBgFloat != null && curPreBgFloat in 0.5f..35.0f
            }
            val dietMod = serializedDiet.trim() != draft.initialDiet.trim()
            val curMedDoseFloat = draft.medDoseInputText.trim().toFloatOrNull()
            val medMod = if (draft.initialMedDose != null) {
                curMedDoseFloat != draft.initialMedDose ||
                        (curMedDoseFloat != null && curMedDoseFloat > 0f && (draft.medNameInputText.trim() != draft.initialMedName.trim() || draft.medTimingChoice != draft.initialMedTiming))
            } else {
                curMedDoseFloat != null && curMedDoseFloat > 0f
            }
            val formattedEx = when {
                draft.exerciseNameInputText.isNotBlank() && draft.exerciseDurationInputText.isNotBlank() ->
                    "${draft.exerciseNameInputText.trim()} ${draft.exerciseDurationInputText.trim()}分钟"
                draft.exerciseNameInputText.isNotBlank() -> draft.exerciseNameInputText.trim()
                draft.exerciseDurationInputText.isNotBlank() -> "${draft.exerciseDurationInputText.trim()}分钟"
                else -> ""
            }
            val exMod = formattedEx != draft.initialExercise.trim()
            val postMealMod = run {
                val currentValidPostMeals = draft.postMealInputs.filter { (_, v) ->
                    v.trim().isNotEmpty() && (v.trim().toFloatOrNull()?.let { it in 0.5f..35.0f } == true)
                }
                if (currentValidPostMeals.keys != draft.initialPostMealMap.keys) {
                    true
                } else {
                    currentValidPostMeals.any { (tag, vStr) ->
                        vStr.trim().toFloatOrNull() != draft.initialPostMealMap[tag]
                    }
                }
            }

            onSaveItem(
                selectedDate,
                p,
                curPreBgFloat,
                preBgMod,
                serializedDiet,
                dietMod,
                draft.medNameInputText.trim(),
                curMedDoseFloat,
                actualTiming,
                medMod,
                formattedEx,
                exMod,
                postMealEntriesList,
                postMealMod,
                recordTime,
                draft.isTimeManuallyEdited,
                if (isLast) keepOpen else true
            )
        }

        if (keepOpen) {
            periodDrafts.clear()
            loadPeriod(selectedPeriod, null, null)
        }
    }


    ImageSourcePickerDialog(
        visible = showImageSourcePicker,
        onDismiss = { showImageSourcePicker = false },
        onTakePhoto = { triggerCamera() },
        onPickGallery = { galleryLauncher.launch("image/*") }
    )

    FrostedGlassDialogOverlay(
        onDismissRequest = onDismiss,
        dismissOnBackPress = !showDatePicker && !showImageSourcePicker
    ) { dismissWithAnimation ->
        val isDark = AppThemeColors.isDark
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {}
                .testTag("add_item_dialog"),
            shape = AppleCardShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White
            ),
            border = appleCardBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.38).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                        // 可点击更换日期的轻量胶囊
                        Surface(
                            shape = ApplePillShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = appleCardBorder(),
                            modifier = Modifier
                                .clip(ApplePillShape)
                                .applePressEffect(0.95f)
                                .clickable { showDatePicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                val datePrefix = if (selectedDate == today) "今天" else selectedDate.substring(5)
                                Text(
                                    text = "$datePrefix $currentItemTime",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.12).sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { dismissWithAnimation() },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .applePressEffect(0.92f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. 时段切换分段胶囊（1 行极简设计，默认匹配当前系统时间，平滑过渡动画）
                MealPeriodSelectorCapsule(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { onPeriodChanged(it) }
                )

                // 3. 条目类型切换分段胶囊（Apple 胶囊轨道与物理滑块动画）
                val itemTypes = remember { ItemType.entries }
                val selectedTypeIndex = itemTypes.indexOf(selectedItemType).coerceAtLeast(0)
                val animatedTypeIndex by animateFloatAsState(
                    targetValue = selectedTypeIndex.toFloat(),
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "item_type_slider"
                )

                val trackBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA).copy(alpha = 0.6f)
                val trackBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(AppleSegmentTrackShape)
                        .background(trackBg)
                        .border(BorderStroke(1.dp, trackBorderColor), AppleSegmentTrackShape)
                        .padding(3.dp)
                ) {
                    val tabWidth = maxWidth / itemTypes.size

                    // 主题色滑块（与全局选项卡统一圆角）
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
                        itemTypes.forEachIndexed { index, itemType ->
                            val isSelected = itemType == selectedItemType
                            val (icon, label) = when (itemType) {
                                ItemType.PRE_MEAL_BG -> "🩸" to if (selectedPeriod == MealPeriod.MORNING) "空腹" else if (selectedPeriod == MealPeriod.NIGHT) "睡前" else "餐前"
                                ItemType.DIET -> "🍽️" to "用餐"
                                ItemType.MEDICATION -> "💊" to "用药"
                                ItemType.POST_MEAL_BG -> "📈" to "餐后"
                                ItemType.EXERCISE -> "🏃" to "运动"
                            }
                            val animText by animateColorAsState(
                                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(200),
                                label = "type_chip_text_$index"
                            )
                            val hasData = doesTabHaveData(itemType)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(AppleSegmentThumbShape)
                                    .applePressEffect(0.96f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onItemTypeChanged(itemType) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = icon,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(2.5.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        letterSpacing = (-0.224).sp,
                                        color = animText,
                                        style = TextStyle(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                    if (hasData && !isSelected) {
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(TealPrimary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. 内容表单区（切换分类时轻量淡入淡出，切换时段时输入框稳固无位移，杜绝过度动画）
                AnimatedContent(
                    targetState = selectedItemType,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(140)) togetherWith fadeOut(animationSpec = tween(100))
                    },
                    label = "form_content_animation"
                ) { currentItemType ->
                    when (currentItemType) {
                    ItemType.PRE_MEAL_BG -> {
                        val bgTitle = if (selectedPeriod == MealPeriod.MORNING) "空腹血糖" else if (selectedPeriod == MealPeriod.NIGHT) "睡前血糖" else "餐前血糖"
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = preBgInputText,
                                    onValueChange = { preBgInputText = it.replace('。', '.').replace('，', '.').replace(" ", "") },
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
                                StepAdjustButtons(
                                    currentValue = preBgInputText,
                                    onValueChange = { preBgInputText = it },
                                    defaultValue = 6.0f
                                )
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
                                    val tabVal = postMealInputs[tab]?.trim()?.toFloatOrNull()
                                    val hasData = (tabVal != null && tabVal in 0.5f..35.0f) || match != null

                                    val targetBg = when {
                                        isSelected -> TealPrimary
                                        hasData -> if (AppThemeColors.isDark) Color(0xFF134E4A).copy(alpha = 0.65f) else Color(0xFFE6F4EA)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                    val border = when {
                                        isSelected -> null
                                        hasData -> BorderStroke(1.dp, if (AppThemeColors.isDark) Color(0xFF2DD4BF).copy(alpha = 0.55f) else TealPrimary.copy(alpha = 0.5f))
                                        else -> null
                                    }
                                    val targetText = when {
                                        isSelected -> Color.White
                                        hasData -> if (AppThemeColors.isDark) Color(0xFF2DD4BF) else TealPrimary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    val animBg by animateColorAsState(
                                        targetValue = targetBg,
                                        animationSpec = tween(220),
                                        label = "post_meal_tab_bg"
                                    )
                                    val animText by animateColorAsState(
                                        targetValue = targetText,
                                        animationSpec = tween(220),
                                        label = "post_meal_tab_text"
                                    )
                                    val tabScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.05f else 1f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "post_meal_tab_scale"
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = animBg,
                                        border = border,
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = tabScale
                                                scaleY = tabScale
                                            }
                                            .clickable {
                                                val prevChoice = postMealTagChoice
                                                if (prevChoice != tab) {
                                                    postMealInputs[prevChoice] = postBgInputText
                                                }
                                                postMealTagChoice = tab
                                                if (match != null) {
                                                    currentPostMealIndex = match.first
                                                    postBgInputText = postMealInputs[tab] ?: String.format(Locale.US, "%.1f", match.second.value)
                                                    postMealTimeInputText = postMealTimes[tab] ?: match.second.time.ifBlank { nowTimeStr }
                                                } else {
                                                    currentPostMealIndex = null
                                                    postBgInputText = postMealInputs[tab] ?: ""
                                                    postMealTimeInputText = postMealTimes[tab] ?: nowTimeStr
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
                                                color = animText
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
                                        val prevChoice = postMealTagChoice
                                        postMealInputs[prevChoice] = postBgInputText

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
                                        postBgInputText = ""
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
                                    value = postBgInputText,
                                    onValueChange = {
                                        val clean = it.replace('。', '.').replace('，', '.').replace(" ", "")
                                        postBgInputText = clean
                                        postMealInputs[postMealTagChoice] = clean
                                    },
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

                                StepAdjustButtons(
                                    currentValue = postBgInputText,
                                    onValueChange = {
                                        postBgInputText = it
                                        postMealInputs[postMealTagChoice] = it
                                    },
                                    defaultValue = 7.5f
                                )
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
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 动态用餐餐次选项卡（正餐、加餐1、加餐2 及 + 新增按钮）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                allDietTabs.forEach { tab ->
                                    val isSelected = dietTagChoice == tab
                                    val match = dietList.find { it.tag == tab }
                                    val tabVal = if (isSelected) dietInputText.trim() else dietInputs[tab]?.trim().orEmpty()
                                    val hasData = tabVal.isNotEmpty() || match != null

                                    val targetBg = when {
                                        isSelected -> TealPrimary
                                        hasData -> if (AppThemeColors.isDark) Color(0xFF134E4A).copy(alpha = 0.65f) else Color(0xFFE6F4EA)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                    val border = when {
                                        isSelected -> null
                                        hasData -> BorderStroke(1.dp, if (AppThemeColors.isDark) Color(0xFF2DD4BF).copy(alpha = 0.55f) else TealPrimary.copy(alpha = 0.5f))
                                        else -> null
                                    }
                                    val targetText = when {
                                        isSelected -> Color.White
                                        hasData -> if (AppThemeColors.isDark) Color(0xFF2DD4BF) else TealPrimary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    val animBg by animateColorAsState(
                                        targetValue = targetBg,
                                        animationSpec = tween(220),
                                        label = "diet_tab_bg"
                                    )
                                    val animText by animateColorAsState(
                                        targetValue = targetText,
                                        animationSpec = tween(220),
                                        label = "diet_tab_text"
                                    )
                                    val tabScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.05f else 1f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "diet_tab_scale"
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = animBg,
                                        border = border,
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = tabScale
                                                scaleY = tabScale
                                            }
                                            .clickable {
                                                val prevChoice = dietTagChoice
                                                if (prevChoice != tab) {
                                                    dietInputs[prevChoice] = dietInputText
                                                }
                                                dietTagChoice = tab
                                                if (match != null) {
                                                    currentDietIndex = dietList.indexOf(match)
                                                    dietInputText = dietInputs[tab] ?: match.content
                                                } else {
                                                    currentDietIndex = null
                                                    dietInputText = dietInputs[tab] ?: ""
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
                                                color = animText
                                            )
                                        }
                                    }
                                }

                                // "+ 新增" 按钮（生成 加餐1, 加餐2, ... 依此类推）
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        val prevChoice = dietTagChoice
                                        dietInputs[prevChoice] = dietInputText

                                        val nextTab = com.example.data.DietUtils.getNextSnackTag(allDietTabs)
                                        if (!extraDynamicDietTabs.contains(nextTab)) {
                                            extraDynamicDietTabs.add(nextTab)
                                        }
                                        dietTagChoice = nextTab
                                        currentDietIndex = null
                                        dietInputText = ""
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

                            // 饮食内容输入框
                            OutlinedTextField(
                                value = dietInputText,
                                onValueChange = {
                                    dietInputText = it
                                    dietInputs[dietTagChoice] = it
                                },
                                label = { Text(if (dietTagChoice == "正餐") "正餐吃了什么？" else "${dietTagChoice}吃了什么？") },
                                placeholder = { Text(if (dietTagChoice == "正餐") "如：全麦面包、水煮蛋1个、纯牛奶" else "如：苹果半个、坚果一小把") },
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
                                onValueChange = { exerciseDurationInputText = it.filter { char -> char.isDigit() } },
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

                        // 综合候选药物列表：优先按真实历史使用频次排序置顶（高频使用的“胰岛素”或具体药名高居前列）
                        val currentMedList = remember(selectedMedCategory, allRecords, medFrequencyMap) {
                            MedicationData.getMedicationOptions(selectedMedCategory, allRecords, medFrequencyMap)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 1. 药物大类选择：胰岛素 vs 口服药 vs GLP-1/针剂 三个按钮
                            MedCategorySelectorCapsule(
                                selectedCategory = selectedMedCategory,
                                onCategorySelected = { cat ->
                                    selectedMedCategory = cat
                                    if (cat == MedCategory.INSULIN && (medNameInputText.isBlank() || medNameInputText in MedicationData.commonOralMeds || medNameInputText in MedicationData.commonGLP1Meds)) {
                                        medNameInputText = if (selectedPeriod == MealPeriod.NIGHT) "甘精胰岛素" else "门冬胰岛素"
                                    } else if (cat == MedCategory.ORAL && (medNameInputText.isBlank() || medNameInputText in MedicationData.commonInsulinMeds || medNameInputText in MedicationData.commonGLP1Meds)) {
                                        medNameInputText = "二甲双胍"
                                    } else if (cat == MedCategory.GLP1 && (medNameInputText.isBlank() || medNameInputText in MedicationData.commonInsulinMeds || medNameInputText in MedicationData.commonOralMeds)) {
                                        medNameInputText = "司美格鲁肽"
                                    }
                                }
                            )

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
                                                            text = when (selectedMedCategory) {
                                                                MedCategory.INSULIN -> "例: 门冬胰岛素"
                                                                MedCategory.ORAL -> "例: 二甲双胍"
                                                                MedCategory.GLP1 -> "例: 司美格鲁肽"
                                                            },
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
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (selectedMedCategory) {
                                                    MedCategory.INSULIN -> "💉 胰岛素"
                                                    MedCategory.ORAL -> "💊 口服药"
                                                    MedCategory.GLP1 -> "💉 GLP-1/针剂"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            thickness = 0.5.dp
                                        )
                                        currentMedList.forEach { med ->
                                            val isCurrent = medNameInputText == med
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = med,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isCurrent) TealPrimary else MaterialTheme.colorScheme.onSurface
                                                        )
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
                                        .clickable { showImageSourcePicker = true }
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
                                            color = TealPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // OCR 识别状态或提示
                            OcrStatusBanner(
                                isRecognizing = isRecognizing,
                                recognitionMessage = recognitionMessage,
                                onDismissMessage = { recognitionMessage = null }
                            )

                            // 3. 常见/置顶药物快捷点选胶囊（分类切换带淡入淡出滑动过渡）
                            AnimatedContent(
                                targetState = selectedMedCategory,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                                },
                                label = "quick_med_chips_anim"
                            ) { targetCat ->
                                val medList = MedicationData.getMedicationOptions(targetCat, allRecords, medFrequencyMap)
                                QuickMedChips(
                                    medList = medList,
                                    selectedMedName = medNameInputText,
                                    onMedSelected = { med ->
                                        medNameInputText = med
                                        selectedMedCategory = MedicationData.inferCategory(med)
                                    }
                                )
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
                                        onValueChange = { medDoseInputText = it.replace('。', '.').replace('，', '.').replace(" ", "") },
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

                                    // 时机选择（餐前、餐中、餐后，配备物理滑块丝滑动画，高度严格 50.dp，与左侧剂量框上下完全齐平）
                                    MedicationTimingSelectorCapsule(
                                        selectedTiming = medTimingChoice,
                                        onTimingSelected = { medTimingChoice = it },
                                        modifier = Modifier.width(175.dp),
                                        height = 50.dp
                                    )
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
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 5. 底部操作栏（Apple 48.dp Pill 确认按钮，全宽且搭载签名级物理弹簧微动效）
                Button(
                    onClick = { submit(keepOpen = false) },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        disabledContainerColor = TealPrimary.copy(alpha = 0.35f)
                    ),
                    shape = ApplePillShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .applePressEffect(0.95f)
                        .testTag("add_item_submit_button")
                ) {
                    Text(
                        text = submitButtonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.32).sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }
        }
    }
}

/**
 * 跨时段草稿记忆状态容器，解决切换时段导致已输入数值被抹除的问题
 */
private class PeriodDraftState(
    var preBgInputText: String = "",
    var postBgInputText: String = "",
    val postMealInputs: MutableMap<String, String> = mutableMapOf(),
    val postMealTimes: MutableMap<String, String> = mutableMapOf(),
    var dietInputText: String = "",
    var dietTagChoice: String = "正餐",
    val dietInputs: MutableMap<String, String> = mutableMapOf(),
    val dietTimes: MutableMap<String, String> = mutableMapOf(),
    var currentDietIndex: Int? = null,
    val extraDynamicDietTabs: MutableList<String> = mutableListOf(),
    val initialDietMap: MutableMap<String, String> = mutableMapOf(),
    var exerciseNameInputText: String = "",
    var exerciseDurationInputText: String = "",
    var medNameInputText: String = "",
    var medDoseInputText: String = "",
    var medTimingChoice: String = "餐前",
    var selectedMedCategory: MedCategory = MedCategory.INSULIN,
    var postMealTagChoice: String = "餐后2h",
    var postMealTimeInputText: String = "",
    var currentItemTime: String = "",
    var isTimeManuallyEdited: Boolean = false,
    var currentPostMealIndex: Int? = null,
    val extraDynamicPostMealTabs: MutableList<String> = mutableListOf(),
    var initialPreBg: Float? = null,
    var initialDiet: String = "",
    var initialExercise: String = "",
    var initialMedDose: Float? = null,
    var initialMedName: String = "",
    var initialMedTiming: String = "",
    val initialPostMealMap: MutableMap<String, Float> = mutableMapOf(),
    var isLoaded: Boolean = false
) {
    fun isModified(): Boolean {
        val currentPreBgFloat = preBgInputText.trim().toFloatOrNull()
        val preBgMod = if (initialPreBg != null) {
            currentPreBgFloat != initialPreBg
        } else {
            currentPreBgFloat != null && currentPreBgFloat in 0.5f..35.0f
        }
        val curEntries = mutableListOf<com.example.data.DietEntry>()
        val sortedDietTags = dietInputs.keys.sortedWith { a, b ->
            if (a == "正餐") -1 else if (b == "正餐") 1 else (Regex("""\d+""").find(a)?.value?.toIntOrNull() ?: 999).compareTo(Regex("""\d+""").find(b)?.value?.toIntOrNull() ?: 999)
        }
        sortedDietTags.forEach { tag ->
            val text = dietInputs[tag]?.trim().orEmpty()
            if (text.isNotEmpty()) {
                val t = dietTimes[tag]?.ifBlank { currentItemTime } ?: currentItemTime
                curEntries.add(com.example.data.DietEntry(content = text, time = t, tag = tag))
            }
        }
        val currentSerialized = com.example.data.DietUtils.serializeEntries(curEntries)
        val dietMod = currentSerialized.trim() != initialDiet.trim()
        val currentMedDoseFloat = medDoseInputText.trim().toFloatOrNull()
        val medMod = if (initialMedDose != null) {
            currentMedDoseFloat != initialMedDose ||
                    (currentMedDoseFloat != null && currentMedDoseFloat > 0f && (medNameInputText.trim() != initialMedName.trim() || medTimingChoice != initialMedTiming))
        } else {
            currentMedDoseFloat != null && currentMedDoseFloat > 0f
        }
        val formattedEx = when {
            exerciseNameInputText.isNotBlank() && exerciseDurationInputText.isNotBlank() ->
                "${exerciseNameInputText.trim()} ${exerciseDurationInputText.trim()}分钟"
            exerciseNameInputText.isNotBlank() -> exerciseNameInputText.trim()
            exerciseDurationInputText.isNotBlank() -> "${exerciseDurationInputText.trim()}分钟"
            else -> ""
        }
        val exMod = formattedEx != initialExercise.trim()
        val postMealMod = run {
            val currentValidPostMeals = postMealInputs.filter { (_, v) ->
                v.trim().isNotEmpty() && (v.trim().toFloatOrNull()?.let { it in 0.5f..35.0f } == true)
            }
            if (currentValidPostMeals.keys != initialPostMealMap.keys) {
                true
            } else {
                currentValidPostMeals.any { (tag, vStr) ->
                    vStr.trim().toFloatOrNull() != initialPostMealMap[tag]
                }
            }
        }
        return preBgMod || dietMod || medMod || exMod || postMealMod
    }
}

