package com.example

import com.example.data.InsulinRecord
import com.example.data.VoiceRecordParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class VoiceRecordParserTest {

    @Test
    fun testFullBreakfastSpokenRecognition() {
        val input = "我今天早餐吃了包子和豆浆，餐前血糖5.6，打了4单位门冬胰岛素，餐后血糖7.2"
        val parsed = VoiceRecordParser.parse(input)

        assertEquals("包子和豆浆", parsed.bfDiet)
        assertEquals(5.6f, parsed.preBfBG ?: parsed.fastingBG ?: 0f, 0.01f)
        assertEquals(4f, parsed.bfInsulin ?: 0f, 0.01f)
        assertEquals("门冬胰岛素", parsed.bfMedName)
        assertEquals(7.2f, parsed.postBfBG ?: 0f, 0.01f)
        assertTrue(parsed.hasAnyData())
    }

    @Test
    fun testOnlyPreMealBGMentioned() {
        val input = "今天早餐前血糖5.8"
        val parsed = VoiceRecordParser.parse(input)

        assertEquals(5.8f, parsed.preBfBG ?: parsed.fastingBG ?: 0f, 0.01f)
        assertNull(parsed.postBfBG)
        assertNull(parsed.bfInsulin)
        assertTrue(parsed.bfDiet.isNullOrBlank())
        assertNull(parsed.preLunchBG)
    }

    @Test
    fun testChineseNumberSpokenRecognition() {
        val input = "今天空腹血糖五点八，早饭吃了两个包子，注射了四单位赖脯胰岛素，餐后七点二"
        val parsed = VoiceRecordParser.parse(input)

        assertEquals(5.8f, parsed.fastingBG ?: 0f, 0.01f)
        assertEquals(4f, parsed.bfInsulin ?: 0f, 0.01f)
        assertEquals(7.2f, parsed.postBfBG ?: 0f, 0.01f)
    }

    @Test
    fun testIncrementalMergeKeepsUnmentionedData() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        // 1. 用户早晨只录入了早餐前血糖
        val step1Text = "今天早餐前血糖5.8"
        val parsedStep1 = VoiceRecordParser.parse(step1Text)
        val recordAfterStep1 = parsedStep1.mergeInto(null)

        assertEquals(5.8f, recordAfterStep1.preBfBG ?: recordAfterStep1.fastingBG ?: 0f, 0.01f)
        assertNull(recordAfterStep1.postBfBG)
        assertNull(recordAfterStep1.preLunchBG)

        // 2. 中午用户补充说："中午餐前血糖6.2，吃了一碗米饭炒青菜，打了6单位门冬"
        val step2Text = "中午餐前血糖6.2，吃了一碗米饭炒青菜，打了6单位门冬"
        val parsedStep2 = VoiceRecordParser.parse(step2Text)
        val recordAfterStep2 = parsedStep2.mergeInto(recordAfterStep1)

        // 验证：早晨录入的早餐前血糖依然保留！未被清空或覆盖！
        assertEquals(5.8f, recordAfterStep2.preBfBG ?: recordAfterStep2.fastingBG ?: 0f, 0.01f)
        // 中午的新数据也成功合并
        assertEquals(6.2f, recordAfterStep2.preLunchBG ?: 0f, 0.01f)
        assertTrue(recordAfterStep2.lunchDiet?.contains("米饭炒青菜") == true)
        assertEquals(6f, recordAfterStep2.lunchInsulin ?: 0f, 0.01f)
        assertEquals("门冬胰岛素", recordAfterStep2.lunchMedName)

        // 3. 下午用户提到早餐后血糖："早餐餐后血糖7.3"
        val step3Text = "早餐餐后血糖7.3"
        val parsedStep3 = VoiceRecordParser.parse(step3Text)
        val recordAfterStep3 = parsedStep3.mergeInto(recordAfterStep2)

        // 验证：早餐前、早餐后、午餐数据全部完整保留与更新！
        assertEquals(5.8f, recordAfterStep3.preBfBG ?: recordAfterStep3.fastingBG ?: 0f, 0.01f)
        assertEquals(7.3f, recordAfterStep3.postBfBG ?: 0f, 0.01f)
        assertEquals(6.2f, recordAfterStep3.preLunchBG ?: 0f, 0.01f)
        assertTrue(recordAfterStep3.lunchDiet?.contains("米饭炒青菜") == true)
    }

    @Test
    fun testVoiceRecognitionServiceOfflineEnhancement() {
        val rawInput = "恐腹 血糖 五 点 八 干净 胰岛素 大 六 单位 吃了 两片 二甲双瓜"
        val parsed = com.example.data.VoiceRecognitionService.parseOffline(rawInput)

        assertEquals(5.8f, parsed.fastingBG ?: 0f, 0.01f)
        assertEquals(6f, parsed.bfInsulin ?: 0f, 0.01f)
        assertTrue(parsed.bfMedName?.contains("甘精") == true || parsed.notes?.contains("二甲双胍") == true)
        assertTrue(parsed.hasAnyData())
    }

    @Test
    fun testMorningExerciseAfterMealTypoFix() {
        // "我早上吃完饭后散步了40分种" (含40分种错别字与吃完饭后口语表达)
        val input = "我早上吃完饭后散步了40分种"
        val parsed = com.example.data.VoiceRecognitionService.parseOffline(input)

        assertEquals(com.example.data.MealPeriod.MORNING, parsed.targetPeriod)
        assertNotNull(parsed.bfExercise)
        assertTrue(parsed.bfExercise?.contains("散步") == true)
        assertTrue(parsed.bfExercise?.contains("40") == true)

        val parsedEx = com.example.data.parseExercise(parsed.bfExercise!!)
        assertEquals("散步", parsedEx.name)
        assertEquals("40", parsedEx.duration)
        assertEquals("分钟", parsedEx.unit)
    }

    @Test
    fun testInvertedExerciseBaduanjin() {
        // "打了半小时八段锦"
        val input = "打了半小时八段锦"
        val parsed = com.example.data.VoiceRecognitionService.parseOffline(input)

        val exStr = parsed.bfExercise ?: parsed.lunchExercise ?: parsed.dinnerExercise ?: parsed.nightExercise
        assertNotNull("Exercise string should not be null", exStr)
        assertTrue(exStr!!.contains("八段锦"))
        assertTrue(exStr.contains("30") || exStr.contains("半小时"))

        val parsedEx = com.example.data.parseExercise(exStr)
        assertEquals("八段锦", parsedEx.name)
        assertEquals("30", parsedEx.duration)
        assertEquals("分钟", parsedEx.unit)
    }

    @Test
    fun testPostMealBGDefaultTo2h() {
        // "我餐后血糖6.8" (未明确餐后时长，一律默认餐后2小时)
        val input = "我餐后血糖6.8"
        val parsed = com.example.data.VoiceRecordParser.parse(input)

        val postBG = parsed.postBfBG ?: parsed.postLunchBG ?: parsed.postDinnerBG ?: parsed.postNightBG
        val postTag = when {
            parsed.postBfBG != null -> parsed.bfPostTag
            parsed.postLunchBG != null -> parsed.lunchPostTag
            parsed.postDinnerBG != null -> parsed.dinnerPostTag
            else -> parsed.nightPostTag
        }
        assertEquals(6.8f, postBG ?: 0f, 0.01f)
        assertEquals("餐后2h", postTag)
    }

    @Test
    fun testPostMealHalfHourAndColloquialAgo() {
        // 1. "我餐后半小时血糖6.0"
        val input1 = "我餐后半小时血糖6.0"
        val parsed1 = com.example.data.VoiceRecordParser.parse(input1)
        val postBG1 = parsed1.postBfBG ?: parsed1.postLunchBG ?: parsed1.postDinnerBG ?: parsed1.postNightBG
        val postTag1 = when {
            parsed1.postBfBG != null -> parsed1.bfPostTag
            parsed1.postLunchBG != null -> parsed1.lunchPostTag
            parsed1.postDinnerBG != null -> parsed1.dinnerPostTag
            else -> parsed1.nightPostTag
        }
        assertEquals(6.0f, postBG1 ?: 0f, 0.01f)
        assertEquals("餐后半小时", postTag1)

        // 2. "我半小时前吃的饭我现在血糖7.2"
        val input2 = "我半小时前吃的饭我现在血糖7.2"
        val parsed2 = com.example.data.VoiceRecognitionService.parseOffline(input2)
        val postBG2 = parsed2.postBfBG ?: parsed2.postLunchBG ?: parsed2.postDinnerBG ?: parsed2.postNightBG
        val postTag2 = when {
            parsed2.postBfBG != null -> parsed2.bfPostTag
            parsed2.postLunchBG != null -> parsed2.lunchPostTag
            parsed2.postDinnerBG != null -> parsed2.dinnerPostTag
            else -> parsed2.nightPostTag
        }
        assertEquals(7.2f, postBG2 ?: 0f, 0.01f)
        assertEquals("餐后半小时", postTag2)
    }

    @Test
    fun testFuzzyTimeJustNow() {
        // "我刚刚散步了半小时" (模糊时间词“刚刚”绑定系统当前时段)
        val input = "我刚刚散步了半小时"
        val parsed = com.example.data.VoiceRecognitionService.parseOffline(input)

        val exStr = parsed.bfExercise ?: parsed.lunchExercise ?: parsed.dinnerExercise ?: parsed.nightExercise
        assertNotNull("Should recognize exercise for current period", exStr)
        assertTrue(exStr!!.contains("散步"))
        assertTrue(exStr.contains("30") || exStr.contains("半小时"))
    }
}
