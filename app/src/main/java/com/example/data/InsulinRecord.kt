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
    val bfMedName: String = "胰岛素",
    val bfInsulin: Float? = null,
    val bfMedTiming: String = "餐前",
    val bfDiet: String = "",

    // 午间：餐前、餐后；药名、用药量、用药时机；餐食
    val preLunchBG: Float? = null,
    val postLunchBG: Float? = null,
    val lunchMedName: String = "胰岛素",
    val lunchInsulin: Float? = null,
    val lunchMedTiming: String = "餐前",
    val lunchDiet: String = "",

    // 傍晚：餐前、餐后；药名、用药量、用药时机；餐食
    val preDinnerBG: Float? = null,
    val postDinnerBG: Float? = null,
    val dinnerMedName: String = "胰岛素",
    val dinnerInsulin: Float? = null,
    val dinnerMedTiming: String = "餐前",
    val dinnerDiet: String = "",

    // 夜晚：餐前、餐后；药名、用药量、用药时机；餐食
    val preNightBG: Float? = null,
    val postNightBG: Float? = null,
    val nightMedName: String = "胰岛素",
    val bedtimeInsulin: Float? = null,
    val nightMedTiming: String = "餐前",
    val nightDiet: String = "",

    val notes: String = ""
) {
    val hasMorningData: Boolean
        get() = fastingBG != null || preBfBG != null || postBfBG != null || bfInsulin != null || bfDiet.isNotBlank()

    val hasLunchData: Boolean
        get() = preLunchBG != null || postLunchBG != null || lunchInsulin != null || lunchDiet.isNotBlank()

    val hasDinnerData: Boolean
        get() = preDinnerBG != null || postDinnerBG != null || dinnerInsulin != null || dinnerDiet.isNotBlank()

    val hasNightData: Boolean
        get() = preNightBG != null || postNightBG != null || bedtimeInsulin != null || nightDiet.isNotBlank()

    /**
     * Determines the next recommended period to record.
     * "比如最近一次的记录是上午，则点击添加默认添加为中午的记录"
     */
    fun getNextRecommendedPeriod(): MealPeriod {
        return when {
            hasMorningData && !hasLunchData -> MealPeriod.LUNCH
            hasLunchData && !hasDinnerData -> MealPeriod.DINNER
            hasDinnerData && !hasNightData -> MealPeriod.NIGHT
            !hasMorningData -> {
                val hour = java.time.LocalTime.now().hour
                when {
                    hour < 11 -> MealPeriod.MORNING
                    hour < 15 -> MealPeriod.LUNCH
                    hour < 20 -> MealPeriod.DINNER
                    else -> MealPeriod.NIGHT
                }
            }
            else -> {
                val hour = java.time.LocalTime.now().hour
                when {
                    hour < 11 -> MealPeriod.MORNING
                    hour < 15 -> MealPeriod.LUNCH
                    hour < 20 -> MealPeriod.DINNER
                    else -> MealPeriod.NIGHT
                }
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
            val list = listOfNotNull(postBfBG, postLunchBG, postDinnerBG, postNightBG)
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
