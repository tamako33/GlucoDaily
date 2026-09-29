package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.data.MedCategory
import com.example.data.MedicationData
import com.example.data.PostMealEntry
import com.example.data.PostMealUtils
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TealPrimary
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 记录类型枚举（与标准模式条目逻辑对齐）
 */
enum class CareRecordType(
    val title: String,
    val shortTitle: String,
    val icon: String,
    val unit: String
) {
    FASTING_OR_PRE("餐前/空腹血糖", "餐前", "🩸", "mmol/L"),
    POST_MEAL("餐后血糖", "餐后", "🩸", "mmol/L"),
    MEDICATION("用药记录", "用药", "💊", ""),
    DIET("饮食记录", "饮食", "🥣", ""),
    EXERCISE("运动记录", "运动", "🏃", "")
}

/**
 * 每个餐段的多条目编辑草稿
 */
class CareMealDraft(
    preBG: String = "",
    postBG: String = "",
    postTag: String = "餐后2h",
    medCategory: MedCategory = MedCategory.INSULIN,
    medName: String = "门冬胰岛素",
    medDose: String = "",
    medTiming: String = "餐前",
    diet: String = "",
    exercise: String = ""
) {
    var preBG by mutableStateOf(preBG)
    var postBG by mutableStateOf(postBG)
    var postTag by mutableStateOf(postTag)
    var medCategory by mutableStateOf(medCategory)
    var medName by mutableStateOf(medName)
    var medDose by mutableStateOf(medDose)
    var medTiming by mutableStateOf(medTiming)
    var diet by mutableStateOf(diet)
    var exercise by mutableStateOf(exercise)

    fun hasData(type: CareRecordType): Boolean {
        return when (type) {
            CareRecordType.FASTING_OR_PRE -> preBG.isNotBlank()
            CareRecordType.POST_MEAL -> postBG.isNotBlank()
            CareRecordType.MEDICATION -> medDose.isNotBlank() || (medName.isNotBlank() && medName != "胰岛素")
            CareRecordType.DIET -> diet.isNotBlank()
            CareRecordType.EXERCISE -> exercise.isNotBlank()
        }
    }

    fun countFilledItems(): Int {
        var count = 0
        if (preBG.isNotBlank()) count++
        if (postBG.isNotBlank()) count++
        if (medDose.isNotBlank() || (medName.isNotBlank() && medName != "胰岛素")) count++
        if (diet.isNotBlank()) count++
        if (exercise.isNotBlank()) count++
        return count
    }
}

