package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Entity(tableName = "insulin_records")
data class InsulinRecord(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD

    // 晨间：空腹、餐前、餐后；药名、用药量、用药时机；餐食
    val fastingBG: Float? = null,
    val preBfBG: Float? = null,
    val postBfBG: Float? = null,
    val postBfBGExtra: String = "",
    val bfMedName: String = "胰岛素",
    val bfInsulin: Float? = null,
    val bfMedTiming: String = "餐前",
    val bfDiet: String = "",

    // 午间：餐前、餐后；药名、用药量、用药时机；餐食
    val preLunchBG: Float? = null,
    val postLunchBG: Float? = null,
    val postLunchBGExtra: String = "",
    val lunchMedName: String = "胰岛素",
    val lunchInsulin: Float? = null,
    val lunchMedTiming: String = "餐前",
    val lunchDiet: String = "",

    // 傍晚：餐前、餐后；药名、用药量、用药时机；餐食
    val preDinnerBG: Float? = null,
    val postDinnerBG: Float? = null,
    val postDinnerBGExtra: String = "",
    val dinnerMedName: String = "胰岛素",
    val dinnerInsulin: Float? = null,
    val dinnerMedTiming: String = "餐前",
    val dinnerDiet: String = "",

    // 夜晚：餐前、餐后；药名、用药量、用药时机；餐食
    val preNightBG: Float? = null,
    val postNightBG: Float? = null,
    val postNightBGExtra: String = "",
    val nightMedName: String = "胰岛素",
    val bedtimeInsulin: Float? = null,
    val nightMedTiming: String = "餐前",
    val nightDiet: String = "",

    val notes: String = ""
) {
    val hasMorningData: Boolean
        get() = fastingBG != null || preBfBG != null || postBfBG != null || postBfBGExtra.isNotBlank() || bfInsulin != null || bfDiet.isNotBlank()

    val hasLunchData: Boolean
        get() = preLunchBG != null || postLunchBG != null || postLunchBGExtra.isNotBlank() || lunchInsulin != null || lunchDiet.isNotBlank()

    val hasDinnerData: Boolean
        get() = preDinnerBG != null || postDinnerBG != null || postDinnerBGExtra.isNotBlank() || dinnerInsulin != null || dinnerDiet.isNotBlank()

    val hasNightData: Boolean
        get() = preNightBG != null || postNightBG != null || postNightBGExtra.isNotBlank() || bedtimeInsulin != null || nightDiet.isNotBlank()

    /**
     * 获取指定餐段的所有餐后血糖记录（包含首个及后续多次增加的记录）
     */
    fun getPostMealList(period: MealPeriod): List<PostMealEntry> {
        val (primary, extra) = when (period) {
            MealPeriod.MORNING -> postBfBG to postBfBGExtra
            MealPeriod.LUNCH -> postLunchBG to postLunchBGExtra
            MealPeriod.DINNER -> postDinnerBG to postDinnerBGExtra
            MealPeriod.NIGHT -> postNightBG to postNightBGExtra
        }
        if (extra.isNotBlank()) {
            val parsed = PostMealUtils.parseEntries(extra).toMutableList()
            if (parsed.isNotEmpty()) {
                // 若外部单独编辑修改了 primary 字段，保持首条记录与 primary 保持同步
                if (primary != null && parsed[0].value != primary) {
                    parsed[0] = parsed[0].copy(value = primary)
                } else if (primary == null) {
                    // 若外部清空了 primary，则视为全部清空
                    return emptyList()
                }
                return parsed
            }
        }
        return if (primary != null) listOf(PostMealEntry(primary, "", "餐后")) else emptyList()
    }

    /**
     * Determines the next recommended period to record.
     * "比如最近一次的记录是上午，则点击添加默认添加为中午的记录"
     */
    fun getNextRecommendedPeriod(): MealPeriod {
        return when {
            hasMorningData && !hasLunchData -> MealPeriod.LUNCH
            hasLunchData && !hasDinnerData -> MealPeriod.DINNER
            hasDinnerData && !hasNightData -> MealPeriod.NIGHT
            else -> getPeriodForTime()
        }
    }

    companion object {
        /**
         * 调用系统时间自动识别当前属于哪一个时段（早，中，晚，睡前）
         * - 早晨 (05:00 - 10:59): 晨间
         * - 中午 (11:00 - 15:59): 午间
         * - 傍晚 (16:00 - 20:59): 傍晚
         * - 睡前 (21:00 - 04:59): 睡前
         */
        fun getPeriodForTime(time: java.time.LocalTime = java.time.LocalTime.now()): MealPeriod {
            val hour = time.hour
            return when {
                hour in 5..10 -> MealPeriod.MORNING
                hour in 11..15 -> MealPeriod.LUNCH
                hour in 16..20 -> MealPeriod.DINNER
                else -> MealPeriod.NIGHT
            }
        }
    }

    val totalInsulin: Float
        get() {
            var sum = 0f
            if (bfInsulin != null && MedicationData.detectUnit(bfMedName) == "U") sum += bfInsulin
            if (lunchInsulin != null && MedicationData.detectUnit(lunchMedName) == "U") sum += lunchInsulin
            if (dinnerInsulin != null && MedicationData.detectUnit(dinnerMedName) == "U") sum += dinnerInsulin
            if (bedtimeInsulin != null && MedicationData.detectUnit(nightMedName) == "U") sum += bedtimeInsulin
            return sum
        }

    val postMealAverageBG: Float?
        get() {
            val list = mutableListOf<Float>()
            MealPeriod.entries.forEach { p ->
                getPostMealList(p).forEach { list.add(it.value) }
            }
            return if (list.isNotEmpty()) list.average().toFloat() else null
        }

    val weekdayText: String
        get() = try {
            val localDate = LocalDate.parse(date)
            when (localDate.dayOfWeek.value) {
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

enum class MealPeriod(val title: String, val periodTag: String, val iconText: String) {
    MORNING("晨间", "早餐时段", "🌅"),
    LUNCH("午间", "午餐时段", "☀️"),
    DINNER("傍晚", "晚餐时段", "🌆"),
    NIGHT("睡前", "睡前时段", "🌙")
}


enum class BGLevel {
    LOW,
    NORMAL,
    HIGH
}

data class BGStatus(
    val valueText: String,
    val level: BGLevel,
    val label: String
)

object BGUtils {
    fun evaluateFasting(bg: Float?): BGStatus? {
        if (bg == null) return null
        return when {
            bg < 3.9f -> BGStatus(String.format(Locale.US, "%.1f", bg), BGLevel.LOW, "偏低")
            bg <= 7.0f -> BGStatus(String.format(Locale.US, "%.1f", bg), BGLevel.NORMAL, "达标")
            else -> BGStatus(String.format(Locale.US, "%.1f", bg), BGLevel.HIGH, "偏高")
        }
    }

    fun evaluatePreMeal(bg: Float?): BGStatus? {
        if (bg == null) return null
        return when {
            bg < 3.9f -> BGStatus(String.format(Locale.US, "%.1f", bg), BGLevel.LOW, "偏低")
            bg <= 7.0f -> BGStatus(String.format(Locale.US, "%.1f", bg), BGLevel.NORMAL, "达标")
            else -> BGStatus(String.format(Locale.US, "%.1f", bg), BGLevel.HIGH, "偏高")
        }
    }

    fun evaluatePostMeal(bg: Float?): BGStatus? {
        if (bg == null) return null
        return when {
            bg < 3.9f -> BGStatus(String.format(Locale.US, "%.1f", bg), BGLevel.LOW, "偏低")
            bg <= 10.0f -> BGStatus(String.format(Locale.US, "%.1f", bg), BGLevel.NORMAL, "达标")
            else -> BGStatus(String.format(Locale.US, "%.1f", bg), BGLevel.HIGH, "偏高")
        }
    }

    fun getTodayString(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun getPrevNightInsulin(targetDate: String, allRecords: List<InsulinRecord>): PrevNightInfo? {
        if (targetDate.isBlank() || allRecords.isEmpty()) return null

        val exactYesterdayStr = try {
            val current = LocalDate.parse(targetDate)
            current.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            null
        }

        // Try exact yesterday first
        var prev = allRecords.find { it.date == exactYesterdayStr }

        // Fallback to latest earlier date if yesterday is missing
        if (prev == null) {
            prev = allRecords
                .filter { it.date < targetDate }
                .maxByOrNull { it.date }
        }

        if (prev == null) return null

        val isExactYesterday = (prev.date == exactYesterdayStr)

        // 仅取前一晚睡前用药（若未注射/未填或为0，则不显示，绝不回退到晚餐用药）
        val bedtime = prev.bedtimeInsulin
        if (bedtime == null || bedtime <= 0f) {
            return null
        }

        return PrevNightInfo(bedtime, "睡前", prev.date, isExactYesterday, prev.nightMedName)
    }
}

data class PrevNightInfo(
    val dose: Float,
    val type: String,
    val date: String,
    val isExactYesterday: Boolean,
    val medName: String = ""
)

/**
 * 餐后血糖条目（支持在同一餐段记录多条）
 */
data class PostMealEntry(
    val value: Float,
    val time: String = "", // 测量时间点，如 "13:30"
    val tag: String = ""   // 标签，如 "餐后1小时"、"餐后2小时"、"加测"
)

object PostMealUtils {
    fun parseEntries(extraStr: String): List<PostMealEntry> {
        if (extraStr.isBlank()) return emptyList()
        val list = mutableListOf<PostMealEntry>()
        // 1. Try standard org.json (Android runtime)
        try {
            val arr = org.json.JSONArray(extraStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val v = obj.optDouble("v", 0.0).toFloat()
                val t = obj.optString("t", "")
                val g = obj.optString("g", "")
                if (v > 0f) {
                    list.add(PostMealEntry(v, t, g))
                }
            }
            if (list.isNotEmpty()) return list
        } catch (_: Throwable) {
            // JVM unit test stub or non-json format
        }

        // 2. Regex fallback for JVM unit test or lightweight JSON
        val regex = Regex("""\{"v":([0-9.]+)(?:,"t":"([^"]*)")?(?:,"g":"([^"]*)")?\}""")
        val matches = regex.findAll(extraStr).toList()
        if (matches.isNotEmpty()) {
            for (m in matches) {
                val v = m.groupValues[1].toFloatOrNull() ?: continue
                val t = m.groupValues.getOrNull(2) ?: ""
                val g = m.groupValues.getOrNull(3) ?: ""
                if (v > 0f) {
                    list.add(PostMealEntry(v, t, g))
                }
            }
            return list
        }

        // 3. Fallback for legacy comma-separated values (e.g. "7.5,8.2")
        extraStr.split(",").forEach {
            it.trim().toFloatOrNull()?.let { v ->
                if (v > 0f) list.add(PostMealEntry(v, "", ""))
            }
        }
        return list
    }

    fun serializeEntries(entries: List<PostMealEntry>): String {
        if (entries.isEmpty()) return ""
        return entries.joinToString(prefix = "[", postfix = "]", separator = ",") { e ->
            buildString {
                append("{\"v\":")
                append(e.value)
                if (e.time.isNotBlank()) {
                    append(",\"t\":\"")
                    append(e.time.replace("\"", "\\\""))
                    append("\"")
                }
                if (e.tag.isNotBlank()) {
                    append(",\"g\":\"")
                    append(e.tag.replace("\"", "\\\""))
                    append("\"")
                }
                append("}")
            }
        }
    }
}

