package com.example.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

data class RecognizedItem(
    val period: MealPeriod?,
    val category: String, // e.g. "早餐", "午餐", "晚餐", "睡前", "日期", "备注"
    val label: String,    // e.g. "空腹血糖", "餐后血糖", "用药", "饮食"
    val value: String,    // e.g. "5.8 mmol/L", "门冬胰岛素 4U", "包子鸡蛋"
    val icon: String      // e.g. "🩸", "💉", "🥗"
)

data class ParsedVoiceRecord(
    val rawText: String,
    val date: String, // YYYY-MM-DD
    val targetPeriod: MealPeriod? = null,

    // 晨间 / 早餐
    val fastingBG: Float? = null,
    val preBfBG: Float? = null,
    val postBfBG: Float? = null,
    val bfPostTag: String = "餐后2小时",
    val bfMedName: String? = null,
    val bfInsulin: Float? = null,
    val bfMedTiming: String? = null,
    val bfDiet: String? = null,
    val bfExercise: String? = null,

    // 午间 / 午餐
    val preLunchBG: Float? = null,
    val postLunchBG: Float? = null,
    val lunchPostTag: String = "餐后2小时",
    val lunchMedName: String? = null,
    val lunchInsulin: Float? = null,
    val lunchMedTiming: String? = null,
    val lunchDiet: String? = null,
    val lunchExercise: String? = null,

    // 傍晚 / 晚餐
    val preDinnerBG: Float? = null,
    val postDinnerBG: Float? = null,
    val dinnerPostTag: String = "餐后2小时",
    val dinnerMedName: String? = null,
    val dinnerInsulin: Float? = null,
    val dinnerMedTiming: String? = null,
    val dinnerDiet: String? = null,
    val dinnerExercise: String? = null,

    // 睡前
    val preNightBG: Float? = null,
    val postNightBG: Float? = null,
    val nightPostTag: String = "餐后2小时",
    val nightMedName: String? = null,
    val bedtimeInsulin: Float? = null,
    val nightMedTiming: String? = null,
    val nightDiet: String? = null,
    val nightExercise: String? = null,

    val notes: String? = null
) {
    fun hasAnyData(): Boolean =
        fastingBG != null || preBfBG != null || postBfBG != null || bfInsulin != null || !bfDiet.isNullOrBlank() || !bfExercise.isNullOrBlank() ||
        preLunchBG != null || postLunchBG != null || lunchInsulin != null || !lunchDiet.isNullOrBlank() || !lunchExercise.isNullOrBlank() ||
        preDinnerBG != null || postDinnerBG != null || dinnerInsulin != null || !dinnerDiet.isNullOrBlank() || !dinnerExercise.isNullOrBlank() ||
        preNightBG != null || postNightBG != null || bedtimeInsulin != null || !nightDiet.isNullOrBlank() || !nightExercise.isNullOrBlank() ||
        !notes.isNullOrBlank()

    fun getRecognizedItems(): List<RecognizedItem> {
        val items = mutableListOf<RecognizedItem>()

        // 早餐段
        if (fastingBG != null) {
            items.add(RecognizedItem(MealPeriod.MORNING, "早餐", "空腹血糖", "${fastingBG} mmol/L", "🩸"))
        }
        if (preBfBG != null) {
            items.add(RecognizedItem(MealPeriod.MORNING, "早餐", "早餐前血糖", "${preBfBG} mmol/L", "🩸"))
        }
        if (bfInsulin != null) {
            val med = bfMedName ?: "胰岛素"
            val unit = MedicationData.detectUnit(med)
            val timing = if (!bfMedTiming.isNullOrBlank() && bfMedTiming != "餐前") "·$bfMedTiming" else ""
            items.add(RecognizedItem(MealPeriod.MORNING, "早餐", "早餐用药", "$med ${bfInsulin.formatDose()}$unit$timing", "💉"))
        }
        if (postBfBG != null) {
            val tagText = if (bfPostTag.isNotBlank()) bfPostTag else "餐后2小时"
            items.add(RecognizedItem(MealPeriod.MORNING, "早餐", "早餐$tagText", "${postBfBG} mmol/L", "🩸"))
        }
        if (!bfDiet.isNullOrBlank()) {
            items.add(RecognizedItem(MealPeriod.MORNING, "早餐", "早餐饮食", bfDiet, "🥗"))
        }
        if (!bfExercise.isNullOrBlank()) {
            items.add(RecognizedItem(MealPeriod.MORNING, "早餐", "早间运动", bfExercise, "🏃"))
        }

        // 午餐段
        if (preLunchBG != null) {
            items.add(RecognizedItem(MealPeriod.LUNCH, "午餐", "午餐前血糖", "${preLunchBG} mmol/L", "🩸"))
        }
        if (lunchInsulin != null) {
            val med = lunchMedName ?: "胰岛素"
            val unit = MedicationData.detectUnit(med)
            val timing = if (!lunchMedTiming.isNullOrBlank() && lunchMedTiming != "餐前") "·$lunchMedTiming" else ""
            items.add(RecognizedItem(MealPeriod.LUNCH, "午餐", "午餐用药", "$med ${lunchInsulin.formatDose()}$unit$timing", "💉"))
        }
        if (postLunchBG != null) {
            val tagText = if (lunchPostTag.isNotBlank()) lunchPostTag else "餐后2小时"
            items.add(RecognizedItem(MealPeriod.LUNCH, "午餐", "午餐$tagText", "${postLunchBG} mmol/L", "🩸"))
        }
        if (!lunchDiet.isNullOrBlank()) {
            items.add(RecognizedItem(MealPeriod.LUNCH, "午餐", "午餐饮食", lunchDiet, "🍱"))
        }
        if (!lunchExercise.isNullOrBlank()) {
            items.add(RecognizedItem(MealPeriod.LUNCH, "午餐", "午间运动", lunchExercise, "🏃"))
        }

        // 晚餐段
        if (preDinnerBG != null) {
            items.add(RecognizedItem(MealPeriod.DINNER, "晚餐", "晚餐前血糖", "${preDinnerBG} mmol/L", "🩸"))
        }
        if (dinnerInsulin != null) {
            val med = dinnerMedName ?: "胰岛素"
            val unit = MedicationData.detectUnit(med)
            val timing = if (!dinnerMedTiming.isNullOrBlank() && dinnerMedTiming != "餐前") "·$dinnerMedTiming" else ""
            items.add(RecognizedItem(MealPeriod.DINNER, "晚餐", "晚餐用药", "$med ${dinnerInsulin.formatDose()}$unit$timing", "💉"))
        }
        if (postDinnerBG != null) {
            val tagText = if (dinnerPostTag.isNotBlank()) dinnerPostTag else "餐后2小时"
            items.add(RecognizedItem(MealPeriod.DINNER, "晚餐", "晚餐$tagText", "${postDinnerBG} mmol/L", "🩸"))
        }
        if (!dinnerDiet.isNullOrBlank()) {
            items.add(RecognizedItem(MealPeriod.DINNER, "晚餐", "晚餐饮食", dinnerDiet, "🍲"))
        }
        if (!dinnerExercise.isNullOrBlank()) {
            items.add(RecognizedItem(MealPeriod.DINNER, "晚餐", "晚间运动", dinnerExercise, "🏃"))
        }

        // 睡前段
        if (preNightBG != null) {
            items.add(RecognizedItem(MealPeriod.NIGHT, "睡前", "睡前血糖", "${preNightBG} mmol/L", "🩸"))
        }
        if (bedtimeInsulin != null) {
            val med = nightMedName ?: "胰岛素"
            val unit = MedicationData.detectUnit(med)
            val timing = if (!nightMedTiming.isNullOrBlank() && nightMedTiming != "餐前") "·$nightMedTiming" else ""
            items.add(RecognizedItem(MealPeriod.NIGHT, "睡前", "睡前用药", "$med ${bedtimeInsulin.formatDose()}$unit$timing", "🛌"))
        }
        if (postNightBG != null) {
            val tagText = if (nightPostTag.isNotBlank()) nightPostTag else "餐后2小时"
            items.add(RecognizedItem(MealPeriod.NIGHT, "睡前", "夜间$tagText", "${postNightBG} mmol/L", "🩸"))
        }
        if (!nightDiet.isNullOrBlank()) {
            items.add(RecognizedItem(MealPeriod.NIGHT, "睡前", "睡前加餐", nightDiet, "🥛"))
        }
        if (!nightExercise.isNullOrBlank()) {
            items.add(RecognizedItem(MealPeriod.NIGHT, "睡前", "睡前运动", nightExercise, "🏃"))
        }

        // 备注
        if (!notes.isNullOrBlank()) {
            items.add(RecognizedItem(null, "备注", "记录备注", notes, "📝"))
        }

        return items
    }

    private fun Float.formatDose(): String {
        return if (this % 1f == 0f) this.toInt().toString() else String.format(Locale.US, "%.1f", this)
    }

    /**
     * 将语音提取到的非空字段增量合并到现有记录中（不覆盖用户未提及的已有内容）
     */
    fun mergeInto(existingRecord: InsulinRecord?): InsulinRecord {
        val base = existingRecord ?: InsulinRecord(date = this.date)

        val newBfExtra = if (this.postBfBG != null) {
            val tag = if (this.bfPostTag.isNotBlank()) this.bfPostTag else "餐后2小时"
            val list = base.getPostMealList(MealPeriod.MORNING).toMutableList()
            if (list.isEmpty()) {
                PostMealUtils.serializeEntries(listOf(PostMealEntry(this.postBfBG, "", tag)))
            } else {
                list[0] = list[0].copy(value = this.postBfBG, tag = tag)
                PostMealUtils.serializeEntries(list)
            }
        } else base.postBfBGExtra

        val newLunchExtra = if (this.postLunchBG != null) {
            val tag = if (this.lunchPostTag.isNotBlank()) this.lunchPostTag else "餐后2小时"
            val list = base.getPostMealList(MealPeriod.LUNCH).toMutableList()
            if (list.isEmpty()) {
                PostMealUtils.serializeEntries(listOf(PostMealEntry(this.postLunchBG, "", tag)))
            } else {
                list[0] = list[0].copy(value = this.postLunchBG, tag = tag)
                PostMealUtils.serializeEntries(list)
            }
        } else base.postLunchBGExtra

        val newDinnerExtra = if (this.postDinnerBG != null) {
            val tag = if (this.dinnerPostTag.isNotBlank()) this.dinnerPostTag else "餐后2小时"
            val list = base.getPostMealList(MealPeriod.DINNER).toMutableList()
            if (list.isEmpty()) {
                PostMealUtils.serializeEntries(listOf(PostMealEntry(this.postDinnerBG, "", tag)))
            } else {
                list[0] = list[0].copy(value = this.postDinnerBG, tag = tag)
                PostMealUtils.serializeEntries(list)
            }
        } else base.postDinnerBGExtra

        val newNightExtra = if (this.postNightBG != null) {
            val tag = if (this.nightPostTag.isNotBlank()) this.nightPostTag else "餐后2小时"
            val list = base.getPostMealList(MealPeriod.NIGHT).toMutableList()
            if (list.isEmpty()) {
                PostMealUtils.serializeEntries(listOf(PostMealEntry(this.postNightBG, "", tag)))
            } else {
                list[0] = list[0].copy(value = this.postNightBG, tag = tag)
                PostMealUtils.serializeEntries(list)
            }
        } else base.postNightBGExtra

        return base.copy(
            date = this.date,
            fastingBG = this.fastingBG ?: base.fastingBG,
            preBfBG = this.preBfBG ?: base.preBfBG,
            postBfBG = this.postBfBG ?: base.postBfBG,
            postBfBGExtra = newBfExtra,
            bfMedName = this.bfMedName ?: base.bfMedName,
            bfInsulin = this.bfInsulin ?: base.bfInsulin,
            bfMedTiming = this.bfMedTiming ?: base.bfMedTiming,
            bfDiet = if (!this.bfDiet.isNullOrBlank()) this.bfDiet else base.bfDiet,
            bfExercise = if (!this.bfExercise.isNullOrBlank()) this.bfExercise else base.bfExercise,

            preLunchBG = this.preLunchBG ?: base.preLunchBG,
            postLunchBG = this.postLunchBG ?: base.postLunchBG,
            postLunchBGExtra = newLunchExtra,
            lunchMedName = this.lunchMedName ?: base.lunchMedName,
            lunchInsulin = this.lunchInsulin ?: base.lunchInsulin,
            lunchMedTiming = this.lunchMedTiming ?: base.lunchMedTiming,
            lunchDiet = if (!this.lunchDiet.isNullOrBlank()) this.lunchDiet else base.lunchDiet,
            lunchExercise = if (!this.lunchExercise.isNullOrBlank()) this.lunchExercise else base.lunchExercise,

            preDinnerBG = this.preDinnerBG ?: base.preDinnerBG,
            postDinnerBG = this.postDinnerBG ?: base.postDinnerBG,
            postDinnerBGExtra = newDinnerExtra,
            dinnerMedName = this.dinnerMedName ?: base.dinnerMedName,
            dinnerInsulin = this.dinnerInsulin ?: base.dinnerInsulin,
            dinnerMedTiming = this.dinnerMedTiming ?: base.dinnerMedTiming,
            dinnerDiet = if (!this.dinnerDiet.isNullOrBlank()) this.dinnerDiet else base.dinnerDiet,
            dinnerExercise = if (!this.dinnerExercise.isNullOrBlank()) this.dinnerExercise else base.dinnerExercise,

            preNightBG = this.preNightBG ?: base.preNightBG,
            postNightBG = this.postNightBG ?: base.postNightBG,
            postNightBGExtra = newNightExtra,
            nightMedName = this.nightMedName ?: base.nightMedName,
            bedtimeInsulin = this.bedtimeInsulin ?: base.bedtimeInsulin,
            nightMedTiming = this.nightMedTiming ?: base.nightMedTiming,
            nightDiet = if (!this.nightDiet.isNullOrBlank()) this.nightDiet else base.nightDiet,
            nightExercise = if (!this.nightExercise.isNullOrBlank()) this.nightExercise else base.nightExercise,

            notes = if (!this.notes.isNullOrBlank()) {
                if (base.notes.isBlank()) this.notes else "${base.notes}；${this.notes}"
            } else {
                base.notes
            }
        )
    }
}

