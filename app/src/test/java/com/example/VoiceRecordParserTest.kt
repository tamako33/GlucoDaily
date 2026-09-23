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
}
