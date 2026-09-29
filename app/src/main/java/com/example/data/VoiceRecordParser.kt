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
    val bfPostTag: String = "餐后2h",
    val bfMedName: String? = null,
    val bfInsulin: Float? = null,
    val bfMedTiming: String? = null,
    val bfDiet: String? = null,
    val bfExercise: String? = null,

    // 午间 / 午餐
    val preLunchBG: Float? = null,
    val postLunchBG: Float? = null,
    val lunchPostTag: String = "餐后2h",
    val lunchMedName: String? = null,
    val lunchInsulin: Float? = null,
    val lunchMedTiming: String? = null,
    val lunchDiet: String? = null,
    val lunchExercise: String? = null,

    // 傍晚 / 晚餐
    val preDinnerBG: Float? = null,
    val postDinnerBG: Float? = null,
    val dinnerPostTag: String = "餐后2h",
    val dinnerMedName: String? = null,
    val dinnerInsulin: Float? = null,
    val dinnerMedTiming: String? = null,
    val dinnerDiet: String? = null,
    val dinnerExercise: String? = null,

    // 睡前
    val preNightBG: Float? = null,
    val postNightBG: Float? = null,
    val nightPostTag: String = "餐后2h",
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

        fun mergePostMeal(period: MealPeriod, bgVal: Float?, rawTag: String): Pair<Float?, String> {
            if (bgVal == null) {
                return (when (period) {
                    MealPeriod.MORNING -> base.postBfBG
                    MealPeriod.LUNCH -> base.postLunchBG
                    MealPeriod.DINNER -> base.postDinnerBG
                    MealPeriod.NIGHT -> base.postNightBG
                }) to (when (period) {
                    MealPeriod.MORNING -> base.postBfBGExtra
                    MealPeriod.LUNCH -> base.postLunchBGExtra
                    MealPeriod.DINNER -> base.postDinnerBGExtra
                    MealPeriod.NIGHT -> base.postNightBGExtra
                })
            }
            val normTag = PostMealUtils.normalizeTag(rawTag.ifBlank { "餐后2h" })
            val currentList = base.getPostMealList(period).toMutableList()
            val existingIdx = currentList.indexOfFirst { PostMealUtils.isTagMatch(it.tag, normTag) }
            val nowTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            if (existingIdx >= 0) {
                currentList[existingIdx] = currentList[existingIdx].copy(value = bgVal, tag = normTag)
            } else {
                currentList.add(PostMealEntry(bgVal, nowTimeStr, normTag))
            }
            // 按阶段时间/小时自然排序（半小时 -> 1h -> 2h -> 3h...）
            currentList.sortWith { a, b ->
                fun parseHour(t: String): Float {
                    if (t.contains("半小时") || t.contains("0.5")) return 0.5f
                    val m = Regex("""^餐后(\d+(?:\.\d+)?)(?:小时|h)$""").find(t.trim())
                    if (m != null) return m.groupValues[1].toFloatOrNull() ?: 2.0f
                    return 99f
                }
                val hA = parseHour(a.tag)
                val hB = parseHour(b.tag)
                if (hA != hB) hA.compareTo(hB) else a.time.compareTo(b.time)
            }
            val newPrimary = currentList.firstOrNull()?.value
            val serialized = PostMealUtils.serializeEntries(currentList)
            return newPrimary to serialized
        }

        val (bfPostVal, newBfExtra) = mergePostMeal(MealPeriod.MORNING, this.postBfBG, this.bfPostTag)
        val (lunchPostVal, newLunchExtra) = mergePostMeal(MealPeriod.LUNCH, this.postLunchBG, this.lunchPostTag)
        val (dinnerPostVal, newDinnerExtra) = mergePostMeal(MealPeriod.DINNER, this.postDinnerBG, this.dinnerPostTag)
        val (nightPostVal, newNightExtra) = mergePostMeal(MealPeriod.NIGHT, this.postNightBG, this.nightPostTag)

        return base.copy(
            date = this.date,
            fastingBG = this.fastingBG ?: base.fastingBG,
            preBfBG = this.preBfBG ?: base.preBfBG,
            postBfBG = bfPostVal,
            postBfBGExtra = newBfExtra,
            bfMedName = this.bfMedName ?: base.bfMedName,
            bfInsulin = this.bfInsulin ?: base.bfInsulin,
            bfMedTiming = this.bfMedTiming ?: base.bfMedTiming,
            bfDiet = if (!this.bfDiet.isNullOrBlank()) this.bfDiet else base.bfDiet,
            bfExercise = if (!this.bfExercise.isNullOrBlank()) this.bfExercise else base.bfExercise,

            preLunchBG = this.preLunchBG ?: base.preLunchBG,
            postLunchBG = lunchPostVal,
            postLunchBGExtra = newLunchExtra,
            lunchMedName = this.lunchMedName ?: base.lunchMedName,
            lunchInsulin = this.lunchInsulin ?: base.lunchInsulin,
            lunchMedTiming = this.lunchMedTiming ?: base.lunchMedTiming,
            lunchDiet = if (!this.lunchDiet.isNullOrBlank()) this.lunchDiet else base.lunchDiet,
            lunchExercise = if (!this.lunchExercise.isNullOrBlank()) this.lunchExercise else base.lunchExercise,

            preDinnerBG = this.preDinnerBG ?: base.preDinnerBG,
            postDinnerBG = dinnerPostVal,
            postDinnerBGExtra = newDinnerExtra,
            dinnerMedName = this.dinnerMedName ?: base.dinnerMedName,
            dinnerInsulin = this.dinnerInsulin ?: base.dinnerInsulin,
            dinnerMedTiming = this.dinnerMedTiming ?: base.dinnerMedTiming,
            dinnerDiet = if (!this.dinnerDiet.isNullOrBlank()) this.dinnerDiet else base.dinnerDiet,
            dinnerExercise = if (!this.dinnerExercise.isNullOrBlank()) this.dinnerExercise else base.dinnerExercise,

            preNightBG = this.preNightBG ?: base.preNightBG,
            postNightBG = nightPostVal,
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

/**
 * 临床级自然语言语义解析状态机 (VoiceRecordParser)：
 *
 * 架构职责与管道分层：
 * 1. 纯函数无状态管道设计：
 *    - 整个解析引擎不持有任何共享可变全局状态，线程安全、无副作用，支持在任意后台 Coroutine 中并发调用；
 * 2. 四阶段语义提取管道：
 *    - Stage 1: 文本预处理与声学同音纠错：[VoiceRecognitionService.enhanceOfflineTranscription]（包括中文数字转阿拉伯数字、常见方言及同音词校正）；
 *    - Stage 2: 日期提取与跨餐段切分：[splitByMeals] 将用户连续报录的长句（如“早上打了4个门冬，中午餐后8.5”）智能分段为独立子句；
 *    - Stage 3: 多维度临床要素提取：
 *      • 血糖 (BG)：空腹/餐前/餐后，支持餐后半小时/1h/2h/3h标签自动识别与单位校验；
 *      • 用药 (Med)：支持胰岛素(U)、口服药(片)、GLP-1(mg/针)现代三大类分类与剂量正规化；
 *      • 饮食 (Diet)：支持顿号/逗号并列多食物提取，过滤虚假食物与时段词干扰；
 *      • 运动 (Exercise)：动结式重叠词识别、口语时长解析（如“半小时”->30分钟）；
 *    - Stage 4: 结果聚合与无损增量合并：组装为 [ParsedVoiceRecord]，提供 [mergeInto] 安全合并至数据库实体。
 *
 * 变更防牵连机制：
 * - 暴露稳定的输入 [parse] 与输出契约 [ParsedVoiceRecord]，内部正则和抽取细节的迭代不破坏调用方的接口契约。
 */
object VoiceRecordParser {

    /**
     * 将中文自然语言解析为结构化记录数据
     */
    fun parse(
        input: String,
        defaultDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
        userProfile: UserMedProfile? = null
    ): ParsedVoiceRecord {
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
        var bfPostTag: String = "餐后2h"
        var bfMedName: String? = null
        var bfInsulin: Float? = null
        var bfMedTiming: String? = null
        var bfDiet: String? = null
        var bfExercise: String? = null

        var preLunchBG: Float? = null
        var postLunchBG: Float? = null
        var lunchPostTag: String = "餐后2h"
        var lunchMedName: String? = null
        var lunchInsulin: Float? = null
        var lunchMedTiming: String? = null
        var lunchDiet: String? = null
        var lunchExercise: String? = null

        var preDinnerBG: Float? = null
        var postDinnerBG: Float? = null
        var dinnerPostTag: String = "餐后2h"
        var dinnerMedName: String? = null
        var dinnerInsulin: Float? = null
        var dinnerMedTiming: String? = null
        var dinnerDiet: String? = null
        var dinnerExercise: String? = null

        var preNightBG: Float? = null
        var postNightBG: Float? = null
        var nightPostTag: String = "餐后2h"
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
            // 提取用药（支持用户常用药自学习画像加权）
            val medResult = extractMedication(text, period, userProfile)
            // 提取饮食
            val diet = extractDiet(text, period)
            // 提取运动
            val exercise = extractExercise(text, period)
            // 提取餐后标签（若未指明多少小时，默认餐后2小时）
            val postTag = extractPostMealTag(text)

            when (period) {
                MealPeriod.MORNING -> {
                    if (bgMap["fasting"] != null) fastingBG = bgMap["fasting"]
                    val mPre = bgMap["bf_pre"] ?: bgMap["pre"]
                    val mPost = bgMap["bf_post"] ?: bgMap["post"]
                    if (mPre != null) {
                        if (preBfBG == null) {
                            preBfBG = mPre
                        } else if (postBfBG == null && !text.contains("前")) {
                            postBfBG = mPre
                            bfPostTag = postTag
                        }
                    }
                    if (mPost != null) {
                        postBfBG = mPost
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
                    val lPre = bgMap["lunch_pre"] ?: bgMap["pre"]
                    val lPost = bgMap["lunch_post"] ?: bgMap["post"]
                    if (lPre != null) {
                        if (preLunchBG == null) {
                            preLunchBG = lPre
                        } else if (postLunchBG == null && !text.contains("前")) {
                            postLunchBG = lPre
                            lunchPostTag = postTag
                        }
                    }
                    if (lPost != null) {
                        postLunchBG = lPost
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
                    val dPre = bgMap["dinner_pre"] ?: bgMap["pre"]
                    val dPost = bgMap["dinner_post"] ?: bgMap["post"]
                    if (dPre != null) {
                        if (preDinnerBG == null) {
                            preDinnerBG = dPre
                        } else if (postDinnerBG == null && !text.contains("前")) {
                            postDinnerBG = dPre
                            dinnerPostTag = postTag
                        }
                    }
                    if (dPost != null) {
                        postDinnerBG = dPost
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
                    val nPre = bgMap["night_pre"] ?: bgMap["pre"]
                    val nPost = bgMap["night_post"] ?: bgMap["post"]
                    if (nPre != null) preNightBG = nPre
                    if (nPost != null) {
                        postNightBG = nPost
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

                    // 通用血糖（未指明餐段，例如用户说“餐后半小时血糖6.8”、“餐前血糖5.8”或“血糖6.8”）
                    val postVal = bgMap["post"] ?: bgMap["generic_post"]
                    val preVal = bgMap["pre"] ?: bgMap["generic_pre"]
                    val genericVal = bgMap["generic"]

                    if (postVal != null || preVal != null || genericVal != null) {
                        val isPost = postVal != null
                        val gVal = postVal ?: preVal ?: genericVal
                        val targetPeriod = if (isPost && defaultPeriod == MealPeriod.NIGHT) {
                            MealPeriod.DINNER
                        } else {
                            defaultPeriod
                        }
                        when (targetPeriod) {
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
                                bfMedName = medResult.name ?: userProfile?.topInsulin ?: "胰岛素"
                                bfMedTiming = medResult.timing
                            }
                            MealPeriod.LUNCH -> {
                                lunchInsulin = medResult.dose
                                lunchMedName = medResult.name ?: userProfile?.topInsulin ?: "胰岛素"
                                lunchMedTiming = medResult.timing
                            }
                            MealPeriod.DINNER -> {
                                dinnerInsulin = medResult.dose
                                dinnerMedName = medResult.name ?: userProfile?.topInsulin ?: "胰岛素"
                                dinnerMedTiming = medResult.timing
                            }
                            MealPeriod.NIGHT -> {
                                bedtimeInsulin = medResult.dose
                                nightMedName = medResult.name ?: userProfile?.topBedtimeInsulin ?: "甘精胰岛素"
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
        val currentPeriod = getCurrentPeriodByTime()
        val markers = listOf(
            MealPeriod.NIGHT to listOf("晚上睡前", "今晚睡前", "昨晚睡前", "临睡前", "睡前", "夜间", "临睡", "夜宵", "睡觉前"),
            MealPeriod.MORNING to listOf("早餐", "早饭", "早上", "晨起", "空腹", "早晨", "晨间", "上午"),
            MealPeriod.LUNCH to listOf("午餐", "午饭", "中午", "中饭"),
            MealPeriod.DINNER to listOf("晚餐", "晚饭", "晚上", "晚间", "傍晚", "下午"),
            currentPeriod to listOf("刚刚", "刚才", "方才", "适才", "这会儿")
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

        foundMarkers.sortWith(compareBy<MarkerPos> { it.index }.thenByDescending { it.length })

        if (foundMarkers.isEmpty()) {
            val fuzzyPeriod = if (text.contains("刚刚") || text.contains("刚才") || text.contains("方才") ||
                text.contains("这会儿") || text.contains("适才") || text.startsWith("刚")
            ) currentPeriod else null
            return listOf(MealSegment(fuzzyPeriod, text))
        }

        // 过滤重叠标记，保留最长/最前置的完整标记（例如“晚上睡前”优先于“晚上”或“睡前”）
        val nonOverlapping = mutableListOf<MarkerPos>()
        for (m in foundMarkers) {
            val last = nonOverlapping.lastOrNull()
            if (last == null || m.index >= last.index + last.length) {
                nonOverlapping.add(m)
            }
        }

        val segments = mutableListOf<MealSegment>()
        if (nonOverlapping.first().index > 0) {
            val prefix = text.substring(0, nonOverlapping.first().index).trim()
            if (prefix.isNotBlank()) {
                segments.add(MealSegment(null, prefix))
            }
        }

        for (i in nonOverlapping.indices) {
            val curr = nonOverlapping[i]
            val nextIdx = if (i + 1 < nonOverlapping.size) nonOverlapping[i + 1].index else text.length
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

    private fun extractMedication(text: String, period: MealPeriod?, userProfile: UserMedProfile? = null): MedicationResult? {
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
            val specificMeds = (MedicationData.commonInsulinMeds + MedicationData.commonOralMeds + MedicationData.commonGLP1Meds)
                .filter { it != "胰岛素" && it != "口服药" && it != "GLP-1/针剂" }
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

        // 3. 通用关键字及特征词兜底推断（优先结合用户自学习常用药画像）
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
            } else if (text.contains("司美") || text.contains("诺和泰") || text.contains("诺和盈")) {
                foundName = "司美格鲁肽"
            } else if (text.contains("替尔泊肽") || text.contains("穆峰达")) {
                foundName = "替尔泊肽"
            } else if (text.contains("度拉糖肽") || text.contains("度易达")) {
                foundName = "度拉糖肽"
            } else if (text.contains("利拉鲁肽") || text.contains("诺和力")) {
                foundName = "利拉鲁肽"
            } else if (text.contains("胰岛素") || text.contains("打针") || text.contains("注射") || text.contains("单位") || text.contains("U") || text.contains("u") || text.contains("打")) {
                foundName = if (period == MealPeriod.NIGHT) {
                    userProfile?.topBedtimeInsulin ?: "甘精胰岛素"
                } else {
                    userProfile?.topInsulin ?: "胰岛素"
                }
            } else if (text.contains("吃药") || text.contains("口服药") || text.contains("降糖药") || text.contains("片") || text.contains("粒") || text.contains("颗")) {
                foundName = userProfile?.topOralMed ?: "口服药"
            } else if ((text.contains("针剂") || text.contains("支") || text.contains("针")) && (text.contains("毫克") || text.contains("mg", ignoreCase = true))) {
                foundName = userProfile?.topGLP1 ?: "司美格鲁肽"
            }
        }

        // 提取剂量
        val dosePatterns = listOf(
            Pattern.compile("""(?:打|注射|用|吃了|服用|摄入)?(?:了)?\s*(\d+(?:\.\d+)?)\s*(?:个)?\s*(?:单位|U|u|支|针)"""),
            Pattern.compile("""(?:打|注射|用|吃了|服用|口服)?(?:了)?\s*(\d+(?:\.\d+)?)\s*(?:毫克|mg|MG|Mg)"""),
            Pattern.compile("""(?:吃了|服用|口服|用)?\s*(\d+(?:\.\d+)?)\s*(?:片|粒|袋|颗)"""),
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
     * 提取血糖值：严格过滤药物剂量及时间量词，杜绝将“打6单位”中的6或“餐后2小时”中的2误识别为血糖
     */
    private fun extractBloodGlucose(text: String, period: MealPeriod?): Map<String, Float> {
        val result = mutableMapOf<String, Float>()

        // 辅助函数：校验匹配到的数字是否紧跟用药量词（单位/U/片/粒等），或者紧跟时间/单位词（小时/h/分钟/岁/点等），如果是则坚决不是血糖
        fun isValidBGNumber(posEnd: Int): Boolean {
            if (posEnd >= text.length) return true
            val following = text.substring(posEnd).trimStart()
            val drugUnits = listOf("单位", "个单位", "U", "u", "片", "粒", "支", "袋", "毫克", "mg", "ml", "颗")
            for (u in drugUnits) {
                if (following.startsWith(u, ignoreCase = true)) {
                    return false
                }
            }
            // 重点过滤时间、阶段量词（如 2小时、半小时、1h、40分钟等，这类数字是时长/阶段，绝对不是血糖值）
            val nonBgUnits = listOf(
                "小时", "个半小时", "个钟头", "个钟", "h", "H", "分钟", "分种", "分", "min",
                "点", "岁", "年", "天", "日", "号", "周", "月", "秒", "倍", "次", "顿"
            )
            for (nu in nonBgUnits) {
                if (following.startsWith(nu)) {
                    return false
                }
            }
            return true
        }

        val stagePattern = """(?:半小时|半个(?:小时|钟头|钟)|(?:一个半|1个半)个?小时|[1234一二三四两]个?半小时|[1234一二三四两]个?小时|[0.51234]h)"""

        // 1. 空腹 / 晨起血糖
        val fastingPat = Pattern.compile("""(?:空腹|晨起)(?:的|测的|测得|测出来的)?(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)""")
        val mFasting = fastingPat.matcher(text)
        while (mFasting.find()) {
            if (isValidBGNumber(mFasting.end(1))) {
                val num = mFasting.group(1)?.toFloatOrNull()
                if (num != null && num in 1.5f..33.3f) {
                    result["fasting"] = num
                    break
                }
            }
        }

        // 2. 餐前 / 饭前血糖
        val prePat = Pattern.compile("""(?:餐前|饭前)(?:的|测的|测得|测出来的)?(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)""")
        val mPre = prePat.matcher(text)
        while (mPre.find()) {
            if (isValidBGNumber(mPre.end(1))) {
                val num = mPre.group(1)?.toFloatOrNull()
                if (num != null && num in 1.5f..33.3f) {
                    result["pre"] = num
                    break
                }
            }
        }

        // 3. 跨餐段的明确修饰（早/中/晚/睡前）
        val specificPatterns = mapOf(
            "bf_pre" to Pattern.compile("""(?:早[餐饭]前|早餐餐前|早上餐前)(?:的|测的|测得|测出来的)?(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)"""),
            "bf_post" to Pattern.compile("""(?:早[餐饭]后|早餐餐后|早上餐后|早[餐饭]吃完|吃完早[餐饭])(?:$stagePattern)?(?:的|的时候|测的|测得|测出来的)?\s*(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE),
            "lunch_pre" to Pattern.compile("""(?:午[餐饭]前|中午餐前|中饭前|午饭前)(?:的|测的|测得|测出来的)?(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)"""),
            "lunch_post" to Pattern.compile("""(?:午[餐饭]后|中午餐后|中饭后|午饭后|午[餐饭]吃完|吃完午[餐饭]|中午吃完饭)(?:$stagePattern)?(?:的|的时候|测的|测得|测出来的)?\s*(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE),
            "dinner_pre" to Pattern.compile("""(?:晚[餐饭]前|晚餐餐前|晚上餐前|晚饭前)(?:的|测的|测得|测出来的)?(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)"""),
            "dinner_post" to Pattern.compile("""(?:晚[餐饭]后|晚餐餐后|晚上餐后|晚饭后|晚[餐饭]吃完|吃完晚[餐饭]|晚上吃完饭)(?:$stagePattern)?(?:的|的时候|测的|测得|测出来的)?\s*(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE),
            "night_pre" to Pattern.compile("""(?:睡前|夜间|睡觉前)(?:的|测的|测得|测出来的)?(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)"""),
            "night_post" to Pattern.compile("""(?:(?:睡前|夜间|睡觉前)\s*(?:加餐后|夜宵后|餐后|饭后|$stagePattern)|(?:加餐后|夜宵后))\s*(?:的|的时候|测的|测得|测出来的)?\s*(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE)
        )

        for ((key, pat) in specificPatterns) {
            val m = pat.matcher(text)
            while (m.find()) {
                if (isValidBGNumber(m.end(1))) {
                    val num = m.group(1)?.toFloatOrNull()
                    if (num != null && num in 1.5f..33.3f) {
                        result[key] = num
                        break
                    }
                }
            }
        }

        // 4. 餐后 / 饭后血糖（强力支持“2小时的血糖是10.0”、“餐后2小时血糖10.0”、“半小时血糖7.2”、“餐后血糖6.8”等）
        if (result["bf_post"] == null && result["lunch_post"] == null && result["dinner_post"] == null && result["night_post"] == null) {
            val postPatterns = listOf(
                // 句式 A: 带时长的阶段表达，如 "2小时的血糖是10.0", "两小时血糖10.0", "餐后2小时血糖是10.0", "半小时血糖7.2"
                Pattern.compile(
                    """(?:(?:餐后|饭后|吃完饭|吃过饭|吃完|吃过)?\s*($stagePattern)\s*(?:后)?(?:的|的时候|测的|测得|测出来的)?\s*|(?<![a-zA-Z0-9])(?:餐后|饭后|吃完饭|吃过饭|吃完|吃过)\s*)(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)""",
                    Pattern.CASE_INSENSITIVE
                ),
                // 句式 B: "半小时前吃的饭我现在血糖7.2", "2小时前吃完饭血糖10.0"
                Pattern.compile(
                    """(?:(?:$stagePattern)前(?:吃的饭|吃完饭|吃过饭|吃完了|吃的)?(?:我现在)?|刚吃完(?:$stagePattern)?)(?:的)?\s*(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)""",
                    Pattern.CASE_INSENSITIVE
                )
            )

            for (pPat in postPatterns) {
                val mPost = pPat.matcher(text)
                while (mPost.find()) {
                    val numStr = if (mPost.groupCount() >= 2 && mPost.group(2) != null) mPost.group(2) else mPost.group(1)
                    val posEnd = if (mPost.groupCount() >= 2 && mPost.group(2) != null) mPost.end(2) else mPost.end(1)
                    if (isValidBGNumber(posEnd)) {
                        val num = numStr?.toFloatOrNull()
                        if (num != null && num in 1.5f..33.3f) {
                            result["post"] = num
                            break
                        }
                    }
                }
                if (result.containsKey("post")) break
            }
        }

        // 5. 睡前 / 夜间血糖
        if (!result.containsKey("pre") && !result.containsKey("night_pre")) {
            val nightPat = Pattern.compile("""(?:睡前|夜间|睡觉前)(?:的|测的|测得|测出来的)?(?:血糖)?(?:是|为|到|测得|测了|有)?\s*(\d+(?:\.\d+)?)""")
            val mNight = nightPat.matcher(text)
            while (mNight.find()) {
                if (isValidBGNumber(mNight.end(1))) {
                    val num = mNight.group(1)?.toFloatOrNull()
                    if (num != null && num in 1.5f..33.3f) {
                        result["pre"] = num
                        break
                    }
                }
            }
        }

        // 6. 通用兜底：包含明确“血糖”二字，或者带 mmol/L 单位的数值
        if (result.isEmpty()) {
            val bgGeneralPat = Pattern.compile("""(?:血糖(?:是|为|测了|到|测得|有)?\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:mmol|mmol/L))""")
            val mGeneral = bgGeneralPat.matcher(text)
            while (mGeneral.find()) {
                val numStr = mGeneral.group(1) ?: mGeneral.group(2)
                val posEnd = if (mGeneral.group(1) != null) mGeneral.end(1) else mGeneral.end(2)
                if (isValidBGNumber(posEnd)) {
                    val v = numStr?.toFloatOrNull()
                    if (v != null && v in 1.5f..33.3f) {
                        val isPostCondition = text.contains("后") || text.contains("餐后") || text.contains("饭后") || text.contains("吃完") || text.contains("吃过") ||
                            text.contains("小时") || text.contains("半小时") || text.contains("h", ignoreCase = true)
                        when {
                            isPostCondition -> {
                                if (period != null) result["post"] = v else result["generic_post"] = v
                            }
                            text.contains("空腹") || period == MealPeriod.MORNING -> {
                                if (period != null) result["fasting"] = v else result["generic_pre"] = v
                            }
                            else -> {
                                if (period != null) result["pre"] = v else result["generic"] = v
                            }
                        }
                        break
                    }
                }
            }
        }

        return result
    }

    private fun extractPostMealTag(subText: String): String {
        // 优先在血糖相关的子句中寻找阶段标记（避免运动时长的“半小时”干扰血糖阶段）
        val bgContext = if (subText.contains("血糖")) {
            val idx = subText.indexOf("血糖")
            val start = maxOf(0, idx - 12)
            val end = minOf(subText.length, idx + 12)
            subText.substring(start, end)
        } else {
            subText
        }

        fun matchTag(s: String): String? = when {
            s.contains("2小时") || s.contains("两小时") || s.contains("二小时") || s.contains("2个小时") || s.contains("两个小时") || s.contains("2h", ignoreCase = true) || s.contains("120分") -> "餐后2h"
            s.contains("1小时") || s.contains("一小时") || s.contains("1个小时") || s.contains("一个小时") || s.contains("1h", ignoreCase = true) || s.contains("60分") -> "餐后1h"
            s.contains("3小时") || s.contains("三小时") || s.contains("3个小时") || s.contains("三个小时") || s.contains("3h", ignoreCase = true) -> "餐后3h"
            s.contains("4小时") || s.contains("四小时") || s.contains("4个小时") || s.contains("四个小时") || s.contains("4h", ignoreCase = true) -> "餐后4h"
            s.contains("半小时") || s.contains("半个") || s.contains("0.5") || s.contains("30分") || s.contains("三十分") -> "餐后半小时"
            else -> null
        }

        return matchTag(bgContext) ?: matchTag(subText) ?: "餐后2h"
    }

    private fun extractExercise(text: String, period: MealPeriod?): String? {
        if (text.isBlank()) return null

        val durPatternStr = """(?:\d+(?:\.\d+)?\s*(?:个?半小时|个?小时|分钟|分种|分|min|h)|(?:一个半|1个半|大半|半)个?(?:多)?(?:小时|钟头|钟)?|(?:一|两|二|三|四|1|2|3|4)个?(?:半小时|小时|钟头|钟)|(?:半|一|两|1|2|3|4)个?多小时|\d+\s*多分钟|\d+\s*来分钟)"""

        fun normalizeExerciseDuration(durRaw: String): String {
            val s = durRaw.trim()
            return when {
                s.contains("一个半") || s.contains("1个半") || s.contains("1.5小时") || s.contains("1.5h") -> "90分钟"
                s.contains("半") -> "30分钟"
                s.contains("两") || s.contains("二") || s.contains("2小时") || s.contains("2个") -> "120分钟"
                s.contains("一小时") || s.contains("1小时") || s.contains("一个") || s.contains("1h") -> "60分钟"
                s.contains("三") || s.contains("3小时") || s.contains("3个") -> "180分钟"
                else -> {
                    val m = Regex("""(\d+(?:\.\d+)?)""").find(s)
                    val num = m?.groupValues?.get(1) ?: "30"
                    val unit = if (s.contains("小时") || s.contains("h", ignoreCase = true)) "小时" else "分钟"
                    "$num$unit"
                }
            }
        }

        val sports = "八段锦|太极拳|打太极|太极|散步|慢跑|快走|跑步|游泳|骑车|骑行|自行车|骑自行车|爬楼梯|爬楼|瑜伽|健身|跳操|跳绳|拉伸|步|路|车|操"

        // 倒装句式 1: 动词 + 时长 + 的? + 运动项目名，如 "打了半小时八段锦"、"打了半个小时八段锦"、"练了20分钟瑜伽"、"跑了半小时步"、"走了40分钟路"
        val invertedPat = Pattern.compile(
            """(?:去|进行了|做了|完成了|打了|练了|跑了|走了|跳了|游了|骑了|爬了)?\s*(?:约|大约|大概|差不多|将近|快|有)?\s*($durPatternStr)\s*(?:的|大约|大概|左右)?\s*($sports)"""
        )
        val mInv = invertedPat.matcher(text)
        if (mInv.find()) {
            val durRaw = mInv.group(1) ?: ""
            val nameRaw = mInv.group(2) ?: ""
            val sportName = when (nameRaw) {
                "步" -> "跑步"
                "路" -> "散步"
                "车", "自行车", "骑自行车" -> "骑车"
                "爬楼" -> "爬楼梯"
                "操" -> "做操"
                else -> nameRaw
            }
            val durNorm = normalizeExerciseDuration(durRaw)
            if (sportName.isNotBlank() && durNorm.isNotBlank()) {
                return "$sportName $durNorm"
            }
        }

        // 标准句式 2: 运动项目名 + 动结重叠词? + 时长，如 "散步散了40分钟"、"散步了半个小时"、"八段锦半小时"、"慢跑30分钟"、"骑车骑了1小时"
        val standardPat = Pattern.compile(
            """(?:去|进行了|做了|完成了|打了|练了|爬了)?\s*($sports|运动|活动|锻炼)\s*(?:[骑跑散游跳打练爬走]?了|[骑跑散游跳打练爬走])?\s*(?:约|大约|大概|差不多|将近|快|有)?\s*($durPatternStr)?"""
        )
        val mStd = standardPat.matcher(text)
        if (mStd.find()) {
            val p1 = mStd.group(1) ?: ""
            val p2 = if (mStd.groupCount() >= 2) (mStd.group(2) ?: "") else ""
            val sportName = when (p1) {
                "自行车", "骑自行车" -> "骑车"
                "爬楼" -> "爬楼梯"
                else -> p1
            }
            val durNorm = if (p2.isNotBlank()) normalizeExerciseDuration(p2) else ""
            val res = (if (durNorm.isNotBlank()) "$sportName $durNorm" else sportName).trim()
            if (res.length >= 2 && !res.contains("血糖") && !res.contains("胰岛素")) {
                return res
            }
        }

        // 句式 3: 纯动词 + 时长，如 "运动了半小时"、"走了40分钟"、"跑了半小时"、"走了半个小时"、"爬了半小时"
        val verbPat = Pattern.compile(
            """(?:运动|锻炼|活动|走路|走|散步|跑步|跑|慢跑|快走|游泳|游|骑车|骑|爬楼梯|爬楼|爬)(?:了)?\s*(?:约|大约|大概|差不多|将近|快|有)?\s*($durPatternStr)"""
        )
        val mV = verbPat.matcher(text)
        if (mV.find()) {
            val durRaw = mV.group(1) ?: ""
            val durNorm = normalizeExerciseDuration(durRaw)
            return "散步 $durNorm"
        }

        return null
    }

    private fun extractDiet(text: String, period: MealPeriod?): String? {
        if (text.isBlank()) return null

        // 饮食引导词模式（剔除单字“吃/喝”，避免将“吃完饭”中的“完饭”当成具体食物）
        val leadPatterns = listOf(
            // 明确的餐别与饮食动词复合引导，如："早饭吃的是"、"早餐吃了点"、"早上吃了"、"午餐喝了"、"晚餐吃的"
            Pattern.compile("""(?:早[餐饭]|晨[起间]|早晨|早上|午[餐饭]|中午|中饭|晚[餐饭]|晚上|晚间|睡前|夜间|夜宵|加餐)?\s*(?:饮食(?:是|为|记录)?|餐食(?:是|为|记录)?|食谱(?:是|为)?|吃的是|吃了点|吃了|吃的|喝的是|喝了点|喝了|喝的|早[餐饭]是|午[餐饭]是|晚[餐饭]是|睡前是|加餐是|夜宵是)\s*"""),
            // 纯餐别冒号或紧跟食物引导，如："早餐：包子馒头"、"早饭 包子稀饭"
            Pattern.compile("""(?:早[餐饭]|晨[起间]|早晨|午[餐饭]|晚[餐饭]|夜宵|加餐)\s*[:：]\s*""")
        )

        // 强截断关键词：一旦出现，饮食内容必须立即截止，杜绝后续闲聊或测量语句污染餐食记录
        val hardStopKeywords = listOf(
            "血糖", "空腹", "晨起", "餐前", "餐后", "饭前", "饭后", "测了", "测得", "打针", "注射", "打", "用了",
            "用药", "口服", "单位", "胰岛素", "二甲双胍", "双胍", "门冬", "甘精", "赖脯", "阿卡波糖", "波糖",
            "达格列净", "恩格列净", "卡格列净", "司美", "替尔", "度拉", "利拉", "降糖药", "片药", "吃药", "服药",
            "血压", "心率", "体温", "体重", "量了", "高了", "低了", "不舒服", "头晕", "心慌", "出汗",
            "然后", "接着", "之后", "后来", "现在", "等会儿", "一会儿", "打算", "准备", "去上班", "去买菜", "去散步",
            "运动", "散步", "跑步", "天气", "心情", "挺好", "不错", "感觉", "吃完", "吃过", "用完", "用过"
        )

        // 常见食物/饮品名词或量词特征库（用于校验后续子句是否为真正的餐食补充）
        val foodKeywords = listOf(
            "包子", "馒头", "稀饭", "粥", "蛋", "奶", "豆浆", "油条", "面包", "燕麦", "面", "饭", "菜", "肉",
            "鱼", "虾", "鸡", "鸭", "牛肉", "猪肉", "汤", "水果", "苹果", "黄瓜", "西红柿", "番茄", "青菜",
            "豆腐", "粗粮", "杂粮", "馄饨", "饺", "饼", "玉米", "红薯", "紫薯", "燕麦片", "沙拉", "酸奶", "牛奶",
            "西兰花", "鸡胸肉", "菠菜", "芹菜", "生菜", "柚子", "蓝莓", "猕猴桃", "糙米饭", "荞麦面"
        )
        val foodUnits = listOf("碗", "杯", "个", "片", "份", "根", "块", "盒", "袋", "克", "两", "斤", "只", "勺", "盘")

        for (leadPat in leadPatterns) {
            val matcher = leadPat.matcher(text)
            if (matcher.find()) {
                val startIndex = matcher.end()
                if (startIndex < text.length) {
                    val candidateRaw = text.substring(startIndex)

                    // 将内部无标点的“喝了/吃了/还有/加了”转为逗号切分
                    val candidateNorm = candidateRaw.replace(Regex("""(?<=[^，,。；;])\s*(?:喝了|吃了|还有|加了)"""), "，")
                    // 按逗号、句号、换行等分句解析，避免将后续闲聊或测量行为贪婪囊括
                    val clauses = candidateNorm.split(Regex("""[，,。！!？?；;\n\r\t]+"""))
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
                        if (MedicationData.isMedicationText(cleanedClause) || MedicationData.isMedicationText(clause)) {
                            break
                        }

                        if (i == 0) {
                            // 第一分句：必须包含有效内容且不能是纯医学词汇或非食物动词
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
        // 去除开头无意义助词或动词残留，如 "是"、"有"、"了"、"点"、"还吃了"、"还喝了"、"吃了点"、"喝了点"、"吃了"、"喝了"、"吃的是"、"喝的是"、"吃完饭"、"吃了完饭"
        s = s.replace(Regex("""^(?:是|有|了|点|还吃了|还喝了|还吃|还喝|还|还有|另外吃了|另外|另外喝了|吃了点|喝了点|吃了|喝了|吃的是|喝的是|吃完饭|吃了完饭|吃完了饭|吃完|吃过)+"""), "").trim()
        // 去除首尾标点
        s = s.trim(',', '，', '。', '；', ';', '、', ' ', ':', '：')
        // 将内部口语连接词规整为中文顿号
        s = s.replace(Regex("""[，,]\s*(?:吃了|喝了|吃了点|喝了点|还有)"""), "、")
        s = s.replace("，", "、").replace(",", "、").replace("和", "、")
        // 两组相邻食物量词之间补充顿号（如 "2个包子1杯豆浆" -> "2个包子、1杯豆浆"）
        s = s.replace(Regex("""(?<=[^\d，,。；;、\s])(?=\d+[个碗份包根块盘只片杯盒袋瓶两斤条度])"""), "、")
        // 去除连续多个顿号
        s = s.replace(Regex("""、+"""), "、").trim('、')
        return s
    }

    private fun isValidDiet(s: String): Boolean {
        if (s.isBlank() || s.length < 2) return false
        val trimmed = s.trim()
        // 必须彻底剔除各类泛指吃饭/完成用餐等非具体菜品名词
        val nonFoodTerms = setOf(
            "完饭", "吃饭", "吃了饭", "吃完饭", "吃过饭", "用完餐", "用过餐", "用餐",
            "早饭", "午饭", "晚饭", "中饭", "饭", "饭菜", "正餐", "大餐",
            "饱了", "吃饱", "吃饱了", "完了", "东西", "食物", "餐食", "饮食",
            "了饭", "过饭", "顿饭", "一顿饭", "这顿饭", "饭后", "餐后", "饭前", "餐前", "好饭",
            "吃了完饭", "吃完了饭", "吃玩饭", "吃了玩饭", "吃了完", "吃完了", "吃好了", "好饭了", "玩饭", "吃饭了",
            "没吃", "没吃饭", "没吃早饭", "没吃午饭", "没吃晚饭", "不吃", "未进食", "禁食",
            "很饱", "太饱", "吃得很饱", "吃得太饱", "吃得比较清淡", "清淡", "随便吃了点", "随便吃了", "随便吃"
        )
        if (nonFoodTerms.contains(trimmed)) return false
        for (term in nonFoodTerms) {
            if (trimmed == term || trimmed == "吃了$term" || trimmed == "喝了$term") {
                return false
            }
        }
        if (trimmed.endsWith("完饭") || trimmed.endsWith("吃饭") || trimmed.endsWith("吃了饭") || trimmed.endsWith("吃完饭") || trimmed.endsWith("玩饭")) {
            return false
        }

        // 杜绝将用药或血糖识别为饮食
        if (MedicationData.isMedicationText(s)) return false
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
        s = s.replace(Regex("""两(?=[个点单位片粒支袋碗份杯盒包瓶根块盘只斤两])"""), "2")

        // 1. 处理常见阿拉伯数字+点/店+阿拉伯数字或吧/把：如 "9点8" -> "9.8", "9点吧" -> "9.8", "6点2" -> "6.2", "5点吧" -> "5.8"
        // 保护整点分钟（如 "7点30分"、"8点20" 不转为浮点数）
        s = s.replace(Regex("""(?<!\d)(\d{1,2})[点店电典](\d)(?!\d|分)"""), "$1.$2")
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

        // 4. 转换带有量词的整数：如 "六单位" -> "6单位", "十二单位" -> "12单位", "吃了两片" -> "吃了2片", "六个单位" -> "6个单位", "一杯牛奶" -> "1杯牛奶"
        val unitPattern = Pattern.compile("""([零一二三四五六七八九十百]+)(?=\s*(?:单位|个单位|个|U|u|片|粒|支|袋|mmol|毫克|mg|分|碗|份|杯|瓶|盒|包|根|块|盘|只|斤|两))""")
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