/**
 * 关怀模式专属适老化大字记录弹窗 (CareRecordDialog)：
 * 1. 结构与标准模式完全一致：顶部为餐段与条目选项卡，中间聚焦展示当前条目大字编辑界面，杜绝一屏罗列过多条目的拥挤；
 * 2. 彻底消除文字垂直截断：采用 Surface + 居中 BasicTextField，零内置边距侵占，32sp 粗体数值完整舒展；
 * 3. 气泡框视觉质感升级：所有选项卡与快速胶囊赋予实心/高对比边框，内边距舒适，绝无挤压；
 * 4. 完善用药选框：大字药名输入/选择框、下拉菜单点选历史药物、快捷药名气泡、用药时机（餐前/餐中/餐后）、加减步进与常用剂量胶囊；
 * 5. 跨选项卡自动保留修改草稿，底部一键批量保存并进行自然语音播报。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CareRecordDialog(
    initialDate: String,
    initialPeriod: MealPeriod = MealPeriod.MORNING,
    initialType: CareRecordType = CareRecordType.FASTING_OR_PRE,
    existingRecord: InsulinRecord?,
    allRecords: List<InsulinRecord> = emptyList(),
    onDismiss: () -> Unit,
    onSaveRecord: (InsulinRecord, String) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val haptic = LocalHapticFeedback.current

    var selectedPeriod by remember { mutableStateOf(initialPeriod) }
    var selectedType by remember { mutableStateOf(initialType) }

    // 初始化4个餐段的编辑草稿
    val drafts = remember(existingRecord) {
        val map = mutableStateMapOf<MealPeriod, CareMealDraft>()
        MealPeriod.entries.forEach { p ->
            val pre = when (p) {
                MealPeriod.MORNING -> existingRecord?.fastingBG
                MealPeriod.LUNCH -> existingRecord?.preLunchBG
                MealPeriod.DINNER -> existingRecord?.preDinnerBG
                MealPeriod.NIGHT -> existingRecord?.preNightBG
            }
            val postList = existingRecord?.getPostMealList(p) ?: emptyList()
            val postEntry = postList.firstOrNull()
            val post = postEntry?.value ?: when (p) {
                MealPeriod.MORNING -> existingRecord?.postBfBG
                MealPeriod.LUNCH -> existingRecord?.postLunchBG
                MealPeriod.DINNER -> existingRecord?.postDinnerBG
                MealPeriod.NIGHT -> existingRecord?.postNightBG
            }
            val postTag = postEntry?.tag ?: "餐后2h"

            val dose = when (p) {
                MealPeriod.MORNING -> existingRecord?.bfInsulin
                MealPeriod.LUNCH -> existingRecord?.lunchInsulin
                MealPeriod.DINNER -> existingRecord?.dinnerInsulin
                MealPeriod.NIGHT -> existingRecord?.bedtimeInsulin
            }
            val name = when (p) {
                MealPeriod.MORNING -> existingRecord?.bfMedName ?: ""
                MealPeriod.LUNCH -> existingRecord?.lunchMedName ?: ""
                MealPeriod.DINNER -> existingRecord?.dinnerMedName ?: ""
                MealPeriod.NIGHT -> existingRecord?.nightMedName ?: ""
            }
            val timing = when (p) {
                MealPeriod.MORNING -> existingRecord?.bfMedTiming ?: "餐前"
                MealPeriod.LUNCH -> existingRecord?.lunchMedTiming ?: "餐前"
                MealPeriod.DINNER -> existingRecord?.dinnerMedTiming ?: "餐前"
                MealPeriod.NIGHT -> existingRecord?.nightMedTiming ?: "睡前"
            }
            val diet = when (p) {
                MealPeriod.MORNING -> existingRecord?.bfDiet ?: ""
                MealPeriod.LUNCH -> existingRecord?.lunchDiet ?: ""
                MealPeriod.DINNER -> existingRecord?.dinnerDiet ?: ""
                MealPeriod.NIGHT -> existingRecord?.nightDiet ?: ""
            }
            val ex = when (p) {
                MealPeriod.MORNING -> existingRecord?.bfExercise ?: ""
                MealPeriod.LUNCH -> existingRecord?.lunchExercise ?: ""
                MealPeriod.DINNER -> existingRecord?.dinnerExercise ?: ""
                MealPeriod.NIGHT -> existingRecord?.nightExercise ?: ""
            }

            val category = when {
                (dose ?: 0f) > 0f -> MedCategory.INSULIN
                name.isNotBlank() -> MedicationData.inferCategory(name)
                else -> MedCategory.INSULIN
            }

            map[p] = CareMealDraft(
                preBG = pre?.let { String.format(Locale.US, "%.1f", it) } ?: "",
                postBG = post?.let { String.format(Locale.US, "%.1f", it) } ?: "",
                postTag = postTag,
                medCategory = category,
                medName = if (name.isNotBlank()) name else (if (p == MealPeriod.NIGHT) "甘精胰岛素" else "门冬胰岛素"),
                medDose = dose?.let { if (it % 1f == 0f) "${it.toInt()}" else "$it" } ?: "",
                medTiming = timing.ifBlank { if (p == MealPeriod.NIGHT) "睡前" else "餐前" },
                diet = diet,
                exercise = ex
            )
        }
        map
    }

    val currentDraft = drafts[selectedPeriod] ?: remember { CareMealDraft() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "✍️ 记一笔",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "日期：$initialDate",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 2. 餐段切换胶囊行（4餐大药丸，高度 52dp，高对比度气泡框）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MealPeriod.entries.forEach { p ->
                        val isSel = p == selectedPeriod
                        val pTitle = when (p) {
                            MealPeriod.MORNING -> "🌅 早饭"
                            MealPeriod.LUNCH -> "☀️ 中饭"
                            MealPeriod.DINNER -> "🌆 晚饭"
                            MealPeriod.NIGHT -> "🌙 睡前"
                        }
                        val draftForP = drafts[p]
                        val hasAnyData = draftForP?.countFilledItems() ?: 0 > 0

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSel) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.5.dp, if (isSel) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedPeriod = p
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = pTitle,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasAnyData && !isSel) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 5.dp, end = 5.dp)
                                            .size(7.dp)
                                            .background(TealPrimary, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 条目类型切换选项卡（餐前 / 餐后 / 用药 / 饮食 / 运动），实体气泡框质感与舒适内边距
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CareRecordType.entries.forEach { type ->
                        val isTypeSel = type == selectedType
                        val hasDataInTab = currentDraft.hasData(type)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isTypeSel) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, if (isTypeSel) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedType = type
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(type.icon, fontSize = 18.sp)
                                Text(
                                    text = type.shortTitle,
                                    fontSize = 16.sp,
                                    fontWeight = if (isTypeSel) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isTypeSel) TealPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasDataInTab) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(TealPrimary, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // 4. 当前所选条目的大字编辑区（每次聚焦单个条目，彻底消除拥挤感）
                when (selectedType) {
                    CareRecordType.FASTING_OR_PRE -> {
                        CarePreBGEditor(
                            period = selectedPeriod,
                            draft = currentDraft,
                            onDraftChanged = { currentDraft.preBG = it }
                        )
                    }
                    CareRecordType.POST_MEAL -> {
                        CarePostBGEditor(
                            period = selectedPeriod,
                            draft = currentDraft,
                            onDraftChanged = { currentDraft.postBG = it },
                            onTagChanged = { currentDraft.postTag = it }
                        )
                    }
                    CareRecordType.MEDICATION -> {
                        CareMedicationEditor(
                            period = selectedPeriod,
                            draft = currentDraft,
                            allRecords = allRecords
                        )
                    }
                    CareRecordType.DIET -> {
                        CareDietEditor(
                            period = selectedPeriod,
                            draft = currentDraft,
                            onDraftChanged = { currentDraft.diet = it }
                        )
                    }
                    CareRecordType.EXERCISE -> {
                        CareExerciseEditor(
                            period = selectedPeriod,
                            draft = currentDraft,
                            onDraftChanged = { currentDraft.exercise = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 5. 底部巨型保存按钮（带已录入条目计数与语音自然语言播报反馈）
                val totalFilled = currentDraft.countFilledItems()
                val submitText = when {
                    totalFilled > 1 -> "保存本餐全部记录 (${totalFilled}项)"
                    totalFilled == 1 -> "保存${selectedType.shortTitle}记录"
                    else -> "保存本餐记录"
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

                        // 基于原有记录合并已填内容
                        var updated = existingRecord ?: InsulinRecord(
                            date = initialDate,
                            fastingBG = null,
                            preBfBG = null,
                            postBfBG = null,
                            bfInsulin = null,
                            lunchInsulin = null,
                            dinnerInsulin = null,
                            bedtimeInsulin = null,
                            notes = ""
                        )

                        // 收集语音播报文案
                        val ttsSb = StringBuilder()
                        val periodTitle = when (selectedPeriod) {
                            MealPeriod.MORNING -> "早餐"
                            MealPeriod.LUNCH -> "午餐"
                            MealPeriod.DINNER -> "晚餐"
                            MealPeriod.NIGHT -> "睡前"
                        }
                        ttsSb.append("已为您保存${periodTitle}记录：")

                        // 1. 餐前
                        val preVal = currentDraft.preBG.trim().toFloatOrNull()
                        if (preVal != null && preVal in 0.5f..35.0f) {
                            updated = when (selectedPeriod) {
                                MealPeriod.MORNING -> updated.copy(fastingBG = preVal, preBfBG = preVal)
                                MealPeriod.LUNCH -> updated.copy(preLunchBG = preVal)
                                MealPeriod.DINNER -> updated.copy(preDinnerBG = preVal)
                                MealPeriod.NIGHT -> updated.copy(preNightBG = preVal)
                            }
                            updated = updated.withItemTime(selectedPeriod, "pre_bg", now)
                            ttsSb.append("餐前血糖 ${preVal}，")
                        }

                        // 2. 餐后
                        val postVal = currentDraft.postBG.trim().toFloatOrNull()
                        if (postVal != null && postVal in 0.5f..35.0f) {
                            val newEntry = PostMealEntry(postVal, now, currentDraft.postTag)
                            val curList = updated.getPostMealList(selectedPeriod).toMutableList()
                            val idx = curList.indexOfFirst { it.tag == currentDraft.postTag }
                            if (idx >= 0) {
                                curList[idx] = newEntry
                            } else {
                                curList.add(newEntry)
                            }
                            val serialized = PostMealUtils.serializeEntries(curList)
                            updated = when (selectedPeriod) {
                                MealPeriod.MORNING -> updated.copy(postBfBG = postVal, postBfBGExtra = serialized)
                                MealPeriod.LUNCH -> updated.copy(postLunchBG = postVal, postLunchBGExtra = serialized)
                                MealPeriod.DINNER -> updated.copy(postDinnerBG = postVal, postDinnerBGExtra = serialized)
                                MealPeriod.NIGHT -> updated.copy(postNightBG = postVal, postNightBGExtra = serialized)
                            }
                            ttsSb.append("${currentDraft.postTag}血糖 ${postVal}，")
                        }

                        // 3. 用药 (胰岛素 / 口服药 / 针剂)
                        val doseVal = currentDraft.medDose.trim().toFloatOrNull()
                        val actualName = currentDraft.medName.trim().ifBlank {
                            when (currentDraft.medCategory) {
                                MedCategory.INSULIN -> if (selectedPeriod == MealPeriod.NIGHT) "甘精胰岛素" else "门冬胰岛素"
                                MedCategory.ORAL -> "二甲双胍"
                                MedCategory.GLP1 -> "司美格鲁肽"
                            }
                        }
                        val timingToSave = if (selectedPeriod == MealPeriod.NIGHT) "睡前" else currentDraft.medTiming.ifBlank { "餐前" }

                        if (currentDraft.medCategory == MedCategory.INSULIN && doseVal != null && doseVal > 0f) {
                            updated = when (selectedPeriod) {
                                MealPeriod.MORNING -> updated.copy(bfInsulin = doseVal, bfMedName = actualName, bfMedTiming = timingToSave)
                                MealPeriod.LUNCH -> updated.copy(lunchInsulin = doseVal, lunchMedName = actualName, lunchMedTiming = timingToSave)
                                MealPeriod.DINNER -> updated.copy(dinnerInsulin = doseVal, dinnerMedName = actualName, dinnerMedTiming = timingToSave)
                                MealPeriod.NIGHT -> updated.copy(bedtimeInsulin = doseVal, nightMedName = actualName, nightMedTiming = "睡前")
                            }
                            updated = updated.withItemTime(selectedPeriod, "med", now)
                            ttsSb.append("${actualName} ${doseVal.toInt()}单位，")
                        } else if (actualName.isNotBlank()) {
                            updated = when (selectedPeriod) {
                                MealPeriod.MORNING -> updated.copy(bfMedName = actualName, bfInsulin = doseVal ?: 0f, bfMedTiming = timingToSave)
                                MealPeriod.LUNCH -> updated.copy(lunchMedName = actualName, lunchInsulin = doseVal ?: 0f, lunchMedTiming = timingToSave)
                                MealPeriod.DINNER -> updated.copy(dinnerMedName = actualName, dinnerInsulin = doseVal ?: 0f, dinnerMedTiming = timingToSave)
                                MealPeriod.NIGHT -> updated.copy(nightMedName = actualName, bedtimeInsulin = doseVal ?: 0f, nightMedTiming = "睡前")
                            }
                            updated = updated.withItemTime(selectedPeriod, "med", now)
                            val doseDesc = if (doseVal != null && doseVal > 0f) "${doseVal}片" else ""
                            ttsSb.append("${actualName}${doseDesc}，")
                        }

                        // 4. 饮食
                        if (currentDraft.diet.isNotBlank()) {
                            updated = when (selectedPeriod) {
                                MealPeriod.MORNING -> updated.copy(bfDiet = currentDraft.diet.trim())
                                MealPeriod.LUNCH -> updated.copy(lunchDiet = currentDraft.diet.trim())
                                MealPeriod.DINNER -> updated.copy(dinnerDiet = currentDraft.diet.trim())
                                MealPeriod.NIGHT -> updated.copy(nightDiet = currentDraft.diet.trim())
                            }
                            updated = updated.withItemTime(selectedPeriod, "diet", now)
                            ttsSb.append("饮食已记录，")
                        }

                        // 5. 运动
                        if (currentDraft.exercise.isNotBlank()) {
                            updated = when (selectedPeriod) {
                                MealPeriod.MORNING -> updated.copy(bfExercise = currentDraft.exercise.trim())
                                MealPeriod.LUNCH -> updated.copy(lunchExercise = currentDraft.exercise.trim())
                                MealPeriod.DINNER -> updated.copy(dinnerExercise = currentDraft.exercise.trim())
                                MealPeriod.NIGHT -> updated.copy(nightExercise = currentDraft.exercise.trim())
                            }
                            updated = updated.withItemTime(selectedPeriod, "exercise", now)
                            ttsSb.append("运动已记录，")
                        }

                        ttsSb.append("保存成功。")
                        onSaveRecord(updated, ttsSb.toString())
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = submitText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 1. 餐前血糖专属大字编辑器（彻底消除文字垂直截断）
 */