object VoiceRecordParser {

    /**
     * 将中文自然语言解析为结构化记录数据
     */
    fun parse(input: String, defaultDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)): ParsedVoiceRecord {
        val rawText = input.trim()
        if (rawText.isBlank()) {
            return ParsedVoiceRecord(rawText = "", date = defaultDate)
        }

        // 1. 语音同音字纠错与中文数字转阿拉伯数字预处理
        val normalized = VoiceRecognitionService.enhanceOfflineTranscription(rawText)

        // 2. 提取日期
        val date = parseDate(normalized, defaultDate)

        // 3. 将文本划分为不同餐段子句进行分析
        val segments = splitByMeals(normalized)

        var fastingBG: Float? = null
        var preBfBG: Float? = null
        var postBfBG: Float? = null
        var bfPostTag: String = "餐后2小时"
        var bfMedName: String? = null
        var bfInsulin: Float? = null
        var bfMedTiming: String? = null
        var bfDiet: String? = null
        var bfExercise: String? = null

        var preLunchBG: Float? = null
        var postLunchBG: Float? = null
        var lunchPostTag: String = "餐后2小时"
        var lunchMedName: String? = null
        var lunchInsulin: Float? = null
        var lunchMedTiming: String? = null
        var lunchDiet: String? = null
        var lunchExercise: String? = null

        var preDinnerBG: Float? = null
        var postDinnerBG: Float? = null
        var dinnerPostTag: String = "餐后2小时"
        var dinnerMedName: String? = null
        var dinnerInsulin: Float? = null
        var dinnerMedTiming: String? = null
        var dinnerDiet: String? = null
        var dinnerExercise: String? = null

        var preNightBG: Float? = null
        var postNightBG: Float? = null
        var nightPostTag: String = "餐后2小时"
        var nightMedName: String? = null
        var bedtimeInsulin: Float? = null
        var nightMedTiming: String? = null
        var nightDiet: String? = null
        var nightExercise: String? = null

        var notes: String? = null

        var primaryPeriod: MealPeriod? = null

        // 默认根据当前时间推断餐段
        val defaultPeriod = getCurrentPeriodByTime()

        for (seg in segments) {
            val period = seg.period
            val text = seg.text

            if (primaryPeriod == null && period != null) {
                primaryPeriod = period
            }

            // 提取血糖（严格模式，杜绝把药物剂量识别为血糖）
            val bgMap = extractBloodGlucose(text, period)
            // 提取用药
            val medResult = extractMedication(text, period)
            // 提取饮食
            val diet = extractDiet(text, period)
            // 提取运动
            val exercise = extractExercise(text, period)
            // 提取餐后标签（若未指明多少小时，默认餐后2小时）
            val postTag = extractPostMealTag(text)

            when (period) {
                MealPeriod.MORNING -> {
                    if (bgMap["fasting"] != null) fastingBG = bgMap["fasting"]
                    if (bgMap["pre"] != null) preBfBG = bgMap["pre"]
                    if (bgMap["post"] != null) {
                        postBfBG = bgMap["post"]
                        bfPostTag = postTag
                    }
                    if (medResult != null) {
                        if (medResult.dose != null) bfInsulin = medResult.dose
                        if (medResult.name != null) bfMedName = medResult.name
                        if (medResult.timing != null) bfMedTiming = medResult.timing
                    }
                    if (!diet.isNullOrBlank()) bfDiet = diet
                    if (!exercise.isNullOrBlank()) bfExercise = exercise
                }
                MealPeriod.LUNCH -> {
                    if (bgMap["pre"] != null) preLunchBG = bgMap["pre"]
                    if (bgMap["post"] != null) {
                        postLunchBG = bgMap["post"]
                        lunchPostTag = postTag
                    }
                    if (medResult != null) {
                        if (medResult.dose != null) lunchInsulin = medResult.dose
                        if (medResult.name != null) lunchMedName = medResult.name
                        if (medResult.timing != null) lunchMedTiming = medResult.timing
                    }
                    if (!diet.isNullOrBlank()) lunchDiet = diet
                    if (!exercise.isNullOrBlank()) lunchExercise = exercise
                }
                MealPeriod.DINNER -> {
                    if (bgMap["pre"] != null) preDinnerBG = bgMap["pre"]
                    if (bgMap["post"] != null) {
                        postDinnerBG = bgMap["post"]
                        dinnerPostTag = postTag
                    }
                    if (medResult != null) {
                        if (medResult.dose != null) dinnerInsulin = medResult.dose
                        if (medResult.name != null) dinnerMedName = medResult.name
                        if (medResult.timing != null) dinnerMedTiming = medResult.timing
                    }
                    if (!diet.isNullOrBlank()) dinnerDiet = diet
                    if (!exercise.isNullOrBlank()) dinnerExercise = exercise
                }
                MealPeriod.NIGHT -> {
                    if (bgMap["pre"] != null) preNightBG = bgMap["pre"]
                    if (bgMap["post"] != null) {
                        postNightBG = bgMap["post"]
                        nightPostTag = postTag
                    }
                    if (medResult != null) {
                        if (medResult.dose != null) bedtimeInsulin = medResult.dose
                        if (medResult.name != null) nightMedName = medResult.name
                        if (medResult.timing != null) nightMedTiming = medResult.timing
                    }
                    if (!diet.isNullOrBlank()) nightDiet = diet
                    if (!exercise.isNullOrBlank()) nightExercise = exercise
                }
                null -> {
                    // 未指定明确餐段的子句
                    if (bgMap["fasting"] != null) fastingBG = bgMap["fasting"]
                    if (bgMap["bf_pre"] != null) preBfBG = bgMap["bf_pre"]
                    if (bgMap["bf_post"] != null) {
                        postBfBG = bgMap["bf_post"]
                        bfPostTag = postTag
                    }
                    if (bgMap["lunch_pre"] != null) preLunchBG = bgMap["lunch_pre"]
                    if (bgMap["lunch_post"] != null) {
                        postLunchBG = bgMap["lunch_post"]
                        lunchPostTag = postTag
                    }
                    if (bgMap["dinner_pre"] != null) preDinnerBG = bgMap["dinner_pre"]
                    if (bgMap["dinner_post"] != null) {
                        postDinnerBG = bgMap["dinner_post"]
                        dinnerPostTag = postTag
                    }
                    if (bgMap["night_pre"] != null) preNightBG = bgMap["night_pre"]

                    // 通用血糖（未指明餐段）
                    if (bgMap["generic_pre"] != null || bgMap["generic_post"] != null || bgMap["generic"] != null) {
                        val gVal = bgMap["generic_pre"] ?: bgMap["generic_post"] ?: bgMap["generic"]
                        val isPost = bgMap["generic_post"] != null
                        when (defaultPeriod) {
                            MealPeriod.MORNING -> {
                                if (isPost) { postBfBG = gVal; bfPostTag = postTag } else fastingBG = gVal
                            }
                            MealPeriod.LUNCH -> {
                                if (isPost) { postLunchBG = gVal; lunchPostTag = postTag } else preLunchBG = gVal
                            }
                            MealPeriod.DINNER -> {
                                if (isPost) { postDinnerBG = gVal; dinnerPostTag = postTag } else preDinnerBG = gVal
                            }
                            MealPeriod.NIGHT -> {
                                if (isPost) { postNightBG = gVal; nightPostTag = postTag } else preNightBG = gVal
                            }
                        }
                    }

                    // 通用用药（未指明餐段，例如用户只说“打了6单位胰岛素”）
                    if (medResult != null && medResult.dose != null) {
                        when (defaultPeriod) {
                            MealPeriod.MORNING -> {
                                bfInsulin = medResult.dose
                                bfMedName = medResult.name ?: "胰岛素"
                                bfMedTiming = medResult.timing
                            }
                            MealPeriod.LUNCH -> {
                                lunchInsulin = medResult.dose
                                lunchMedName = medResult.name ?: "胰岛素"
                                lunchMedTiming = medResult.timing
                            }
                            MealPeriod.DINNER -> {
                                dinnerInsulin = medResult.dose
                                dinnerMedName = medResult.name ?: "胰岛素"
                                dinnerMedTiming = medResult.timing
                            }
                            MealPeriod.NIGHT -> {
                                bedtimeInsulin = medResult.dose
                                nightMedName = medResult.name ?: "甘精胰岛素"
                                nightMedTiming = medResult.timing
                            }
                        }
                    }

                    // 通用餐食饮食（未指明餐段，例如用户只说“吃了包子馒头稀饭”）
                    if (!diet.isNullOrBlank()) {
                        when (defaultPeriod) {
                            MealPeriod.MORNING -> if (bfDiet.isNullOrBlank()) bfDiet = diet
                            MealPeriod.LUNCH -> if (lunchDiet.isNullOrBlank()) lunchDiet = diet
                            MealPeriod.DINNER -> if (dinnerDiet.isNullOrBlank()) dinnerDiet = diet
                            MealPeriod.NIGHT -> if (nightDiet.isNullOrBlank()) nightDiet = diet
                        }
                    }

                    // 通用运动（未指明餐段）
                    if (!exercise.isNullOrBlank()) {
                        when (defaultPeriod) {
                            MealPeriod.MORNING -> if (bfExercise.isNullOrBlank()) bfExercise = exercise
                            MealPeriod.LUNCH -> if (lunchExercise.isNullOrBlank()) lunchExercise = exercise
                            MealPeriod.DINNER -> if (dinnerExercise.isNullOrBlank()) dinnerExercise = exercise
                            MealPeriod.NIGHT -> if (nightExercise.isNullOrBlank()) nightExercise = exercise
                        }
                    }

                    // 备注提取
                    val noteMatch = extractNotes(text)
                    if (!noteMatch.isNullOrBlank()) {
                        notes = if (notes.isNullOrBlank()) noteMatch else "$notes；$noteMatch"
                    }
                }
            }
        }

        return ParsedVoiceRecord(
            rawText = rawText,
            date = date,
            targetPeriod = primaryPeriod ?: defaultPeriod,
            fastingBG = fastingBG,
            preBfBG = preBfBG,
            postBfBG = postBfBG,
            bfPostTag = bfPostTag,
            bfMedName = bfMedName,
            bfInsulin = bfInsulin,
            bfMedTiming = bfMedTiming,
            bfDiet = bfDiet,
            bfExercise = bfExercise,

            preLunchBG = preLunchBG,
            postLunchBG = postLunchBG,
            lunchPostTag = lunchPostTag,
            lunchMedName = lunchMedName,
            lunchInsulin = lunchInsulin,
            lunchMedTiming = lunchMedTiming,
            lunchDiet = lunchDiet,
            lunchExercise = lunchExercise,

            preDinnerBG = preDinnerBG,
            postDinnerBG = postDinnerBG,
            dinnerPostTag = dinnerPostTag,
            dinnerMedName = dinnerMedName,
            dinnerInsulin = dinnerInsulin,
            dinnerMedTiming = dinnerMedTiming,
            dinnerDiet = dinnerDiet,
            dinnerExercise = dinnerExercise,

            preNightBG = preNightBG,
            postNightBG = postNightBG,
            nightPostTag = nightPostTag,
            nightMedName = nightMedName,
            bedtimeInsulin = bedtimeInsulin,
            nightMedTiming = nightMedTiming,
            nightDiet = nightDiet,
            nightExercise = nightExercise,

            notes = notes
        )
    }

    private fun getCurrentPeriodByTime(): MealPeriod {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 4..10 -> MealPeriod.MORNING
            in 11..14 -> MealPeriod.LUNCH
            in 15..20 -> MealPeriod.DINNER
            else -> MealPeriod.NIGHT
        }
    }

    private data class MealSegment(
        val period: MealPeriod?,
        val text: String
    )

    private fun splitByMeals(text: String): List<MealSegment> {
        val markers = listOf(
            MealPeriod.MORNING to listOf("早餐", "早饭", "早上", "晨起", "空腹", "早晨", "晨间"),
            MealPeriod.LUNCH to listOf("午餐", "午饭", "中午", "中饭"),
            MealPeriod.DINNER to listOf("晚餐", "晚饭", "晚上", "晚间", "傍晚"),
            MealPeriod.NIGHT to listOf("睡前", "夜间", "临睡")
        )

        data class MarkerPos(val period: MealPeriod, val index: Int, val length: Int)
        val foundMarkers = mutableListOf<MarkerPos>()

        for ((period, keywords) in markers) {
            for (kw in keywords) {
                var idx = text.indexOf(kw)
                while (idx >= 0) {
                    foundMarkers.add(MarkerPos(period, idx, kw.length))
                    idx = text.indexOf(kw, idx + kw.length)
                }
            }
        }

        foundMarkers.sortBy { it.index }

        if (foundMarkers.isEmpty()) {
            return listOf(MealSegment(null, text))
        }

        val segments = mutableListOf<MealSegment>()
        if (foundMarkers.first().index > 0) {
            val prefix = text.substring(0, foundMarkers.first().index).trim()
            if (prefix.isNotBlank()) {
                segments.add(MealSegment(null, prefix))
            }
        }

        for (i in foundMarkers.indices) {
            val curr = foundMarkers[i]
            val nextIdx = if (i + 1 < foundMarkers.size) foundMarkers[i + 1].index else text.length
            val segmentText = text.substring(curr.index, nextIdx).trim()
            if (segmentText.isNotBlank()) {
                segments.add(MealSegment(curr.period, segmentText))
            }
        }

        return segments
    }

    private data class MedicationResult(
        val name: String?,
        val dose: Float?,
        val timing: String?
    )

    private fun extractMedication(text: String, period: MealPeriod?): MedicationResult? {
        var foundName: String? = null
        var foundDose: Float? = null
        var foundTiming: String? = null

        // 1. 优先精准匹配药物商品名/口语别名（如“格华止”、“诺和锐”、“来得时”、“替尔泊肽”等）
        val sortedAliases = MedicationData.medicationAliasMap.keys.sortedByDescending { it.length }
        for (alias in sortedAliases) {
            if (text.contains(alias)) {
                foundName = MedicationData.medicationAliasMap[alias]
                break
            }
        }

        // 2. 匹配标准药物库名称（按长度降序避免子串混淆）
        if (foundName == null) {
            val specificMeds = (MedicationData.commonInsulinMeds + MedicationData.commonOralMeds)
                .filter { it != "胰岛素" && it != "口服药" }
                .sortedByDescending { it.length }

            for (med in specificMeds) {
                val short = med.replace("胰岛素", "").replace("片", "").replace("胶囊", "")
                if (text.contains(med)) {
                    foundName = med
                    break
                } else if (short.length >= 2 && text.contains(short)) {
                    foundName = if (MedicationData.commonInsulinMeds.contains(med)) "${short}胰岛素" else med
                    break
                }
            }
        }

        // 3. 通用关键字及特征词兜底推断
        if (foundName == null) {
            if (text.contains("门冬")) {
                foundName = "门冬胰岛素"
            } else if (text.contains("甘精")) {
                foundName = "甘精胰岛素"
            } else if (text.contains("赖脯")) {
                foundName = "赖脯胰岛素"
            } else if (text.contains("德谷")) {
                foundName = "德谷胰岛素"
            } else if (text.contains("地特")) {
                foundName = "地特胰岛素"
            } else if (text.contains("谷赖")) {
                foundName = "谷赖胰岛素"
            } else if (text.contains("双胍")) {
                foundName = "二甲双胍"
            } else if (text.contains("波糖")) {
                foundName = "阿卡波糖"
            } else if (text.contains("胰岛素") || text.contains("打针") || text.contains("注射") || text.contains("单位") || text.contains("U") || text.contains("u") || text.contains("打")) {
                foundName = if (period == MealPeriod.NIGHT) "甘精胰岛素" else "胰岛素"
            } else if (text.contains("吃药") || text.contains("口服药") || text.contains("片") || text.contains("粒")) {
                foundName = "口服药"
            }
        }

        // 提取剂量
        val dosePatterns = listOf(
            Pattern.compile("""(?:打|注射|用|吃了|服用|摄入)?(?:了)?\s*(\d+(?:\.\d+)?)\s*(?:个)?\s*(?:单位|U|u|支)"""),
            Pattern.compile("""(?:吃了|服用|口服)\s*(\d+(?:\.\d+)?)\s*(?:片|粒|袋)"""),
            Pattern.compile("""(?:打了|注射了|用了|剂量|用量)\s*(\d+(?:\.\d+)?)(?!\s*(?:mmol|个点|点\d))"""),
            Pattern.compile("""(\d+(?:\.\d+)?)\s*(?:单位|U|u)""")
        )

        for (pat in dosePatterns) {
            val m = pat.matcher(text)
            if (m.find()) {
                val num = m.group(1)?.toFloatOrNull()
                if (num != null && num > 0f) {
                    foundDose = num
                    break
                }
            }
        }

        foundTiming = when {
            text.contains("随餐") || text.contains("餐中") || text.contains("吃饭时") -> "餐中"
            text.contains("餐后") || text.contains("饭后") -> "餐后"
            text.contains("睡前") || text.contains("临睡") -> "睡前"
            text.contains("空腹") -> "空腹"
            text.contains("餐前") || text.contains("饭前") -> "餐前"
            else -> null
        }

        return if (foundName != null || foundDose != null) {
            MedicationResult(foundName, foundDose, foundTiming)
        } else {
            null
        }
    }

    /**
     * 提取血糖值：严格过滤药物剂量，杜绝将“打6单位”中的6误识别为血糖
     */
    private fun extractBloodGlucose(text: String, period: MealPeriod?): Map<String, Float> {
        val result = mutableMapOf<String, Float>()

        // 辅助函数：校验匹配到的数字是否紧跟用药量词（单位/U/片/粒等），如果是则坚决不是血糖
        fun isValidBGNumber(posEnd: Int): Boolean {
            if (posEnd >= text.length) return true
            val following = text.substring(posEnd).trimStart()
            val drugUnits = listOf("单位", "个单位", "U", "u", "片", "粒", "支", "袋", "毫克", "mg", "ml")
            for (u in drugUnits) {
                if (following.startsWith(u, ignoreCase = true)) {
                    return false
                }
            }
            return true
        }

        // 1. 空腹 / 晨起血糖
        val fastingPat = Pattern.compile("""(?:空腹|晨起)(?:血糖)?(?:是|为|到|测得)?\s*(\d+(?:\.\d+)?)""")
        val mFasting = fastingPat.matcher(text)
        if (mFasting.find()) {
            if (isValidBGNumber(mFasting.end(1))) {
                mFasting.group(1)?.toFloatOrNull()?.let { if (it in 1.5f..33.3f) result["fasting"] = it }
            }
        }

        // 2. 餐前 / 饭前血糖
        val prePat = Pattern.compile("""(?:餐前|饭前)(?:血糖)?(?:是|为|到|测得)?\s*(\d+(?:\.\d+)?)""")
        val mPre = prePat.matcher(text)
        if (mPre.find()) {
            if (isValidBGNumber(mPre.end(1))) {
                mPre.group(1)?.toFloatOrNull()?.let { if (it in 1.5f..33.3f) result["pre"] = it }
            }
        }

        // 3. 餐后 / 饭后血糖
        val postPat = Pattern.compile("""(?:餐后(?:[123一二两]小时|[123]h)?|饭后(?:[123一二两]小时|[123]h)?|加餐后)(?:血糖)?(?:是|为|到|测得)?\s*(\d+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE)
        val mPost = postPat.matcher(text)
        if (mPost.find()) {
            if (isValidBGNumber(mPost.end(1))) {
                mPost.group(1)?.toFloatOrNull()?.let { if (it in 1.5f..33.3f) result["post"] = it }
            }
        }

        // 4. 睡前 / 夜间血糖
        val nightPat = Pattern.compile("""(?:睡前|夜间)(?:血糖)?(?:是|为|到|测得)?\s*(\d+(?:\.\d+)?)""")
        val mNight = nightPat.matcher(text)
        if (mNight.find()) {
            if (isValidBGNumber(mNight.end(1))) {
                mNight.group(1)?.toFloatOrNull()?.let { if (it in 1.5f..33.3f) result["pre"] = it }
            }
        }

        // 5. 跨餐段的明确修饰
        val specificPatterns = mapOf(
            "bf_pre" to Pattern.compile("""(?:早[餐饭]前|早餐餐前)(?:血糖)?(?:是|为|到)?\s*(\d+(?:\.\d+)?)"""),
            "bf_post" to Pattern.compile("""(?:早[餐饭]后|早餐餐后)(?:(?:[123一二两]小时|[123]h))?(?:血糖)?(?:是|为|到)?\s*(\d+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE),
            "lunch_pre" to Pattern.compile("""(?:午[餐饭]前|中午餐前|中饭前)(?:血糖)?(?:是|为|到)?\s*(\d+(?:\.\d+)?)"""),
            "lunch_post" to Pattern.compile("""(?:午[餐饭]后|中午餐后|中饭后)(?:(?:[123一二两]小时|[123]h))?(?:血糖)?(?:是|为|到)?\s*(\d+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE),
            "dinner_pre" to Pattern.compile("""(?:晚[餐饭]前|晚餐餐前|晚上餐前)(?:血糖)?(?:是|为|到)?\s*(\d+(?:\.\d+)?)"""),
            "dinner_post" to Pattern.compile("""(?:晚[餐饭]后|晚餐餐后|晚上餐后)(?:(?:[123一二两]小时|[123]h))?(?:血糖)?(?:是|为|到)?\s*(\d+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE),
            "night_pre" to Pattern.compile("""(?:睡前|夜间)(?:血糖)?(?:是|为|到)?\s*(\d+(?:\.\d+)?)"""),
            "night_post" to Pattern.compile("""(?:睡前|夜间)(?:(?:[123一二两]小时|[123]h))?(?:血糖)?(?:是|为|到)?\s*(\d+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE)
        )

        for ((key, pat) in specificPatterns) {
            val m = pat.matcher(text)
            if (m.find()) {
                if (isValidBGNumber(m.end(1))) {
                    m.group(1)?.toFloatOrNull()?.let { if (it in 1.5f..33.3f) result[key] = it }
                }
            }
        }

        // 6. 包含明确“血糖”二字，或者带 mmol/L 单位的数值
        if (result.isEmpty()) {
            val bgGeneralPat = Pattern.compile("""(?:血糖(?:是|为|测了|到|测得)?\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:mmol|mmol/L))""")
            val mGeneral = bgGeneralPat.matcher(text)
            if (mGeneral.find()) {
                val numStr = mGeneral.group(1) ?: mGeneral.group(2)
                val posEnd = if (mGeneral.group(1) != null) mGeneral.end(1) else mGeneral.end(2)
                if (isValidBGNumber(posEnd)) {
                    val v = numStr?.toFloatOrNull()
                    if (v != null && v in 1.5f..33.3f) {
                        when {
                            text.contains("后") || text.contains("餐后") || text.contains("饭后") -> {
                                if (period != null) result["post"] = v else result["generic_post"] = v
                            }
                            text.contains("空腹") || period == MealPeriod.MORNING -> {
                                if (period != null) result["fasting"] = v else result["generic_pre"] = v
                            }
                            else -> {
                                if (period != null) result["pre"] = v else result["generic"] = v
                            }
                        }
                    }
                }
            }
        }

        return result
    }

    private fun extractPostMealTag(subText: String): String {
        return when {
            subText.contains("1小时") || subText.contains("一小时") || subText.contains("1h", ignoreCase = true) -> "餐后1小时"
            subText.contains("3小时") || subText.contains("三小时") || subText.contains("3h", ignoreCase = true) -> "餐后3小时"
            subText.contains("加餐") -> "加餐后"
            else -> "餐后2小时" // 语音识别若未特别指明餐后多少小时，一律默认为餐后两小时
        }
    }

    private fun extractExercise(text: String, period: MealPeriod?): String? {
        if (text.isBlank()) return null
        val exercisePatterns = listOf(
            Pattern.compile("""(?:去|进行了|做了|完成了)?\s*(散步|慢跑|快走|跑步|游泳|骑车|骑行|打太极|太极拳|瑜伽|健身|八段锦|跳操|跳绳|运动|活动|锻炼)\s*(\d+(?:小时|分钟|个半小时)?|半小时|一小时)?"""),
            Pattern.compile("""(?:运动|锻炼|活动|走|跑)(?:了)?\s*(\d+(?:小时|分钟|个半小时)?|半小时|一小时)""")
        )
        for (pat in exercisePatterns) {
            val m = pat.matcher(text)
            if (m.find()) {
                val p1 = m.group(1) ?: ""
                val p2 = if (m.groupCount() >= 2) (m.group(2) ?: "") else ""
                val res = (p1 + p2).trim()
                if (res.length >= 2 && !res.contains("血糖") && !res.contains("胰岛素")) {
                    return res
                }
            }
        }
        return null
    }

    private fun extractDiet(text: String, period: MealPeriod?): String? {
        if (text.isBlank()) return null

        // 饮食引导词模式
        val leadPatterns = listOf(
            // 明确的餐别与饮食动词复合引导，如："早饭吃的是"、"早餐吃了点"、"早上吃了"、"午餐喝了"、"晚餐吃的"
            Pattern.compile("""(?:早[餐饭]|晨[起间]|早晨|早上|午[餐饭]|中午|中饭|晚[餐饭]|晚上|晚间|睡前|夜间|夜宵|加餐)?\s*(?:饮食(?:是|为|记录)?|餐食(?:是|为|记录)?|食谱(?:是|为)?|吃的是|吃了点|吃了|吃的|吃|喝的是|喝了点|喝了|喝|早[餐饭]是|午[餐饭]是|晚[餐饭]是|睡前是|加餐是|夜宵是)\s*"""),
            // 纯餐别冒号或紧跟食物引导，如："早餐：包子馒头"、"早饭 包子稀饭"
            Pattern.compile("""(?:早[餐饭]|晨[起间]|早晨|午[餐饭]|晚[餐饭]|夜宵|加餐)\s*[:：]\s*""")
        )

        // 强截断关键词：一旦出现，饮食内容必须立即截止，杜绝后续闲聊或测量语句污染餐食记录
        val hardStopKeywords = listOf(
            "血糖", "空腹", "晨起", "餐前", "餐后", "饭前", "饭后", "测了", "测得", "打针", "注射", "打", "用了",
            "用药", "口服", "单位", "胰岛素", "二甲双胍", "门冬", "甘精", "赖脯", "阿卡波糖",
            "血压", "心率", "体温", "体重", "量了", "高了", "低了", "不舒服", "头晕", "心慌", "出汗",
            "然后", "接着", "之后", "后来", "现在", "等会儿", "一会儿", "打算", "准备", "去上班", "去买菜", "去散步",
            "运动", "散步", "跑步", "天气", "心情", "挺好", "不错", "感觉"
        )

        // 常见食物/饮品名词或量词特征库（用于校验后续子句是否为真正的餐食补充）
        val foodKeywords = listOf(
            "包子", "馒头", "稀饭", "粥", "蛋", "奶", "豆浆", "油条", "面包", "燕麦", "面", "饭", "菜", "肉",
            "鱼", "虾", "鸡", "鸭", "牛肉", "猪肉", "汤", "水果", "苹果", "黄瓜", "西红柿", "番茄", "青菜",
            "豆腐", "粗粮", "杂粮", "馄饨", "饺", "饼", "玉米", "红薯", "紫薯", "燕麦片", "沙拉", "酸奶", "牛奶"
        )
        val foodUnits = listOf("碗", "杯", "个", "片", "份", "根", "块", "盒", "袋", "克", "两", "斤", "只", "勺", "盘")

        for (leadPat in leadPatterns) {
            val matcher = leadPat.matcher(text)
            if (matcher.find()) {
                val startIndex = matcher.end()
                if (startIndex < text.length) {
                    val candidateRaw = text.substring(startIndex)

                    // 按逗号、句号、换行等分句解析，避免将后续闲聊或测量行为贪婪囊括
                    val clauses = candidateRaw.split(Regex("""[，,。！!？?；;\n\r\t]+"""))
                    val validFoodClauses = mutableListOf<String>()

                    for (i in clauses.indices) {
                        var clause = clauses[i].trim()
                        if (clause.isBlank()) continue

                        // 检查子句内部是否有强截断关键词，截取关键词前面的部分
                        var cutIndex = -1
                        for (kw in hardStopKeywords) {
                            val pos = clause.indexOf(kw)
                            if (pos >= 0 && (cutIndex == -1 || pos < cutIndex)) {
                                cutIndex = pos
                            }
                        }
                        if (cutIndex >= 0) {
                            clause = clause.substring(0, cutIndex).trim()
                        }

                        val cleanedClause = cleanDietString(clause)

                        if (i == 0) {
                            // 第一分句：必须包含有效内容且不能是纯医学词汇
                            if (isValidDiet(cleanedClause)) {
                                validFoodClauses.add(cleanedClause)
                            }
                            // 如果第一句就触发了强截断词，后续不再继续
                            if (cutIndex >= 0) break
                        } else {
                            // 后续分句：只有当它是显式食物（含有食物名词、量词，或以吃/喝开头），且不含闲聊/动作词时，才允许作为连词拼接
                            val isFoodContinuation = (clause.startsWith("吃了") || clause.startsWith("喝了") ||
                                    clause.startsWith("还有") || clause.startsWith("加了") ||
                                    foodKeywords.any { clause.contains(it) } ||
                                    foodUnits.any { clause.contains(it) })

                            if (isFoodContinuation && isValidDiet(cleanedClause)) {
                                validFoodClauses.add(cleanedClause)
                            } else {
                                // 一旦后续句子脱离了食物范畴（如“然后量了血压”、“感觉很饱”），彻底终止解析
                                break
                            }

                            if (cutIndex >= 0) break
                        }
                    }

                    if (validFoodClauses.isNotEmpty()) {
                        val finalDiet = validFoodClauses.joinToString("、")
                        if (isValidDiet(finalDiet)) {
                            return finalDiet
                        }
                    }
                }
            }
        }

        return null
    }

    private fun cleanDietString(raw: String): String {
        var s = raw.trim()
        // 去除开头无意义助词或动词残留，如 "是"、"有"、"了"、"点"、"吃了"、"喝了"
        s = s.replace(Regex("""^(?:是|有|了|点|吃了点|喝了点|吃了|喝了|吃的是|喝的是)+"""), "").trim()
        // 去除首尾标点
        s = s.trim(',', '，', '。', '；', ';', '、', ' ', ':', '：')
        // 将内部口语连接词规整为中文顿号
        s = s.replace(Regex("""[，,]\s*(?:吃了|喝了|吃了点|喝了点|还有)"""), "、")
        s = s.replace("，", "、").replace(",", "、")
        // 去除连续多个顿号
        s = s.replace(Regex("""、+"""), "、").trim('、')
        return s
    }

    private fun isValidDiet(s: String): Boolean {
        if (s.isBlank() || s.length < 2) return false
        // 杜绝将用药或血糖识别为饮食
        val invalidKeywords = listOf("血糖", "单位", "胰岛素", "二甲双胍", "门冬", "甘精", "mmol", "毫摩")
        for (kw in invalidKeywords) {
            if (s.contains(kw)) return false
        }
        // 如果全是纯数字或量词，也不是有效饮食
        if (s.matches(Regex("""^[\d\s.片粒支袋Uu单位]+$"""))) return false
        return true
    }

    private fun extractNotes(text: String): String? {
        val keywords = listOf("不舒服", "头晕", "心慌", "出汗", "低血糖", "运动", "散步", "跑步", "发烧", "感冒", "聚餐", "喝酒", "熬夜", "失眠")
        for (kw in keywords) {
            if (text.contains(kw)) {
                val parts = text.split(Regex("""[，,。！!？?；;]"""))
                for (p in parts) {
                    if (p.contains(kw)) {
                        return p.trim()
                    }
                }
                return kw
            }
        }
        return null
    }

    private fun parseDate(text: String, defaultDate: String): String {
        val today = LocalDate.now()
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE

        if (text.contains("今天") || text.contains("今日")) {
            return today.format(fmt)
        }
        if (text.contains("昨天") || text.contains("昨日")) {
            return today.minusDays(1).format(fmt)
        }
        if (text.contains("前天")) {
            return today.minusDays(2).format(fmt)
        }
        if (text.contains("大前天")) {
            return today.minusDays(3).format(fmt)
        }
        if (text.contains("明天")) {
            return today.plusDays(1).format(fmt)
        }

        val datePat = Pattern.compile("""(?:(\d{4})年)?(\d{1,2})月(\d{1,2})[日号]""")
        val m = datePat.matcher(text)
        if (m.find()) {
            val year = m.group(1)?.toIntOrNull() ?: today.year
            val month = m.group(2)?.toIntOrNull() ?: today.monthValue
            val day = m.group(3)?.toIntOrNull() ?: today.dayOfMonth
            return try {
                LocalDate.of(year, month, day).format(fmt)
            } catch (e: Exception) {
                defaultDate
            }
        }

        return defaultDate
    }

    /**
     * 将口语中文数字转换为阿拉伯数字，方便后续正则识别
     * 例如："五点八" -> "5.8", "六点二" -> "6.2", "打了六单位" -> "打了6单位", "吃了两片" -> "吃了2片"
     */
    fun normalizeChineseNumbers(text: String): String {
        var s = text

        // 统一标点
        s = s.replace("，", ",").replace("。", ".").replace("；", ";")

        // 替换常见的 "两" 为 "2"
        s = s.replace(Regex("""两(?=[个点单位片粒支袋碗份])"""), "2")

        // 1. 处理常见阿拉伯数字+点/店+阿拉伯数字或吧/把：如 "9点8" -> "9.8", "9点吧" -> "9.8", "6点2" -> "6.2", "5点吧" -> "5.8"
        s = s.replace(Regex("""(\d+)[点店电典](\d+)"""), "$1.$2")
        s = s.replace(Regex("""(\d+)[点店电典][吧把]"""), "$1.8")
        s = s.replace(Regex("""(\d+)个点(\d+)"""), "$1.$2")
        s = s.replace(Regex("""(\d+)个点[吧把]"""), "$1.8")

        // 2. 转换口语浮点数：如 "五点八" -> "5.8", "六点二" -> "6.2", "十二点五" -> "12.5", "酒店吧" -> "9.8", "五点吧" -> "5.8"
        val pointPattern = Pattern.compile("""([零一二三四五六七八九十百两酒武留寺]+)[点店电典]([零一二三四五六七八九吧把幺两俩0-9]+)""")
        var matcher = pointPattern.matcher(s)
        val sb = StringBuffer()
        while (matcher.find()) {
            val intStr = matcher.group(1) ?: ""
            val intPart = if (intStr == "酒" || intStr == "酒店") 9 else chineseToInt(intStr)
            val decChars = matcher.group(2) ?: ""
            val decSb = StringBuilder()
            for (c in decChars) {
                decSb.append(chineseDigitChar(c))
            }
            matcher.appendReplacement(sb, "$intPart.$decSb")
        }
        matcher.appendTail(sb)
        s = sb.toString()

        // 3. 处理带"个点"的中文数字：如 "九个点八" -> "9.8", "五个点二" -> "5.2", "九个点吧" -> "9.8"
        val pointGePattern = Pattern.compile("""([零一二三四五六七八九十百两酒武留寺]+)个点([零一二三四五六七八九吧把幺两俩0-9]*)""")
        matcher = pointGePattern.matcher(s)
        val sbGe = StringBuffer()
        while (matcher.find()) {
            val intStr = matcher.group(1) ?: ""
            val intPart = if (intStr == "酒" || intStr == "酒店") 9 else chineseToInt(intStr)
            val decChars = matcher.group(2) ?: ""
            if (decChars.isEmpty()) {
                matcher.appendReplacement(sbGe, "$intPart.0")
            } else {
                val decSb = StringBuilder()
                for (c in decChars) {
                    decSb.append(chineseDigitChar(c))
                }
                matcher.appendReplacement(sbGe, "$intPart.$decSb")
            }
        }
        matcher.appendTail(sbGe)
        s = sbGe.toString()

        // 4. 转换带有量词的整数：如 "六单位" -> "6单位", "十二单位" -> "12单位", "吃了两片" -> "吃了2片", "六个单位" -> "6个单位"
        val unitPattern = Pattern.compile("""([零一二三四五六七八九十百]+)(?=\s*(?:单位|个单位|个|U|u|片|粒|支|袋|mmol|毫克|mg|分|碗|份))""")
        matcher = unitPattern.matcher(s)
        val sb2 = StringBuffer()
        while (matcher.find()) {
            val numStr = matcher.group(1) ?: ""
            val intVal = chineseToInt(numStr)
            matcher.appendReplacement(sb2, "$intVal")
        }
        matcher.appendTail(sb2)
        s = sb2.toString()

        // 5. 转换动词前缀后接的纯数字："打了六" -> "打了6", "注射四" -> "注射4", "血糖五" -> "血糖5", "餐后七" -> "餐后7"
        val prefixPattern = Pattern.compile("""(?<=(?:血糖|餐前|餐后|空腹|睡前|注射|打了|用了|吃了|是|为))([零一二三四五六七八九十]+)""")
        matcher = prefixPattern.matcher(s)
        val sb3 = StringBuffer()
        while (matcher.find()) {
            val numStr = matcher.group(1) ?: ""
            val intVal = chineseToInt(numStr)
            matcher.appendReplacement(sb3, "$intVal")
        }
        matcher.appendTail(sb3)
        s = sb3.toString()

        return s
    }

    private fun chineseDigitChar(c: Char): Char {
        return when (c) {
            '零', '0' -> '0'
            '一', '幺', '1' -> '1'
            '二', '两', '俩', '2' -> '2'
            '三', '3' -> '3'
            '四', '寺', '4' -> '4'
            '五', '武', '5' -> '5'
            '六', '留', '6' -> '6'
            '七', '期', '妻', '7' -> '7'
            '八', '吧', '把', '8' -> '8'
            '九', '酒', '9' -> '9'
            else -> c
        }
    }

    private fun chineseToInt(s: String): Int {
        if (s.isEmpty()) return 0
        if (s == "酒" || s == "酒店") return 9
        if (s == "留") return 6
        if (s == "武") return 5
        if (s == "寺") return 4
        if (s == "两" || s == "俩") return 2
        if (s.length == 1) {
            return when (s[0]) {
                '零', '0' -> 0
                '一', '幺', '1' -> 1
                '二', '两', '俩', '2' -> 2
                '三', '3' -> 3
                '四', '寺', '4' -> 4
                '五', '武', '5' -> 5
                '六', '留', '6' -> 6
                '七', '期', '妻', '7' -> 7
                '八', '吧', '把', '8' -> 8
                '九', '酒', '9' -> 9
                '十' -> 10
                else -> s.toIntOrNull() ?: 0
            }
        }

        var result = 0
        var temp = 0
        for (c in s) {
            when (c) {
                '零' -> {}
                '一', '幺' -> temp = 1
                '二', '两', '俩' -> temp = 2
                '三' -> temp = 3
                '四', '寺' -> temp = 4
                '五', '武' -> temp = 5
                '六', '留' -> temp = 6
                '七', '期', '妻' -> temp = 7
                '八', '吧', '把' -> temp = 8
                '九', '酒' -> temp = 9
                '十' -> {
                    if (temp == 0) temp = 1
                    result += temp * 10
                    temp = 0
                }
                '百' -> {
                    result += temp * 100
                    temp = 0
                }
                else -> {
                    val d = c.digitToIntOrNull()
                    if (d != null) {
                        temp = temp * 10 + d
                    }
                }
            }
        }
        result += temp
        return result
    }
}
