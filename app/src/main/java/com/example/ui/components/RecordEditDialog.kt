package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.BGUtils
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.data.MedCategory
import com.example.data.MedicationData
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 全天综合记录详细编辑弹窗 (RecordEditDialog)：
 *
 * 架构职责与边界隔离：
 * 1. 深度编辑全天数据：允许用户细致编辑一整天的所有时段（早、午、晚、睡前）的全部指标；
 * 2. 键盘智能避让系统：内置基于 IME WindowInsets 与局部坐标计算的滚动避让控制器，
 *    确保任意输入框聚焦时自动高出底部操作区 8dp（半个字距），解决软键盘遮挡问题；
 * 3. 边界与入参契约：
 *    - 严格遵循单一传入实体 [initialRecord] 进行草稿初始化；
 *    - 保存时组装全新的不可变 [InsulinRecord] 实体通过 [onSave] 回调输出；
 *    - 内部编辑过程与外部 ViewModel 状态完全解耦，不产生中间副作用；
 * 4. 共享组件复用：复用 [OcrStatusBanner] 等通用交互组件，保持视觉统一与易于维护。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecordEditDialog(
    initialRecord: InsulinRecord?,
    initialDate: String?,
    initialPeriod: MealPeriod? = null,
    allRecords: List<InsulinRecord>,
    onDismiss: () -> Unit,
    onSave: (InsulinRecord) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val medFrequencyMap = remember(allRecords) { MedicationData.computeMedicationFrequencies(allRecords) }
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeBottom = imeInsets.getBottom(density)
    val isKeyboardOpen = imeBottom > 0

    var scrollContainerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var activeFocusedCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var isScrollingAnimationRunning by remember { mutableStateOf(false) }

    fun adjustScrollForField(targetCoordinates: LayoutCoordinates?) {
        val container = scrollContainerCoordinates ?: return
        val field = targetCoordinates ?: return
        if (!field.isAttached || !container.isAttached) return

        // 避让时输入框高出底部固定操作栏半个字距 (8dp)
        val halfCharGapPx = with(density) { 8.dp.toPx() }

        // 使用 localPositionOf 准确获取输入框相对于内部滚动视口的未裁剪相对坐标
        val positionInContainer = container.localPositionOf(field, Offset.Zero)
        val fieldTopInContainer = positionInContainer.y
        val fieldBottomInContainer = positionInContainer.y + field.size.height.toFloat()
        val containerHeight = container.size.height.toFloat()

        val overlapBottom = fieldBottomInContainer - (containerHeight - halfCharGapPx)
        if (overlapBottom > 1f) {
            coroutineScope.launch {
                isScrollingAnimationRunning = true
                try {
                    scrollState.animateScrollBy(overlapBottom)
                } finally {
                    isScrollingAnimationRunning = false
                }
            }
        } else {
            val overlapTop = halfCharGapPx - fieldTopInContainer
            if (overlapTop > 1f) {
                coroutineScope.launch {
                    isScrollingAnimationRunning = true
                    try {
                        scrollState.animateScrollBy(-overlapTop)
                    } finally {
                        isScrollingAnimationRunning = false
                    }
                }
            }
        }
    }

    val onFieldFocus: (LayoutCoordinates) -> Unit = { coords ->
        activeFocusedCoordinates = coords
        coroutineScope.launch {
            delay(120)
            adjustScrollForField(coords)
            delay(200)
            adjustScrollForField(coords)
        }
    }

    LaunchedEffect(isKeyboardOpen) {
        if (!isKeyboardOpen) {
            scrollState.animateScrollTo(0)
            activeFocusedCoordinates = null
        } else if (activeFocusedCoordinates != null) {
            delay(250)
            adjustScrollForField(activeFocusedCoordinates)
        }
    }

    val today = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    var date by remember { mutableStateOf(initialRecord?.date ?: initialDate ?: today) }

    // Current record resolved from date (updates when date switches)
    val currentRecordForDate = remember(date, initialRecord, allRecords) {
        if (initialRecord != null && initialRecord.date == date) {
            initialRecord
        } else {
            allRecords.find { it.date == date }
        }
    }

    // 默认时段：如果传入则按传入，否则检测该日期记录已填情况自动推荐
    var selectedPeriod by remember {
        mutableStateOf(
            initialPeriod ?: currentRecordForDate?.getNextRecommendedPeriod() ?: run {
                val hour = java.time.LocalTime.now().hour
                when {
                    hour < 11 -> MealPeriod.MORNING
                    hour < 15 -> MealPeriod.LUNCH
                    hour < 20 -> MealPeriod.DINNER
                    else -> MealPeriod.NIGHT
                }
            }
        )
    }

    // 晨间数据状态（晨间空腹即为餐前）
    var fastingBG by remember(currentRecordForDate) {
        val initialBG = currentRecordForDate?.fastingBG ?: currentRecordForDate?.preBfBG
        mutableStateOf(initialBG?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var postBfBG by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.postBfBG?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var bfMedName by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.bfMedName?.ifBlank { "胰岛素" } ?: "胰岛素")
    }
    var bfInsulin by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.bfInsulin?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var bfMedTiming by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.bfMedTiming?.ifBlank { "餐前" } ?: "餐前")
    }
    var bfDiet by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.bfDiet ?: "")
    }
    var bfExercise by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.bfExercise ?: "")
    }

    // 午间数据状态
    var preLunchBG by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.preLunchBG?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var postLunchBG by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.postLunchBG?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var lunchMedName by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.lunchMedName?.ifBlank { "胰岛素" } ?: "胰岛素")
    }
    var lunchInsulin by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.lunchInsulin?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var lunchMedTiming by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.lunchMedTiming?.ifBlank { "餐前" } ?: "餐前")
    }
    var lunchDiet by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.lunchDiet ?: "")
    }
    var lunchExercise by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.lunchExercise ?: "")
    }

    // 傍晚数据状态
    var preDinnerBG by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.preDinnerBG?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var postDinnerBG by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.postDinnerBG?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var dinnerMedName by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.dinnerMedName?.ifBlank { "胰岛素" } ?: "胰岛素")
    }
    var dinnerInsulin by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.dinnerInsulin?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var dinnerMedTiming by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.dinnerMedTiming?.ifBlank { "餐前" } ?: "餐前")
    }
    var dinnerDiet by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.dinnerDiet ?: "")
    }
    var dinnerExercise by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.dinnerExercise ?: "")
    }

    // 夜晚数据状态
    var preNightBG by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.preNightBG?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var postNightBG by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.postNightBG?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var nightMedName by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.nightMedName?.ifBlank { "胰岛素" } ?: "胰岛素")
    }
    var bedtimeInsulin by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.bedtimeInsulin?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    var nightMedTiming by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.nightMedTiming?.ifBlank { "餐前" } ?: "餐前")
    }
    var nightDiet by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.nightDiet ?: "")
    }
    var nightExercise by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.nightExercise ?: "")
    }

    var notes by remember(currentRecordForDate) {
        mutableStateOf(currentRecordForDate?.notes ?: "")
    }

    var showDatePicker by remember { mutableStateOf(false) }

    val weekdayStr = remember(date) {
        try {
            val ld = LocalDate.parse(date)
            when (ld.dayOfWeek.value) {
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

    val prevNightInfo = remember(date, allRecords) {
        BGUtils.getPrevNightInsulin(date, allRecords)
    }

    // 用药分类状态 (胰岛素 vs 口服药)
    var bfCategory by remember(currentRecordForDate) {
        mutableStateOf(MedicationData.inferCategory(bfMedName))
    }
    var lunchCategory by remember(currentRecordForDate) {
        mutableStateOf(MedicationData.inferCategory(lunchMedName))
    }
    var dinnerCategory by remember(currentRecordForDate) {
        mutableStateOf(MedicationData.inferCategory(dinnerMedName))
    }
    var nightCategory by remember(currentRecordForDate) {
        mutableStateOf(MedicationData.inferCategory(nightMedName))
    }

    val currentCategory = when (selectedPeriod) {
        MealPeriod.MORNING -> bfCategory
        MealPeriod.LUNCH -> lunchCategory
        MealPeriod.DINNER -> dinnerCategory
        MealPeriod.NIGHT -> nightCategory
    }
    val onCategoryChange: (MedCategory) -> Unit = { newCat ->
        when (selectedPeriod) {
            MealPeriod.MORNING -> {
                bfCategory = newCat
                if (newCat == MedCategory.ORAL && (bfMedName.contains("胰岛素") || bfMedName.isBlank() || MedicationData.isGLP1Med(bfMedName))) {
                    bfMedName = "口服药"
                } else if (newCat == MedCategory.INSULIN && (!bfMedName.contains("胰岛素") || bfMedName.isBlank())) {
                    bfMedName = "胰岛素"
                } else if (newCat == MedCategory.GLP1 && (!MedicationData.isGLP1Med(bfMedName) || bfMedName.isBlank())) {
                    bfMedName = "司美格鲁肽"
                }
            }
            MealPeriod.LUNCH -> {
                lunchCategory = newCat
                if (newCat == MedCategory.ORAL && (lunchMedName.contains("胰岛素") || lunchMedName.isBlank() || MedicationData.isGLP1Med(lunchMedName))) {
                    lunchMedName = "口服药"
                } else if (newCat == MedCategory.INSULIN && (!lunchMedName.contains("胰岛素") || lunchMedName.isBlank())) {
                    lunchMedName = "胰岛素"
                } else if (newCat == MedCategory.GLP1 && (!MedicationData.isGLP1Med(lunchMedName) || lunchMedName.isBlank())) {
                    lunchMedName = "司美格鲁肽"
                }
            }
            MealPeriod.DINNER -> {
                dinnerCategory = newCat
                if (newCat == MedCategory.ORAL && (dinnerMedName.contains("胰岛素") || dinnerMedName.isBlank() || MedicationData.isGLP1Med(dinnerMedName))) {
                    dinnerMedName = "口服药"
                } else if (newCat == MedCategory.INSULIN && (!dinnerMedName.contains("胰岛素") || dinnerMedName.isBlank())) {
                    dinnerMedName = "胰岛素"
                } else if (newCat == MedCategory.GLP1 && (!MedicationData.isGLP1Med(dinnerMedName) || dinnerMedName.isBlank())) {
                    dinnerMedName = "司美格鲁肽"
                }
            }
            MealPeriod.NIGHT -> {
                nightCategory = newCat
                if (newCat == MedCategory.ORAL && (nightMedName.contains("胰岛素") || nightMedName.isBlank() || MedicationData.isGLP1Med(nightMedName))) {
                    nightMedName = "口服药"
                } else if (newCat == MedCategory.INSULIN && (!nightMedName.contains("胰岛素") || nightMedName.isBlank())) {
                    nightMedName = "胰岛素"
                } else if (newCat == MedCategory.GLP1 && (!MedicationData.isGLP1Med(nightMedName) || nightMedName.isBlank())) {
                    nightMedName = "司美格鲁肽"
                }
            }
        }
    }

    val currentMedName = when (selectedPeriod) {
        MealPeriod.MORNING -> bfMedName
        MealPeriod.LUNCH -> lunchMedName
        MealPeriod.DINNER -> dinnerMedName
        MealPeriod.NIGHT -> nightMedName
    }
    val onMedNameChange: (String) -> Unit = { newName ->
        when (selectedPeriod) {
            MealPeriod.MORNING -> bfMedName = newName
            MealPeriod.LUNCH -> lunchMedName = newName
            MealPeriod.DINNER -> dinnerMedName = newName
            MealPeriod.NIGHT -> nightMedName = newName
        }
    }

    val currentDose = when (selectedPeriod) {
        MealPeriod.MORNING -> bfInsulin
        MealPeriod.LUNCH -> lunchInsulin
        MealPeriod.DINNER -> dinnerInsulin
        MealPeriod.NIGHT -> bedtimeInsulin
    }
    val onDoseChange: (String) -> Unit = { newDose ->
        when (selectedPeriod) {
            MealPeriod.MORNING -> bfInsulin = newDose
            MealPeriod.LUNCH -> lunchInsulin = newDose
            MealPeriod.DINNER -> dinnerInsulin = newDose
            MealPeriod.NIGHT -> bedtimeInsulin = newDose
        }
    }

    val currentMedTiming = when (selectedPeriod) {
        MealPeriod.MORNING -> bfMedTiming
        MealPeriod.LUNCH -> lunchMedTiming
        MealPeriod.DINNER -> dinnerMedTiming
        MealPeriod.NIGHT -> nightMedTiming
    }
    val onMedTimingChange: (String) -> Unit = { newTiming ->
        when (selectedPeriod) {
            MealPeriod.MORNING -> bfMedTiming = newTiming
            MealPeriod.LUNCH -> lunchMedTiming = newTiming
            MealPeriod.DINNER -> dinnerMedTiming = newTiming
            MealPeriod.NIGHT -> nightMedTiming = newTiming
        }
    }

    // 原相机拍照/相册识别药物逻辑
    val context = LocalContext.current
    var isRecognizing by remember { mutableStateOf(false) }
    var recognitionMessage by remember { mutableStateOf<String?>(null) }
    var recognitionMatched by remember { mutableStateOf(false) }
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
                            recognitionMatched = true
                            onCategoryChange(result.category)
                            onMedNameChange(result.matchedName)
                            if (result.suggestedDose != null) {
                                val doseStr = if (result.suggestedDose % 1f == 0f) {
                                    result.suggestedDose.toInt().toString()
                                } else {
                                    result.suggestedDose.toString()
                                }
                                onDoseChange(doseStr)
                            }
                            if (result.suggestedTiming != null && selectedPeriod != MealPeriod.NIGHT) {
                                onMedTimingChange(result.suggestedTiming)
                            }
                            val timingPart = if (result.suggestedTiming != null) "·${result.suggestedTiming}" else ""
                            val unitStr = MedicationData.detectUnit(result.matchedName, result.category)
                            val dosePart = if (result.suggestedDose != null) " ${if (result.suggestedDose % 1f == 0f) result.suggestedDose.toInt() else result.suggestedDose}$unitStr" else ""
                            val bgPart = if (result.detectedBG != null) " (检测到血糖 ${result.detectedBG} mmol/L)" else ""
                            recognitionMessage = "已识别：${result.matchedName}$dosePart$timingPart（${result.category.label}）$bgPart"
                        } else {
                            recognitionMatched = false
                            if (result.detectedBG != null) {
                                recognitionMessage = "未匹配到药物，但检测到血糖：${result.detectedBG} mmol/L"
                            } else {
                                recognitionMessage = "未匹配到列表内的药物，请手动输入药物名称"
                            }
                        }
                    }
                    .addOnFailureListener {
                        isRecognizing = false
                        recognitionMatched = false
                        recognitionMessage = "未识别到文字，请手动输入药物名称"
                    }
            } catch (_: Exception) {
                isRecognizing = false
                recognitionMatched = false
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

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val initialEpochMillis = remember(date) {
            try {
                LocalDate.parse(date).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
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
                            date = selectedLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
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
                        text = "选择记录日期",
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

    // 物理返回键优先关闭日期选择器
    if (showDatePicker) {
        BackHandler {
            showDatePicker = false
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = if (isKeyboardOpen) 6.dp else 16.dp,
                    bottom = if (isKeyboardOpen) 8.dp else 16.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            val maxDialogHeight = maxHeight
            val isDark = AppThemeColors.isDark

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .heightIn(max = maxDialogHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent dismissal on card click */ }
                    .testTag("record_edit_dialog"),
                shape = AppleCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White
                ),
                border = appleCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. 顶部紧凑栏：日期选择与关闭按钮（固定在Card顶部，圆角与关闭按钮永不被裁剪）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = ApplePillShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = appleCardBorder(),
                            modifier = Modifier
                                .clip(ApplePillShape)
                                .applePressEffect(0.95f)
                                .clickable { showDatePicker = true }
                                .testTag("dialog_date_picker_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = date,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.2).sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                                if (weekdayStr.isNotBlank()) {
                                    Text(
                                        text = weekdayStr,
                                        fontSize = 12.sp,
                                        letterSpacing = (-0.12).sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                    )
                                }
                                Text(
                                    text = "修改 >",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.12).sp,
                                    color = TealPrimary,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                        }

                        IconButton(
                            onClick = { dismissWithAnimation() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .applePressEffect(0.92f)
                                .testTag("dialog_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. 中间可滚动区域（表单内容在卡片内部独立滚动，顶部日期栏与底部保存栏保持固定）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState)
                            .onGloballyPositioned { coords ->
                                scrollContainerCoordinates = coords
                                if (isKeyboardOpen && activeFocusedCoordinates != null && !isScrollingAnimationRunning) {
                                    adjustScrollForField(activeFocusedCoordinates)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                            // 2. 时段选择 Tab 栏 (晨间, 午间, 傍晚, 睡前)
                            MealPeriodSelectorCapsule(
                                selectedPeriod = selectedPeriod,
                                onPeriodSelected = { selectedPeriod = it }
                            )

                            // 3. 仅展示选中时段的录入表单 (一次只添加/修改一个时段)
                            val currentThemeBg = when (selectedPeriod) {
                                MealPeriod.MORNING -> AppThemeColors.breakfastBg
                                MealPeriod.LUNCH -> AppThemeColors.lunchBg
                                MealPeriod.DINNER -> AppThemeColors.dinnerBg
                                MealPeriod.NIGHT -> AppThemeColors.bedtimeBg
                            }
                            val currentThemeColor = when (selectedPeriod) {
                                MealPeriod.MORNING -> AppThemeColors.breakfastColor
                                MealPeriod.LUNCH -> AppThemeColors.lunchColor
                                MealPeriod.DINNER -> AppThemeColors.dinnerColor
                                MealPeriod.NIGHT -> AppThemeColors.bedtimeColor
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(currentThemeBg)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 标题区
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${selectedPeriod.iconText} ${selectedPeriod.title}数据录入",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(currentThemeColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = selectedPeriod.periodTag,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = currentThemeColor
                                            )
                                        }
                                    }

                                    // 晨间时段显示昨夜参考
                                    if (selectedPeriod == MealPeriod.MORNING && prevNightInfo != null && prevNightInfo.dose != null) {
                                        Text(
                                            text = "昨夜: ${if (prevNightInfo.dose % 1f == 0f) prevNightInfo.dose.toInt() else prevNightInfo.dose}U",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (AppThemeColors.isDark) AppThemeColors.bedtimeColor else Color(0xFF4338CA)
                                        )
                                    }
                                }

                                // 第一行：血糖记录情况
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "血糖记录 (mmol/L)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    when (selectedPeriod) {
                                        MealPeriod.MORNING -> {
                                            // 晨间为 2 个数据：空腹（即餐前）、餐后2h
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                NumberField(
                                                    value = fastingBG,
                                                    onValueChange = { fastingBG = it },
                                                    label = "空腹血糖",
                                                    unit = "",
                                                    modifier = Modifier.weight(1f),
                                                    onFocusWithCoordinates = onFieldFocus
                                                )
                                                NumberField(
                                                    value = postBfBG,
                                                    onValueChange = { postBfBG = it },
                                                    label = "餐后2h",
                                                    unit = "",
                                                    modifier = Modifier.weight(1f),
                                                    onFocusWithCoordinates = onFieldFocus
                                                )
                                            }
                                        }
                                        MealPeriod.LUNCH -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                NumberField(
                                                    value = preLunchBG,
                                                    onValueChange = { preLunchBG = it },
                                                    label = "午餐前血糖",
                                                    unit = "",
                                                    modifier = Modifier.weight(1f),
                                                    onFocusWithCoordinates = onFieldFocus
                                                )
                                                NumberField(
                                                    value = postLunchBG,
                                                    onValueChange = { postLunchBG = it },
                                                    label = "午餐后2h血糖",
                                                    unit = "",
                                                    modifier = Modifier.weight(1f),
                                                    onFocusWithCoordinates = onFieldFocus
                                                )
                                            }
                                        }
                                        MealPeriod.DINNER -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                NumberField(
                                                    value = preDinnerBG,
                                                    onValueChange = { preDinnerBG = it },
                                                    label = "晚餐前血糖",
                                                    unit = "",
                                                    modifier = Modifier.weight(1f),
                                                    onFocusWithCoordinates = onFieldFocus
                                                )
                                                NumberField(
                                                    value = postDinnerBG,
                                                    onValueChange = { postDinnerBG = it },
                                                    label = "晚餐后2h血糖",
                                                    unit = "",
                                                    modifier = Modifier.weight(1f),
                                                    onFocusWithCoordinates = onFieldFocus
                                                )
                                            }
                                        }
                                        MealPeriod.NIGHT -> {
                                            NumberField(
                                                value = preNightBG,
                                                onValueChange = { preNightBG = it },
                                                label = "睡前血糖",
                                                unit = "",
                                                modifier = Modifier.fillMaxWidth(),
                                                onFocusWithCoordinates = onFieldFocus
                                            )
                                        }
                                    }
                                }

                                // 第二行：用药数据 (胰岛素 / 口服药分类，支持常用药物下拉与自由输入，自动单位识别，拍照识药)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "用药记录",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // 拍照识药按钮
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = TealPrimary.copy(alpha = 0.12f),
                                            border = BorderStroke(0.5.dp, TealPrimary.copy(alpha = 0.35f)),
                                            modifier = Modifier
                                                .clickable { showImageSourcePicker = true }
                                                .testTag("camera_scan_med_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoCamera,
                                                    contentDescription = "拍照识别药物",
                                                    tint = TealPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "拍照识药",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TealPrimary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    // 类别分类选择：胰岛素 vs 口服药 vs GLP-1/针剂
                                    MedCategorySelectorCapsule(
                                        selectedCategory = currentCategory,
                                        onCategorySelected = onCategoryChange
                                    )

                                    // 药名输入(口服药/胰岛素下拉+用户自由输入) + 自动识别单位的用药量输入
                                    val dynamicUnit = MedicationData.detectUnit(currentMedName, currentCategory)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MedicationDropdownField(
                                            value = currentMedName,
                                            onValueChange = onMedNameChange,
                                            category = currentCategory,
                                            options = MedicationData.getMedicationOptions(currentCategory, allRecords, medFrequencyMap),
                                            modifier = Modifier.weight(1.3f),
                                            onFocusWithCoordinates = onFieldFocus
                                        )

                                        NumberField(
                                            value = currentDose,
                                            onValueChange = onDoseChange,
                                            label = "用药量",
                                            unit = dynamicUnit,
                                            modifier = Modifier.weight(1f),
                                            onFocusWithCoordinates = onFieldFocus
                                        )
                                    }

                                    // 用药时机选择（餐前 / 餐中 / 餐后，睡前时段隐藏无意义时机）
                                    if (selectedPeriod != MealPeriod.NIGHT) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "时机:",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            MedicationTimingSelectorCapsule(
                                                selectedTiming = currentMedTiming,
                                                onTimingSelected = onMedTimingChange,
                                                modifier = Modifier.weight(1f),
                                                height = 36.dp
                                            )
                                        }
                                    }

                                    // OCR 识别状态与反馈提示
                                    OcrStatusBanner(
                                        isRecognizing = isRecognizing,
                                        recognitionMessage = recognitionMessage,
                                        onDismissMessage = { recognitionMessage = null }
                                    )
                                }

                                // 第三行：餐食记录 (让用户记录这一餐吃了什么东西)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "餐食记录",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    when (selectedPeriod) {
                                        MealPeriod.MORNING -> {
                                            DietInputField(
                                                value = bfDiet,
                                                onValueChange = { bfDiet = it },
                                                placeholder = "例：全麦面包1片、无糖豆浆、水煮蛋1个...",
                                                onFocusWithCoordinates = onFieldFocus
                                            )
                                        }
                                        MealPeriod.LUNCH -> {
                                            DietInputField(
                                                value = lunchDiet,
                                                onValueChange = { lunchDiet = it },
                                                placeholder = "例：杂粮米饭半碗、清蒸鲈鱼、西蓝花...",
                                                onFocusWithCoordinates = onFieldFocus
                                            )
                                        }
                                        MealPeriod.DINNER -> {
                                            DietInputField(
                                                value = dinnerDiet,
                                                onValueChange = { dinnerDiet = it },
                                                placeholder = "例：荞麦面少许、清炒时蔬、煎鸡胸肉...",
                                                onFocusWithCoordinates = onFieldFocus
                                            )
                                        }
                                        MealPeriod.NIGHT -> {
                                            DietInputField(
                                                value = nightDiet,
                                                onValueChange = { nightDiet = it },
                                                placeholder = "例：睡前加餐脱脂纯牛奶100ml，或无加餐...",
                                                onFocusWithCoordinates = onFieldFocus
                                            )
                                        }
                                    }
                                }

                                // 第四行：运动记录
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "运动记录",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    when (selectedPeriod) {
                                        MealPeriod.MORNING -> {
                                            DietInputField(
                                                value = bfExercise,
                                                onValueChange = { bfExercise = it },
                                                placeholder = "例：晨练散步30分钟、慢跑...",
                                                onFocusWithCoordinates = onFieldFocus
                                            )
                                        }
                                        MealPeriod.LUNCH -> {
                                            DietInputField(
                                                value = lunchExercise,
                                                onValueChange = { lunchExercise = it },
                                                placeholder = "例：午后散步20分钟...",
                                                onFocusWithCoordinates = onFieldFocus
                                            )
                                        }
                                        MealPeriod.DINNER -> {
                                            DietInputField(
                                                value = dinnerExercise,
                                                onValueChange = { dinnerExercise = it },
                                                placeholder = "例：傍晚快走40分钟、瑜伽...",
                                                onFocusWithCoordinates = onFieldFocus
                                            )
                                        }
                                        MealPeriod.NIGHT -> {
                                            DietInputField(
                                                value = nightExercise,
                                                onValueChange = { nightExercise = it },
                                                placeholder = "例：室内拉伸15分钟...",
                                                onFocusWithCoordinates = onFieldFocus
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. 全天可选备注
                            val notesBringIntoViewRequester = remember { BringIntoViewRequester() }
                            var notesFocused by remember { mutableStateOf(false) }
                            var notesCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("全天备注 (可选)", fontSize = 11.5.sp) },
                                placeholder = { Text("如：今日运动30分钟、轻微低血糖感...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(notesBringIntoViewRequester)
                                    .onGloballyPositioned { coords ->
                                        notesCoords = coords
                                        if (notesFocused) {
                                            onFieldFocus(coords)
                                        }
                                    }
                                    .onFocusChanged {
                                        notesFocused = it.isFocused
                                        if (it.isFocused) {
                                            notesCoords?.let { coords -> onFieldFocus(coords) }
                                            coroutineScope.launch {
                                                delay(250)
                                                notesBringIntoViewRequester.bringIntoView()
                                            }
                                        }
                                    },
                                maxLines = 2,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                        } // 结束中间可滚动区域 Column

                        // 3. 底部固定操作栏：分割线与保存/取消按钮（固定在Card底部，圆角完整且随时可直接点击保存）
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = ApplePillShape,
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .applePressEffect(0.95f)
                            ) {
                                Text(
                                    "取消",
                                    fontSize = 14.5.sp,
                                    letterSpacing = (-0.2).sp
                                )
                            }

                            Button(
                                onClick = {
                                    val trimmedDate = date.trim()
                                    if (trimmedDate.isNotBlank()) {
                                        val base = currentRecordForDate ?: InsulinRecord(date = trimmedDate)
                                        val newRecord = when (selectedPeriod) {
                                            MealPeriod.MORNING -> base.copy(
                                                date = trimmedDate,
                                                fastingBG = fastingBG.toFloatOrNull(),
                                                preBfBG = null,
                                                postBfBG = postBfBG.toFloatOrNull(),
                                                bfMedName = bfMedName.trim(),
                                                bfInsulin = bfInsulin.toFloatOrNull(),
                                                bfMedTiming = bfMedTiming.trim().ifBlank { "餐前" },
                                                bfDiet = bfDiet.trim(),
                                                bfExercise = bfExercise.trim(),
                                                notes = notes.trim()
                                            )
                                            MealPeriod.LUNCH -> base.copy(
                                                date = trimmedDate,
                                                preLunchBG = preLunchBG.toFloatOrNull(),
                                                postLunchBG = postLunchBG.toFloatOrNull(),
                                                lunchMedName = lunchMedName.trim(),
                                                lunchInsulin = lunchInsulin.toFloatOrNull(),
                                                lunchMedTiming = lunchMedTiming.trim().ifBlank { "餐前" },
                                                lunchDiet = lunchDiet.trim(),
                                                lunchExercise = lunchExercise.trim(),
                                                notes = notes.trim()
                                            )
                                            MealPeriod.DINNER -> base.copy(
                                                date = trimmedDate,
                                                preDinnerBG = preDinnerBG.toFloatOrNull(),
                                                postDinnerBG = postDinnerBG.toFloatOrNull(),
                                                dinnerMedName = dinnerMedName.trim(),
                                                dinnerInsulin = dinnerInsulin.toFloatOrNull(),
                                                dinnerMedTiming = dinnerMedTiming.trim().ifBlank { "餐前" },
                                                dinnerDiet = dinnerDiet.trim(),
                                                dinnerExercise = dinnerExercise.trim(),
                                                notes = notes.trim()
                                            )
                                            MealPeriod.NIGHT -> base.copy(
                                                date = trimmedDate,
                                                preNightBG = preNightBG.toFloatOrNull(),
                                                postNightBG = postNightBG.toFloatOrNull(),
                                                nightMedName = nightMedName.trim(),
                                                bedtimeInsulin = bedtimeInsulin.toFloatOrNull(),
                                                nightMedTiming = "睡前",
                                                nightDiet = nightDiet.trim(),
                                                nightExercise = nightExercise.trim(),
                                                notes = notes.trim()
                                            )
                                        }
                                        onSave(newRecord)
                                    }
                                },
                                shape = ApplePillShape,
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                modifier = Modifier
                                    .height(42.dp)
                                    .applePressEffect(0.95f)
                                    .testTag("save_record_button")
                            ) {
                                Text(
                                    "保存${selectedPeriod.title}记录",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.2).sp,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                        }
                    } // 结束 Card 内顶层 Column
                } // 结束 Card
        } // 结束 BoxWithConstraints
    } // 结束最外层暗色蒙层 Box
} // 结束 RecordEditDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    modifier: Modifier = Modifier,
    onFocusWithCoordinates: ((LayoutCoordinates) -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var currentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(input)
            }
        },
        label = { Text(label, fontSize = 11.sp) },
        suffix = if (unit.isNotBlank()) {
            { Text(unit, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        modifier = modifier
            .height(54.dp)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onGloballyPositioned { coords ->
                currentCoords = coords
                if (isFocused) {
                    onFocusWithCoordinates?.invoke(coords)
                }
            }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    currentCoords?.let { onFocusWithCoordinates?.invoke(it) }
                    coroutineScope.launch {
                        delay(250)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TealPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MedicationDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    category: MedCategory,
    options: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    onFocusWithCoordinates: ((LayoutCoordinates) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val displayOptions = remember(options, category) {
        if (options.isNotEmpty()) options else when (category) {
            MedCategory.ORAL -> MedicationData.commonOralMeds
            MedCategory.INSULIN -> MedicationData.commonInsulinMeds
            MedCategory.GLP1 -> MedicationData.commonGLP1Meds
        }
    }
    var isFocused by remember { mutableStateOf(false) }
    var currentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    when (category) {
                        MedCategory.ORAL -> "口服药名"
                        MedCategory.INSULIN -> "胰岛素名"
                        MedCategory.GLP1 -> "针剂/GLP-1药名"
                    },
                    fontSize = 11.sp
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onGloballyPositioned { coords ->
                    currentCoords = coords
                    if (isFocused) {
                        onFocusWithCoordinates?.invoke(coords)
                    }
                }
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (focusState.isFocused) {
                        currentCoords?.let { onFocusWithCoordinates?.invoke(it) }
                        coroutineScope.launch {
                            delay(250)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                },
            trailingIcon = {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "选择常用药物",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .widthIn(min = 180.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            displayOptions.forEach { option ->
                val isGeneric = option == "胰岛素" || option == "口服药"
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                fontWeight = if (option == value) FontWeight.Bold else FontWeight.Normal,
                                color = if (option == value) TealPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isGeneric) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = TealPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "通用选项",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TealPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DietInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onFocusWithCoordinates: ((LayoutCoordinates) -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var currentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("吃了什么东西", fontSize = 11.sp) },
        placeholder = { Text(placeholder, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onGloballyPositioned { coords ->
                currentCoords = coords
                if (isFocused) {
                    onFocusWithCoordinates?.invoke(coords)
                }
            }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    currentCoords?.let { onFocusWithCoordinates?.invoke(it) }
                    coroutineScope.launch {
                        delay(250)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        maxLines = 2,
        shape = RoundedCornerShape(12.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TealPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    )
}