@Composable
private fun CarePreBGEditor(
    period: MealPeriod,
    draft: CareMealDraft,
    onDraftChanged: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalIsDarkTheme.current
    val pTitle = when (period) {
        MealPeriod.MORNING -> "🌅 晨起空腹"
        MealPeriod.LUNCH -> "☀️ 午餐前"
        MealPeriod.DINNER -> "🌆 晚餐前"
        MealPeriod.NIGHT -> "🌙 睡前"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$pTitle 血糖",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val v = draft.preBG.toFloatOrNull()
            if (v != null && v > 0f) {
                val (stText, stColor) = when {
                    v < 3.9f -> "⚠️ 偏低" to Color(0xFFEF4444)
                    v in 3.9f..7.2f -> "🟢 达标" to Color(0xFF059669)
                    v <= 10.0f -> "🟡 良好" to Color(0xFFF59E0B)
                    else -> "🔴 偏高" to Color(0xFFEF4444)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = stColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, stColor)
                ) {
                    Text(
                        text = stText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = stColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // 大字输入框与步进微调按键（Surface + BasicTextField 绝不截断）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val cur = draft.preBG.toFloatOrNull() ?: 6.0f
                    val n = (cur - 0.5f).coerceAtLeast(1.0f)
                    onDraftChanged(String.format(Locale.US, "%.1f", n))
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(0.95f)
                    .height(58.dp)
            ) {
                Text("- 0.5", fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF3F4F6),
                border = BorderStroke(1.5.dp, if (draft.preBG.isNotBlank()) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1.3f)
                    .height(58.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    if (draft.preBG.isEmpty()) {
                        Text(
                            text = "0.0",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }
                    BasicTextField(
                        value = draft.preBG,
                        onValueChange = { onDraftChanged(it.replace('。', '.').replace('，', '.').replace(" ", "")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = TealPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val cur = draft.preBG.toFloatOrNull() ?: 6.0f
                    val n = (cur + 0.5f).coerceAtMost(30.0f)
                    onDraftChanged(String.format(Locale.US, "%.1f", n))
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(0.95f)
                    .height(58.dp)
            ) {
                Text("+ 0.5", fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }

        // 快捷常见数值气泡（轻触即填，高对比度气泡框）
        Text("常用血糖快速选：", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("5.2", "5.8", "6.5", "7.0", "7.8", "8.5").forEach { num ->
                val isSel = draft.preBG == num
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.2.dp, if (isSel) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDraftChanged(num)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 11.dp)) {
                        Text(
                            text = num,
                            fontSize = 16.sp,
                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (isSel) TealPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. 餐后血糖专属大字编辑器（彻底消除文字垂直截断）
 */
@Composable
private fun CarePostBGEditor(
    period: MealPeriod,
    draft: CareMealDraft,
    onDraftChanged: (String) -> Unit,
    onTagChanged: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalIsDarkTheme.current
    val pTitle = when (period) {
        MealPeriod.MORNING -> "🌅 早餐后"
        MealPeriod.LUNCH -> "☀️ 午餐后"
        MealPeriod.DINNER -> "🌆 晚餐后"
        MealPeriod.NIGHT -> "🌙 睡后加测"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$pTitle 血糖",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val v = draft.postBG.toFloatOrNull()
            if (v != null && v > 0f) {
                val (stText, stColor) = when {
                    v < 3.9f -> "⚠️ 偏低" to Color(0xFFEF4444)
                    v in 3.9f..10.0f -> "🟢 达标" to Color(0xFF059669)
                    else -> "🔴 偏高" to Color(0xFFEF4444)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = stColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, stColor)
                ) {
                    Text(
                        text = stText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = stColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // 餐后时段标签气泡框（餐后2小时 / 餐后1小时 / 餐后半小时 / 加餐后）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("餐后2h", "餐后1h", "餐后半小时", "加餐后").forEach { tag ->
                val isSel = tag == draft.postTag
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSel) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.2.dp, if (isSel) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTagChanged(tag)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = tag,
                            fontSize = 14.sp,
                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 大字输入框与步进微调按键（Surface + BasicTextField 绝不截断）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val cur = draft.postBG.toFloatOrNull() ?: 8.0f
                    val n = (cur - 0.5f).coerceAtLeast(1.0f)
                    onDraftChanged(String.format(Locale.US, "%.1f", n))
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(0.95f)
                    .height(58.dp)
            ) {
                Text("- 0.5", fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF3F4F6),
                border = BorderStroke(1.5.dp, if (draft.postBG.isNotBlank()) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1.3f)
                    .height(58.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    if (draft.postBG.isEmpty()) {
                        Text(
                            text = "0.0",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }
                    BasicTextField(
                        value = draft.postBG,
                        onValueChange = { onDraftChanged(it.replace('。', '.').replace('，', '.').replace(" ", "")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = TealPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val cur = draft.postBG.toFloatOrNull() ?: 8.0f
                    val n = (cur + 0.5f).coerceAtMost(30.0f)
                    onDraftChanged(String.format(Locale.US, "%.1f", n))
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(0.95f)
                    .height(58.dp)
            ) {
                Text("+ 0.5", fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }

        // 常用餐后数值快速选
        Text("常用餐后血糖快速选：", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("7.2", "7.8", "8.5", "9.2", "10.0", "11.5").forEach { num ->
                val isSel = draft.postBG == num
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.2.dp, if (isSel) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDraftChanged(num)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 11.dp)) {
                        Text(
                            text = num,
                            fontSize = 16.sp,
                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (isSel) TealPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. 用药专属大字编辑器（配备完整用药选框、下拉药物菜单、用药时机、无截断剂量输入）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareMedicationEditor(
    period: MealPeriod,
    draft: CareMealDraft,
    allRecords: List<InsulinRecord>
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalIsDarkTheme.current
    val pTitle = when (period) {
        MealPeriod.MORNING -> "🌅 早餐"
        MealPeriod.LUNCH -> "☀️ 午餐"
        MealPeriod.DINNER -> "🌆 晚餐"
        MealPeriod.NIGHT -> "🌙 睡前"
    }

    var medDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 顶栏：标题 + 药物大类切换胶囊（胰岛素 / 口服药 / 针剂）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$pTitle 用药",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 药物分类切换胶囊
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    MedCategory.INSULIN to "💉 胰岛素",
                    MedCategory.ORAL to "💊 口服药",
                    MedCategory.GLP1 to "💉 针剂"
                ).forEach { (cat, label) ->
                    val isSel = draft.medCategory == cat
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.2.dp, if (isSel) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                draft.medCategory = cat
                                if (cat == MedCategory.INSULIN && (draft.medName.isBlank() || draft.medName in MedicationData.commonOralMeds || draft.medName in MedicationData.commonGLP1Meds)) {
                                    draft.medName = if (period == MealPeriod.NIGHT) "甘精胰岛素" else "门冬胰岛素"
                                } else if (cat == MedCategory.ORAL && (draft.medName.isBlank() || draft.medName in MedicationData.commonInsulinMeds || draft.medName in MedicationData.commonGLP1Meds)) {
                                    draft.medName = "二甲双胍"
                                } else if (cat == MedCategory.GLP1 && (draft.medName.isBlank() || draft.medName in MedicationData.commonInsulinMeds || draft.medName in MedicationData.commonOralMeds)) {
                                    draft.medName = "司美格鲁肽"
                                }
                            }
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 2. 用药选框 (The Medication Selection Box: 支持输入、支持下拉)
        Text(
            text = "所用药物名称：",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        val currentMedOptions = remember(draft.medCategory, allRecords) {
            MedicationData.getMedicationOptions(draft.medCategory, allRecords)
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF3F4F6),
            border = BorderStroke(1.5.dp, TealPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (draft.medCategory) {
                        MedCategory.INSULIN -> "💉"
                        MedCategory.ORAL -> "💊"
                        MedCategory.GLP1 -> "💉"
                    },
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (draft.medName.isBlank()) {
                        Text(
                            text = "轻触输入药名或右侧选药",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    BasicTextField(
                        value = draft.medName,
                        onValueChange = {
                            draft.medName = it
                            draft.medCategory = MedicationData.inferCategory(it)
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 下拉选药按钮
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TealPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { medDropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("选药", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "展开药物列表",
                                tint = TealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = medDropdownExpanded,
                        onDismissRequest = { medDropdownExpanded = false },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .widthIn(min = 220.dp, max = 280.dp)
                            .heightIn(max = 340.dp)
                    ) {
                        Text(
                            text = "请选择常用或历史药物：",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        currentMedOptions.forEach { med ->
                            val isSelected = draft.medName == med
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = med,
                                            fontSize = 16.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "已选",
                                                tint = TealPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    draft.medName = med
                                    draft.medCategory = MedicationData.inferCategory(med)
                                    medDropdownExpanded = false
                                },
                                modifier = Modifier.background(if (isSelected) TealPrimary.copy(alpha = 0.1f) else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }

        // 3. 常用药快捷气泡
        Text("常用药品轻触快选：", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currentMedOptions.take(6).forEach { name ->
                val isSel = draft.medName == name
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.2.dp, if (isSel) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            draft.medName = name
                            draft.medCategory = MedicationData.inferCategory(name)
                        }
                ) {
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSel) TealPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 4. 用药时机选框（餐前 / 餐中 / 餐后）
        if (period != MealPeriod.NIGHT) {
            Text("用药时机：", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("餐前", "餐中", "餐后").forEach { timing ->
                    val isSel = draft.medTiming == timing
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSel) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.2.dp, if (isSel) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                draft.medTiming = timing
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = timing,
                                fontSize = 15.sp,
                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 5. 剂量输入与步进微调按键（Surface + BasicTextField 绝不截断）
        val unit = MedicationData.detectUnit(draft.medName, draft.medCategory)
        val isInsulinOrInjection = draft.medCategory == MedCategory.INSULIN || draft.medCategory == MedCategory.GLP1
        val step = if (isInsulinOrInjection) 1f else 0.5f

        Text("用药剂量 ($unit)：", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val cur = draft.medDose.toFloatOrNull() ?: if (isInsulinOrInjection) 6f else 1f
                    val n = (cur - step).coerceAtLeast(if (isInsulinOrInjection) 1f else 0.5f)
                    draft.medDose = if (n % 1f == 0f) "${n.toInt()}" else "$n"
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(0.95f)
                    .height(58.dp)
            ) {
                Text(
                    if (isInsulinOrInjection) "- 1U" else "- 0.5",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF3F4F6),
                border = BorderStroke(1.5.dp, if (draft.medDose.isNotBlank()) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1.3f)
                    .height(58.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (draft.medDose.isEmpty()) {
                            Text(
                                text = "0",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                        }
                        BasicTextField(
                            value = draft.medDose,
                            onValueChange = { draft.medDose = it.replace('。', '.').replace('，', '.').replace(" ", "") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = TealPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = unit,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val cur = draft.medDose.toFloatOrNull() ?: if (isInsulinOrInjection) 6f else 1f
                    val n = (cur + step).coerceAtMost(if (isInsulinOrInjection) 80f else 10f)
                    draft.medDose = if (n % 1f == 0f) "${n.toInt()}" else "$n"
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(0.95f)
                    .height(58.dp)
            ) {
                Text(
                    if (isInsulinOrInjection) "+ 1U" else "+ 0.5",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // 6. 常用剂量快捷点选
        Text("常用剂量快速点选：", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val doseOptions = if (isInsulinOrInjection) {
            listOf("4", "6", "8", "10", "12", "14")
        } else {
            listOf("0.5", "1", "1.5", "2", "2.5", "3")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            doseOptions.forEach { dose ->
                val isSel = draft.medDose == dose
                val label = if (isInsulinOrInjection) "${dose}U" else "${dose}片"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isSel) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            draft.medDose = dose
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4. 饮食专属大字编辑器（带长辈生活化快捷食物气泡框）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareDietEditor(
    period: MealPeriod,
    draft: CareMealDraft,
    onDraftChanged: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val pTitle = when (period) {
        MealPeriod.MORNING -> "🌅 早餐"
        MealPeriod.LUNCH -> "☀️ 午餐"
        MealPeriod.DINNER -> "🌆 晚餐"
        MealPeriod.NIGHT -> "🌙 睡前加餐"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "$pTitle 吃了什么",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = draft.diet,
            onValueChange = onDraftChanged,
            placeholder = {
                Text(
                    text = "在此输入长辈本餐吃了什么（支持系统输入法打字或手写）",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        )

        Text("轻触快捷添加常见餐食：", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("包子", "馒头", "燕麦粥", "鸡蛋", "牛奶", "青菜豆腐", "杂粮饭", "面条", "鱼肉").forEach { food ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val cur = draft.diet.trim()
                            val next = if (cur.isBlank()) food else "$cur, $food"
                            onDraftChanged(next)
                        }
                ) {
                    Text(
                        text = "+ $food",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * 5. 运动专属大字编辑器（带常见适老化活动与时长点选）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareExerciseEditor(
    period: MealPeriod,
    draft: CareMealDraft,
    onDraftChanged: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val pTitle = when (period) {
        MealPeriod.MORNING -> "🌅 晨间"
        MealPeriod.LUNCH -> "☀️ 午后"
        MealPeriod.DINNER -> "🌆 晚间"
        MealPeriod.NIGHT -> "🌙 睡前"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "$pTitle 运动/活动",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = draft.exercise,
            onValueChange = onDraftChanged,
            placeholder = {
                Text(
                    text = "例如：散步 30分钟、太极拳 20分钟",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("长辈常见运动项目：", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("散步", "快走", "太极拳", "广场舞", "慢跑", "做家务").forEach { act ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDraftChanged("$act 30分钟")
                        }
                ) {
                    Text(
                        text = act,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Text("时长微调：", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("15分钟", "20分钟", "30分钟", "45分钟", "60分钟").forEach { dur ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val rawName = draft.exercise.split(" ").firstOrNull()?.ifBlank { "散步" } ?: "散步"
                            onDraftChanged("$rawName $dur")
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = dur,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
