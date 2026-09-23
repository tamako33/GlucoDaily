package com.example

import com.example.data.BGLevel
import com.example.data.BGUtils
import com.example.data.InsulinRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun insulinCalculations_areCorrect() {
    val record = InsulinRecord(
      date = "2026-09-07",
      fastingBG = 4.1f,
      bfInsulin = 8f,
      postBfBG = 7.5f,
      lunchInsulin = 6f,
      postLunchBG = 8.0f,
      dinnerInsulin = 7f,
      postDinnerBG = 7.0f,
      bedtimeInsulin = 12f
    )

    assertEquals(33f, record.totalInsulin, 0.01f)
    assertEquals(7.5f, record.postMealAverageBG ?: 0f, 0.01f)

    val fastingStatus = BGUtils.evaluateFasting(4.1f)
    assertNotNull(fastingStatus)
    assertEquals(BGLevel.NORMAL, fastingStatus?.level)
    assertEquals("达标", fastingStatus?.label)

    val postMealStatus = BGUtils.evaluatePostMeal(7.5f)
    assertNotNull(postMealStatus)
    assertEquals(BGLevel.NORMAL, postMealStatus?.level)
    assertEquals("达标", postMealStatus?.label)
  }

  @Test
  fun prevNightAssociation_isCorrect() {
    val records = listOf(
      InsulinRecord(date = "2026-09-01", bedtimeInsulin = 12f, dinnerInsulin = 8f),
      InsulinRecord(date = "2026-09-02", fastingBG = 5.8f)
    )

    val prev = BGUtils.getPrevNightInsulin("2026-09-02", records)
    assertNotNull(prev)
    assertEquals(12f, prev?.dose ?: 0f, 0.01f)
    assertEquals("睡前", prev?.type)
    assertEquals(true, prev?.isExactYesterday)
  }

  @Test
  fun prevNightAssociation_nullOrZeroBedtime_doesNotFallbackToDinner() {
    // Case 1: bedtimeInsulin is null, but dinnerInsulin exists
    val recordsWithNullBedtime = listOf(
      InsulinRecord(date = "2026-09-01", bedtimeInsulin = null, dinnerInsulin = 8f),
      InsulinRecord(date = "2026-09-02", fastingBG = 5.8f)
    )
    val prev1 = BGUtils.getPrevNightInsulin("2026-09-02", recordsWithNullBedtime)
    assertNull(prev1)

    // Case 2: bedtimeInsulin is 0f, and dinnerInsulin exists
    val recordsWithZeroBedtime = listOf(
      InsulinRecord(date = "2026-09-01", bedtimeInsulin = 0f, dinnerInsulin = 8f),
      InsulinRecord(date = "2026-09-02", fastingBG = 5.8f)
    )
    val prev2 = BGUtils.getPrevNightInsulin("2026-09-02", recordsWithZeroBedtime)
    assertNull(prev2)
  }
}
