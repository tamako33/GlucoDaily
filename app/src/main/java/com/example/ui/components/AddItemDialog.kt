package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
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

    // 表单状态
    var bgInputText by remember { mutableStateOf("") }
    var dietInputText by remember { mutableStateOf("") }
    var exerciseInputText by remember { mutableStateOf("") }
    var medNameInputText by remember { mutableStateOf(if (selectedPeriod == MealPeriod.NIGHT) "甘精胰岛素" else "门冬胰岛素") }
    var medDoseInputText by remember { mutableStateOf("") }
    var medTimingChoice by remember { mutableStateOf(if (selectedPeriod == MealPeriod.NIGHT) "睡前" else "餐前") }
    var postMealTagChoice by remember { mutableStateOf("餐后2小时") }
    var postMealTimeInputText by remember { mutableStateOf(nowTimeStr) }

    var showDatePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // 切换时段联动默认药名与时机
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

    // 物理返回键处理
    BackHandler {
        if (showDatePicker) {
            showDatePicker = false
        } else {
            onDismiss()
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
        ItemType.EXERCISE -> {
            exerciseInputText.trim().isNotBlank()
        }
    }

    fun submit(keepOpen: Boolean) {
        if (!isInputValid) return
        onSaveItem(
            selectedDate,
            selectedPeriod,
            selectedItemType,
            bgInputText.trim().toFloatOrNull(),
            dietInputText.trim(),
            medNameInputText.trim(),
            medDoseInputText.trim().toFloatOrNull(),
            medTimingChoice,
            postMealTagChoice,
            postMealTimeInputText.trim(),
            exerciseInputText.trim(),
            keepOpen
        )
        if (keepOpen) {
            bgInputText = ""
            dietInputText = ""
            medDoseInputText = ""
            exerciseInputText = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {}
                .testTag("add_item_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        onClick = onDismiss,
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
                                .clickable { selectedItemType = itemType }
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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 餐后阶段快捷标签选择
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("餐后1小时", "餐后2小时", "餐后3小时", "加餐后").forEach { tag ->
                                    val isSel = postMealTagChoice == tag
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { postMealTagChoice = tag }
                                    ) {
                                        Text(
                                            text = tag.replace("小时", "h"),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = exerciseInputText,
                                onValueChange = { exerciseInputText = it },
                                label = { Text("运动项目与时长") },
                                placeholder = { Text("例: 散步30分钟、慢跑20分钟") },
                                singleLine = true,
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
                        val unit = MedicationData.detectUnit(medNameInputText)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 药名选择
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = medNameInputText,
                                    onValueChange = { medNameInputText = it },
                                    label = { Text("药物名称") },
                                    placeholder = { Text("如：门冬胰岛素") },
                                    trailingIcon = {
                                        IconButton(onClick = { medDropdownExpanded = true }) {
                                            Text("▼", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                DropdownMenu(
                                    expanded = medDropdownExpanded,
                                    onDismissRequest = { medDropdownExpanded = false }
                                ) {
                                    (MedicationData.commonInsulinMeds.take(4) + MedicationData.commonOralMeds.take(4)).forEach { med ->
                                        DropdownMenuItem(
                                            text = { Text(med) },
                                            onClick = {
                                                medNameInputText = med
                                                medDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 剂量与时机
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = medDoseInputText,
                                    onValueChange = { medDoseInputText = it },
                                    label = { Text("用药剂量") },
                                    placeholder = { Text("例: 6") },
                                    trailingIcon = { Text(unit, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary, modifier = Modifier.padding(end = 8.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                // 时机选择
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(2.dp)
                                ) {
                                    listOf("餐前", "餐后", "睡前").forEach { timing ->
                                        val isSel = medTimingChoice == timing
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) TealPrimary else Color.Transparent)
                                                .clickable { medTimingChoice = timing }
                                            .padding(horizontal = 8.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = timing,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 5. 底部操作栏（极简双按钮）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { submit(keepOpen = true) },
                        enabled = isInputValid
                    ) {
                        Text("保存并再加", color = if (isInputValid) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { submit(keepOpen = false) },
                        enabled = isInputValid,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存条目", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
