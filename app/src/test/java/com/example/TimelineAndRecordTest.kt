package com.example

import com.example.data.DataBackupManager
import com.example.data.InsulinRecord
import com.example.data.MealPeriod
import com.example.data.PostMealEntry
import com.example.data.PostMealUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TimelineAndRecordTest {

    @Test
    fun testPeriodTimeRecognition() {
        // Morning: 05:00 - 10:59
        assertEquals(MealPeriod.MORNING, InsulinRecord.getPeriodForTime(LocalTime.of(5, 0)))
        assertEquals(MealPeriod.MORNING, InsulinRecord.getPeriodForTime(LocalTime.of(7, 30)))
        assertEquals(MealPeriod.MORNING, InsulinRecord.getPeriodForTime(LocalTime.of(10, 59)))

        // Lunch: 11:00 - 15:59
        assertEquals(MealPeriod.LUNCH, InsulinRecord.getPeriodForTime(LocalTime.of(11, 0)))
        assertEquals(MealPeriod.LUNCH, InsulinRecord.getPeriodForTime(LocalTime.of(12, 30)))
        assertEquals(MealPeriod.LUNCH, InsulinRecord.getPeriodForTime(LocalTime.of(15, 59)))

        // Dinner: 16:00 - 20:59
        assertEquals(MealPeriod.DINNER, InsulinRecord.getPeriodForTime(LocalTime.of(16, 0)))
        assertEquals(MealPeriod.DINNER, InsulinRecord.getPeriodForTime(LocalTime.of(18, 45)))
        assertEquals(MealPeriod.DINNER, InsulinRecord.getPeriodForTime(LocalTime.of(20, 59)))

        // Night: 21:00 - 04:59
        assertEquals(MealPeriod.NIGHT, InsulinRecord.getPeriodForTime(LocalTime.of(21, 0)))
        assertEquals(MealPeriod.NIGHT, InsulinRecord.getPeriodForTime(LocalTime.of(23, 59)))
        assertEquals(MealPeriod.NIGHT, InsulinRecord.getPeriodForTime(LocalTime.of(0, 0)))
        assertEquals(MealPeriod.NIGHT, InsulinRecord.getPeriodForTime(LocalTime.of(2, 30)))
        assertEquals(MealPeriod.NIGHT, InsulinRecord.getPeriodForTime(LocalTime.of(4, 59)))
    }

    @Test
    fun testPostMealSerializationAndParsing() {
        val entries = listOf(
            PostMealEntry(7.2f, "12:30", "餐后1小时"),
            PostMealEntry(8.5f, "13:30", "餐后2小时"),
            PostMealEntry(9.0f, "14:30", "加测")
        )
        val serialized = PostMealUtils.serializeEntries(entries)
        assertTrue(serialized.contains("7.2"))
        assertTrue(serialized.contains("餐后1小时"))
        assertTrue(serialized.contains("12:30"))

        val parsed = PostMealUtils.parseEntries(serialized)
        assertEquals(3, parsed.size)
        assertEquals(7.2f, parsed[0].value, 0.01f)
        assertEquals("12:30", parsed[0].time)
        assertEquals("餐后1小时", parsed[0].tag)

        assertEquals(8.5f, parsed[1].value, 0.01f)
        assertEquals("13:30", parsed[1].time)
        assertEquals("餐后2小时", parsed[1].tag)

        assertEquals(9.0f, parsed[2].value, 0.01f)
        assertEquals("14:30", parsed[2].time)
        assertEquals("加测", parsed[2].tag)
    }

    @Test
    fun testLegacyPostMealCompatibility() {
        // Record with legacy primary only and empty extra
        val legacyRecord = InsulinRecord(
            date = "2026-09-01",
            postBfBG = 6.8f,
            postBfBGExtra = ""
        )
        val list = legacyRecord.getPostMealList(MealPeriod.MORNING)
        assertEquals(1, list.size)
        assertEquals(6.8f, list[0].value, 0.01f)
        assertEquals("餐后", list[0].tag)
        assertEquals("", list[0].time)

        // Empty record
        val emptyRecord = InsulinRecord(date = "2026-09-02")
        val emptyList = emptyRecord.getPostMealList(MealPeriod.MORNING)
        assertTrue(emptyList.isEmpty())

        // Legacy comma-separated extra
        val legacyCommaParsed = PostMealUtils.parseEntries("6.5, 7.8")
        assertEquals(2, legacyCommaParsed.size)
        assertEquals(6.5f, legacyCommaParsed[0].value, 0.01f)
        assertEquals(7.8f, legacyCommaParsed[1].value, 0.01f)
    }

    @Test
    fun testPostMealLifecycleAddDeletePromote() {
        var record = InsulinRecord(date = "2026-09-20")

        // 1. Add first entry: 7.0, 12:00, "餐后1小时"
        val list1 = record.getPostMealList(MealPeriod.MORNING).toMutableList()
        list1.add(PostMealEntry(7.0f, "12:00", "餐后1小时"))
        record = record.copy(
            postBfBG = list1.firstOrNull()?.value,
            postBfBGExtra = PostMealUtils.serializeEntries(list1)
        )

        assertEquals(7.0f, record.postBfBG ?: 0f, 0.01f)
        var currentPosts = record.getPostMealList(MealPeriod.MORNING)
        assertEquals(1, currentPosts.size)
        assertEquals("12:00", currentPosts[0].time)
        assertEquals("餐后1小时", currentPosts[0].tag)

        // 2. Add second entry: 8.2, 13:00, "餐后2小时"
        val list2 = record.getPostMealList(MealPeriod.MORNING).toMutableList()
        list2.add(PostMealEntry(8.2f, "13:00", "餐后2小时"))
        record = record.copy(
            postBfBG = list2.firstOrNull()?.value,
            postBfBGExtra = PostMealUtils.serializeEntries(list2)
        )

        assertEquals(7.0f, record.postBfBG ?: 0f, 0.01f)
        currentPosts = record.getPostMealList(MealPeriod.MORNING)
        assertEquals(2, currentPosts.size)
        assertEquals("餐后1小时", currentPosts[0].tag)
        assertEquals("餐后2小时", currentPosts[1].tag)

        // 3. Add third entry: 6.5, 14:00, "加测"
        val list3 = record.getPostMealList(MealPeriod.MORNING).toMutableList()
        list3.add(PostMealEntry(6.5f, "14:00", "加测"))
        record = record.copy(
            postBfBG = list3.firstOrNull()?.value,
            postBfBGExtra = PostMealUtils.serializeEntries(list3)
        )
        assertEquals(3, record.getPostMealList(MealPeriod.MORNING).size)

        // 4. Delete index 0 (the first entry, 7.0)
        val deleteList = record.getPostMealList(MealPeriod.MORNING).toMutableList()
        deleteList.removeAt(0)
        record = record.copy(
            postBfBG = deleteList.firstOrNull()?.value,
            postBfBGExtra = if (deleteList.isEmpty()) "" else PostMealUtils.serializeEntries(deleteList)
        )

        // Verify remaining entries: second entry (8.2) is now primary AND retained its metadata!
        assertEquals(8.2f, record.postBfBG ?: 0f, 0.01f)
        currentPosts = record.getPostMealList(MealPeriod.MORNING)
        assertEquals(2, currentPosts.size)
        assertEquals(8.2f, currentPosts[0].value, 0.01f)
        assertEquals("13:00", currentPosts[0].time)
        assertEquals("餐后2小时", currentPosts[0].tag) // Metadata preserved!

        assertEquals(6.5f, currentPosts[1].value, 0.01f)
        assertEquals("14:00", currentPosts[1].time)
        assertEquals("加测", currentPosts[1].tag)

        // 5. Delete all remaining
        deleteList.clear()
        record = record.copy(
            postBfBG = deleteList.firstOrNull()?.value,
            postBfBGExtra = ""
        )
        assertNull(record.postBfBG)
        assertTrue(record.getPostMealList(MealPeriod.MORNING).isEmpty())
    }

    @Test
    fun testPostMealAverageCalculation() {
        val morningList = listOf(
            PostMealEntry(6.0f, "08:30", "餐后1小时"),
            PostMealEntry(8.0f, "09:30", "餐后2小时")
        )
        val lunchList = listOf(
            PostMealEntry(7.0f, "13:30", "餐后2小时")
        )
        val record = InsulinRecord(
            date = "2026-09-20",
            postBfBG = 6.0f,
            postBfBGExtra = PostMealUtils.serializeEntries(morningList),
            postLunchBG = 7.0f,
            postLunchBGExtra = PostMealUtils.serializeEntries(lunchList)
        )

        // Average should be (6.0 + 8.0 + 7.0) / 3 = 7.0
        assertEquals(7.0f, record.postMealAverageBG ?: 0f, 0.01f)
    }

    @Test
    fun testExternalEditSync() {
        // If an external editor changes postBfBG from 7.0 to 9.0
        val entries = listOf(
            PostMealEntry(7.0f, "12:00", "餐后1小时")
        )
        val record = InsulinRecord(
            date = "2026-09-20",
            postBfBG = 9.0f, // updated externally
            postBfBGExtra = PostMealUtils.serializeEntries(entries)
        )

        val posts = record.getPostMealList(MealPeriod.MORNING)
        assertEquals(1, posts.size)
        assertEquals(9.0f, posts[0].value, 0.01f) // synced!
        assertEquals("12:00", posts[0].time)
        assertEquals("餐后1小时", posts[0].tag)

        // If postBfBG is set to null externally
        val clearedRecord = record.copy(postBfBG = null)
        assertTrue(clearedRecord.getPostMealList(MealPeriod.MORNING).isEmpty())
    }
}
