package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BGLevel
import com.example.data.BGUtils
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.data.MedicationData
import com.example.ui.ItemType
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItemDialog(
    initialDate: String?,
    initialPeriod: MealPeriod? = null,
    initialItemType: ItemType? = null,
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

    // 当前日期已有记录（用于参考与提示）
    val currentRecordForDate = remember(selectedDate, allRecords) {
        allRecords.find { it.date == selectedDate }
    }

    // 表单输入状态
    var bgInputText by remember { mutableStateOf("") }
    var dietInputText by remember { mutableStateOf("") }
    var medNameInputText by remember { mutableStateOf("门冬胰岛素") }
    var medDoseInputText by remember { mutableStateOf("") }
    var medTimingChoice by remember { mutableStateOf("餐前") }
    var postMealTagChoice by remember { mutableStateOf("餐后2小时") }
    var postMealTimeInputText by remember { mutableStateOf(nowTimeStr) }

    var showDatePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // 拦截物理/手势返回键
    BackHandler {
        if (showDatePicker) {
            showDatePicker = false
        } else {
            onDismiss()
        }
    }

    // 切换时段时自动同步时段专属默认值
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
    }

    // 日期选择器
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
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .clickable(enabled = false) {}
                .testTag("add_item_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // 1. 标题与操作栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(TealPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("➕", fontSize = 16.sp)
                        }
                        Column {
                            Text(
                                text = "添加记录条目",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "一次增加一个条目，精准归纳时段",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. 日期选择卡片与系统时间识别提示
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showDatePicker = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "选择日期",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formatChineseDate(selectedDate),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (selectedDate == today) "(今天)" else "(点此更换)",
                                    fontSize = 11.sp,
                                    color = TealPrimary
                                )
                            }

                            // 识别提示标签
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TealPrimary.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "系统识别: $nowTimeStr",
                                    fontSize = 10.5.sp,
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 已根据系统当前时间自动为您匹配到「${autoIdentifiedPeriod.title}」，亦可自由点击切换其他时段：",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. 时段选择（早、中、晚、睡前）
                Text(
                    text = "选择所属时段：",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MealPeriod.entries.forEach { period ->
                        val isSelected = period == selectedPeriod
                        val periodBg = when (period) {
                            MealPeriod.MORNING -> AppThemeColors.breakfastColor
                            MealPeriod.LUNCH -> AppThemeColors.lunchColor
                            MealPeriod.DINNER -> AppThemeColors.dinnerColor
                            MealPeriod.NIGHT -> AppThemeColors.bedtimeColor
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) periodBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) periodBg else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPeriodChanged(period) }
                                .testTag("period_choice_${period.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = period.iconText,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = period.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. 条目类型选择（餐前血糖，用餐情况，用药，餐后血糖）
                Text(
                    text = "自由选择这次增加什么条目：",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ItemType.entries.forEach { itemType ->
                        val isSelected = itemType == selectedItemType
                        val label = when (itemType) {
                            ItemType.PRE_MEAL_BG -> if (selectedPeriod == MealPeriod.MORNING) "空腹血糖" else if (selectedPeriod == MealPeriod.NIGHT) "睡前血糖" else "餐前血糖"
                            ItemType.DIET -> "用餐情况"
                            ItemType.MEDICATION -> "用药"
                            ItemType.POST_MEAL_BG -> "餐后血糖"
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) TealPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedItemType = itemType }
                                .testTag("item_type_${itemType.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = itemType.icon, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                if (itemType == ItemType.POST_MEAL_BG) {
                                    Text(
                                        text = "可多条",
                                        fontSize = 8.5.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else TealPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(14.dp))

                // 5. 针对所选条目类型的专属输入区域
                when (selectedItemType) {
                    ItemType.PRE_MEAL_BG -> {
                        PreMealBGSection(
                            period = selectedPeriod,
                            valueText = bgInputText,
                            onValueChange = { bgInputText = it }
                        )
                    }
                    ItemType.DIET -> {
                        DietSection(
                            period = selectedPeriod,
                            dietText = dietInputText,
                            onDietChange = { dietInputText = it }
                        )
                    }
                    ItemType.MEDICATION -> {
                        MedicationSection(
                            period = selectedPeriod,
                            medName = medNameInputText,
                            onMedNameChange = { medNameInputText = it },
                            dose = medDoseInputText,
                            onDoseChange = { medDoseInputText = it },
                            timing = medTimingChoice,
                            onTimingChange = { medTimingChoice = it }
                        )
                    }
                    ItemType.POST_MEAL_BG -> {
                        val existingPosts = remember(currentRecordForDate, selectedPeriod) {
                            currentRecordForDate?.getPostMealList(selectedPeriod) ?: emptyList()
                        }
                        PostMealBGSection(
                            period = selectedPeriod,
                            valueText = bgInputText,
                            onValueChange = { bgInputText = it },
                            tagText = postMealTagChoice,
                            onTagChange = { postMealTagChoice = it },
                            timeText = postMealTimeInputText,
                            onTimeChange = { postMealTimeInputText = it },
                            existingEntries = existingPosts
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 6. 底部操作按钮：取消、保存并继续增加、保存
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val bgVal = bgInputText.trim().toFloatOrNull()
                            val doseVal = medDoseInputText.trim().toFloatOrNull()
                            onSaveItem(
                                selectedDate,
                                selectedPeriod,
                                selectedItemType,
                                bgVal,
                                dietInputText.trim(),
                                medNameInputText.trim(),
                                doseVal,
                                medTimingChoice,
                                postMealTagChoice,
                                postMealTimeInputText.trim(),
                                true // keepDialogOpen
                            )
                            // 清理单条输入框，准备增加下一个条目
                            bgInputText = ""
                            dietInputText = ""
                            medDoseInputText = ""
                        },
                        enabled = isInputValid,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("保存并再加", fontSize = 12.5.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            val bgVal = bgInputText.trim().toFloatOrNull()
                            val doseVal = medDoseInputText.trim().toFloatOrNull()
                            onSaveItem(
                                selectedDate,
                                selectedPeriod,
                                selectedItemType,
                                bgVal,
                                dietInputText.trim(),
                                medNameInputText.trim(),
                                doseVal,
                                medTimingChoice,
                                postMealTagChoice,
                                postMealTimeInputText.trim(),
                                false // close dialog
                            )
                        },
                        enabled = isInputValid,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_save_item")
                    ) {
                        Text("保存条目", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreMealBGSection(
    period: MealPeriod,
    valueText: String,
    onValueChange: (String) -> Unit
) {
    val title = when (period) {
        MealPeriod.MORNING -> "晨间 · 空腹血糖"
        MealPeriod.NIGHT -> "睡前 · 睡前血糖"
        else -> "${period.title} · 餐前血糖"
    }

    val floatVal = valueText.toFloatOrNull()
    val status = when (period) {
        MealPeriod.MORNING -> BGUtils.evaluateFasting(floatVal)
        else -> BGUtils.evaluatePreMeal(floatVal)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (status != null) {
                val (badgeBg, badgeText) = when (status.level) {
                    BGLevel.NORMAL -> Color(0xFFECFDF5) to Color(0xFF059669)
                    BGLevel.LOW -> Color(0xFFFEF2F2) to Color(0xFFDC2626)
                    BGLevel.HIGH -> Color(0xFFFFFBEB) to Color(0xFFD97706)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "达标状态: ${status.label} (${status.valueText} mmol/L)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = valueText,
                onValueChange = onValueChange,
                label = { Text("血糖数值 (mmol/L)") },
                placeholder = { Text("例如：5.8") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_pre_bg"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    focusedLabelColor = TealPrimary
                )
            )

            // 快速微调按钮
            StepperButton(text = "-0.1") {
                val curr = valueText.toFloatOrNull() ?: 6.0f
                onValueChange(String.format(Locale.US, "%.1f", (curr - 0.1f).coerceAtLeast(0f)))
            }
            StepperButton(text = "+0.1") {
                val curr = valueText.toFloatOrNull() ?: 6.0f
                onValueChange(String.format(Locale.US, "%.1f", curr + 0.1f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "参考标准：空腹/餐前血糖标准参考值 3.9 ~ 7.0 mmol/L",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))
        // 快捷预设芯片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("5.0", "5.5", "6.0", "6.5", "7.0").forEach { preset ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { onValueChange(preset) }
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = preset,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DietSection(
    period: MealPeriod,
    dietText: String,
    onDietChange: (String) -> Unit
) {
    val quickTags = when (period) {
        MealPeriod.MORNING -> listOf("燕麦片", "水煮蛋", "纯牛奶", "无糖豆浆", "全麦面包", "半根玉米", "鸡蛋羹")
        MealPeriod.LUNCH -> listOf("杂粮饭半碗", "荞麦面", "清蒸鲈鱼", "清炒西兰花", "番茄炒蛋", "白灼大虾", "去皮鸡胸肉")
        MealPeriod.DINNER -> listOf("紫薯小半个", "豆腐蔬菜汤", "白灼菜心", "清炖鸡汤", "蒜蓉生菜", "水煮牛肉片")
        MealPeriod.NIGHT -> listOf("温开水一杯", "无糖酸奶半杯", "少量坚果", "无夜宵")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${period.title} · 用餐情况记录",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dietText,
            onValueChange = onDietChange,
            label = { Text("记录餐食食谱与饮食情况") },
            placeholder = { Text("例如：全麦面包两片、低脂纯牛奶、煎荷包蛋") },
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_diet"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary,
                focusedLabelColor = TealPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点按快捷添加常用餐食：",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickTags.forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TealPrimary.copy(alpha = 0.09f),
                    border = BorderStroke(0.5.dp, TealPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.clickable {
                        val newText = if (dietText.isBlank()) tag else "$dietText、$tag"
                        onDietChange(newText)
                    }
                ) {
                    Text(
                        text = "+ $tag",
                        fontSize = 11.sp,
                        color = TealPrimary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicationSection(
    period: MealPeriod,
    medName: String,
    onMedNameChange: (String) -> Unit,
    dose: String,
    onDoseChange: (String) -> Unit,
    timing: String,
    onTimingChange: (String) -> Unit
) {
    val commonMeds = listOf(
        "门冬胰岛素", "赖脯胰岛素", "甘精胰岛素", "地特胰岛素", "德谷胰岛素",
        "预混胰岛素(30R)", "二甲双胍", "阿卡波糖", "达格列净", "利格列汀"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${period.title} · 用药记录",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 药物名称输入与常用药品
        OutlinedTextField(
            value = medName,
            onValueChange = onMedNameChange,
            label = { Text("药品名称") },
            placeholder = { Text("例如：门冬胰岛素") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_med_name"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary,
                focusedLabelColor = TealPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickChoices = if (period == MealPeriod.NIGHT) {
                listOf("甘精胰岛素", "地特胰岛素", "德谷胰岛素")
            } else {
                listOf("门冬胰岛素", "赖脯胰岛素", "二甲双胍")
            }
            quickChoices.forEach { name ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (medName == name) TealPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(0.5.dp, if (medName == name) TealPrimary else Color.Transparent),
                    modifier = Modifier.clickable { onMedNameChange(name) }
                ) {
                    Text(
                        text = name,
                        fontSize = 11.sp,
                        color = if (medName == name) TealPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 剂量输入
        val unit = MedicationData.detectUnit(medName)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = dose,
                onValueChange = onDoseChange,
                label = { Text("用药剂量 ($unit)") },
                placeholder = { Text("例如：8") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_med_dose"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    focusedLabelColor = TealPrimary
                )
            )

            StepperButton(text = "-1") {
                val curr = dose.toFloatOrNull() ?: 8f
                onDoseChange(if ((curr - 1f) % 1f == 0f) (curr - 1f).toInt().coerceAtLeast(0).toString() else String.format(Locale.US, "%.1f", (curr - 1f).coerceAtLeast(0f)))
            }
            StepperButton(text = "+1") {
                val curr = dose.toFloatOrNull() ?: 8f
                onDoseChange(if ((curr + 1f) % 1f == 0f) (curr + 1f).toInt().toString() else String.format(Locale.US, "%.1f", curr + 1f))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 用药时机
        Text(
            text = "用药时机：",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val timings = if (period == MealPeriod.NIGHT) listOf("睡前", "餐后", "餐前") else listOf("餐前", "餐中", "餐后")
            timings.forEach { t ->
                val isSelected = timing == t
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onTimingChange(t) }
                ) {
                    Text(
                        text = t,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PostMealBGSection(
    period: MealPeriod,
    valueText: String,
    onValueChange: (String) -> Unit,
    tagText: String,
    onTagChange: (String) -> Unit,
    timeText: String,
    onTimeChange: (String) -> Unit,
    existingEntries: List<com.example.data.PostMealEntry>
) {
    val floatVal = valueText.toFloatOrNull()
    val status = BGUtils.evaluatePostMeal(floatVal)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${period.title} · 餐后血糖",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (status != null) {
                val (badgeBg, badgeText) = when (status.level) {
                    BGLevel.NORMAL -> Color(0xFFECFDF5) to Color(0xFF059669)
                    BGLevel.LOW -> Color(0xFFFEF2F2) to Color(0xFFDC2626)
                    BGLevel.HIGH -> Color(0xFFFFFBEB) to Color(0xFFD97706)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "餐后达标: ${status.label} (${status.valueText} mmol/L)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        // 多条餐后记录提示
        if (existingEntries.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TealPrimary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text(
                        text = "该时段已有 ${existingEntries.size} 条餐后记录：",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealPrimary
                    )
                    existingEntries.forEachIndexed { idx, entry ->
                        Text(
                            text = "  #${idx + 1}: ${String.format(Locale.US, "%.1f", entry.value)} mmol/L ${if (entry.tag.isNotBlank()) "(${entry.tag})" else ""} ${entry.time}",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "✨ 再次保存将作为该时段第 ${existingEntries.size + 1} 条餐后血糖独立增加！",
                        fontSize = 10.5.sp,
                        color = TealPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 数值输入
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = valueText,
                onValueChange = onValueChange,
                label = { Text("餐后血糖数值 (mmol/L)") },
                placeholder = { Text("例如：7.8") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_post_bg"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    focusedLabelColor = TealPrimary
                )
            )

            StepperButton(text = "-0.1") {
                val curr = valueText.toFloatOrNull() ?: 7.5f
                onValueChange(String.format(Locale.US, "%.1f", (curr - 0.1f).coerceAtLeast(0f)))
            }
            StepperButton(text = "+0.1") {
                val curr = valueText.toFloatOrNull() ?: 7.5f
                onValueChange(String.format(Locale.US, "%.1f", curr + 0.1f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 标签/时机（餐后1小时、餐后2小时、餐后3小时、加测）
        Text(
            text = "记录标签（可选择或自定义时间）：",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("餐后1小时", "餐后2小时", "餐后3小时", "加测").forEach { tag ->
                val isSelected = tagText == tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onTagChange(tag) }
                ) {
                    Text(
                        text = tag,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "测量时间点: ",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = timeText,
                onValueChange = onTimeChange,
                singleLine = true,
                modifier = Modifier
                    .width(90.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "(标准参考: 餐后2h ≤ 10.0 mmol/L)",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StepperButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
        )
    }
}

private fun formatChineseDate(dateStr: String): String {
    return try {
        val ld = LocalDate.parse(dateStr)
        val weekday = when (ld.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "周一"
            java.time.DayOfWeek.TUESDAY -> "周二"
            java.time.DayOfWeek.WEDNESDAY -> "周三"
            java.time.DayOfWeek.THURSDAY -> "周四"
            java.time.DayOfWeek.FRIDAY -> "周五"
            java.time.DayOfWeek.SATURDAY -> "周六"
            java.time.DayOfWeek.SUNDAY -> "周日"
        }
        "${ld.year}年${ld.monthValue}月${ld.dayOfMonth}日 $weekday"
    } catch (_: Exception) {
        dateStr
    }
}
