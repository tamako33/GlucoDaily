package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.BackupParseResult
import com.example.data.DataBackupManager
import com.example.data.InsulinRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("糖舒心", appName)
  }

  @Test
  fun `test backup zip export and parse from uri`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testRecords = listOf(
      InsulinRecord(date = "2026-09-17", fastingBG = 5.2f, bfInsulin = 4f),
      InsulinRecord(date = "2026-09-18", fastingBG = 5.6f, bfInsulin = 5f)
    )

    // 导出备份 ZIP 文件
    val zipFile = DataBackupManager.exportBackupZip(context, testRecords)
    assertTrue("备份文件应成功创建且非空", zipFile.exists() && zipFile.length() > 0)

    // 通过 File URI 模拟外部打开与解析
    val fileUri = Uri.fromFile(zipFile)
    val parseResult = DataBackupManager.parseBackupFromUri(context, fileUri)

    assertTrue("解析结果应为 Success", parseResult is BackupParseResult.Success)
    val success = parseResult as BackupParseResult.Success
    assertEquals(2, success.count)
    assertEquals("2026-09-17 ~ 2026-09-18", success.dateRange)
    assertEquals(5.2f, success.records[0].fastingBG ?: 0f, 0.01f)
    assertEquals(5.6f, success.records[1].fastingBG ?: 0f, 0.01f)
  }
}
