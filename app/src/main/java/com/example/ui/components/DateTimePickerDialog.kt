package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.TealPrimary
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 日期与时间双模式选择弹窗 (DateTimePickerDialog)：
 * - 支持日期日历选择与 24 小时制时间转盘选择；
 * - 提供“设为现在”一键快捷同步当前系统时间；
 * - 纯净深浅色自适应无白边卡片设计。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    currentDate: String,
    currentTime: String,
    onDismissRequest: () -> Unit,
    onConfirm: (date: String, time: String) -> Unit,
    onSetToNow: (date: String, time: String) -> Unit
) {
    val isDark = AppThemeColors.isDark

    val initialEpoch = remember(currentDate) {
        try {
            LocalDate.parse(currentDate).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpoch)

    val (initHour, initMin) = remember(currentTime) {
        try {
            val parts = currentTime.split(":")
            parts[0].toInt() to parts[1].toInt()
        } catch (_: Exception) {
            val now = LocalTime.now()
            now.hour to now.minute
        }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initHour,
        initialMinute = initMin,
        is24Hour = true
    )

    var pickerTab by remember { mutableIntStateOf(0) } // 0: 日期, 1: 时间

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(AppleCardShape),
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
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. 顶栏：标题 + 快捷设为现在按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择日期与时间",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.28).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = ApplePillShape,
                        color = TealPrimary.copy(alpha = if (isDark) 0.18f else 0.10f),
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .clip(ApplePillShape)
                            .applePressEffect(0.95f)
                            .clickable {
                                val nowD = LocalDate.now()
                                val nowT = LocalTime.now()
                                val newDate = nowD.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val newTime = String.format(Locale.getDefault(), "%02d:%02d", nowT.hour, nowT.minute)
                                onSetToNow(newDate, newTime)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("⚡", fontSize = 11.sp)
                            Text(
                                text = "设为现在",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.12).sp,
                                color = TealPrimary
                            )
                        }
                    }
                }

                // 2. 日期 / 时间 选项卡切换（Apple 胶囊分段轨道）
                val tempDateStr = datePickerState.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().format(DateTimeFormatter.ofPattern("MM-dd"))
                } ?: currentDate.substring(5)
                val tempTimeStr = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)

                val trackBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA).copy(alpha = 0.6f)
                val trackBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppleSegmentTrackShape)
                        .background(trackBg)
                        .border(BorderStroke(1.dp, trackBorderColor), AppleSegmentTrackShape)
                        .padding(2.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        0 to "📅 日期 ($tempDateStr)",
                        1 to "🕒 时间 ($tempTimeStr)"
                    ).forEach { (idx, label) ->
                        val isSel = pickerTab == idx
                        Surface(
                            shape = AppleSegmentThumbShape,
                            color = if (isSel) TealPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(AppleSegmentThumbShape)
                                .applePressEffect(0.96f)
                                .clickable { pickerTab = idx }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                                    letterSpacing = (-0.224).sp,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 3. 内容区：DatePicker 或 TimePicker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 310.dp, max = 370.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (pickerTab == 0) {
                        DatePicker(
                            state = datePickerState,
                            colors = DatePickerDefaults.colors(
                                containerColor = Color.Transparent,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                headlineContentColor = MaterialTheme.colorScheme.onSurface,
                                weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                subheadContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationContentColor = MaterialTheme.colorScheme.onSurface,
                                yearContentColor = MaterialTheme.colorScheme.onSurface,
                                currentYearContentColor = TealPrimary,
                                selectedYearContentColor = Color.White,
                                selectedYearContainerColor = TealPrimary,
                                dayContentColor = MaterialTheme.colorScheme.onSurface,
                                selectedDayContentColor = Color.White,
                                selectedDayContainerColor = TealPrimary,
                                todayDateBorderColor = TealPrimary,
                                todayContentColor = TealPrimary,
                                dividerColor = Color.Transparent
                            ),
                            title = null,
                            headline = null,
                            showModeToggle = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimePicker(
                                state = timePickerState,
                                colors = TimePickerDefaults.colors(
                                    clockDialColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    clockDialSelectedContentColor = Color.White,
                                    clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                                    selectorColor = TealPrimary,
                                    containerColor = Color.Transparent,
                                    periodSelectorBorderColor = TealPrimary,
                                    periodSelectorSelectedContainerColor = TealPrimary,
                                    periodSelectorUnselectedContainerColor = Color.Transparent,
                                    periodSelectorSelectedContentColor = Color.White,
                                    periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                                    timeSelectorSelectedContainerColor = TealPrimary.copy(alpha = 0.18f),
                                    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    timeSelectorSelectedContentColor = TealPrimary,
                                    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                layoutType = TimePickerLayoutType.Vertical
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // 4. 底部操作按钮栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.applePressEffect(0.95f)
                    ) {
                        Text(
                            "取消",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            letterSpacing = (-0.2).sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val chosenDate = datePickerState.selectedDateMillis?.let { millis ->
                                val ld = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                                ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            } ?: currentDate
                            val chosenTime = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
                            onConfirm(chosenDate, chosenTime)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = ApplePillShape,
                        modifier = Modifier
                            .height(42.dp)
                            .applePressEffect(0.95f)
                    ) {
                        Text(
                            "确定",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            letterSpacing = (-0.2).sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
