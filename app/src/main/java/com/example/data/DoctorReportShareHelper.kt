package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 医生端随访数据表格与临床报告分享助手 (DoctorReportShareHelper)
 *
 * 架构设计与临床交互规范：
 * 1. 原生 Microsoft Excel (.xlsx) OpenXML 高保真生成：
 *    - 纯标准库 ZipOutputStream 实现，0 外部第三方依赖，不增加 APK 任何包体积；
 *    - 内置临床规范排版：翡翠绿主题表头 (#0F766E)、加粗白字、数值居中、文本左对齐、自适应列宽；
 * 2. 双载荷并发分享策略 (Dual-Payload Sharing Architecture)：
 *    - 文本报告 (EXTRA_TEXT)：在微信、短信、邮件会话气泡中直接展现核心指标概况与近期每日明细，秒级查阅；
 *    - 表格附件 (EXTRA_STREAM)：生成标准 .xlsx 格式文件，供医生/营养师导入电脑端 Excel、WPS 或医院电子病历系统；
 * 3. 国际临床标准达标率 (Time in Range - TIR) 引擎：
 *    - 遵循 ADA 与 CDS 临床指南，智能计算 3.9 ~ 10.0 mmol/L 达标百分比与低血糖发生频次；
 * 4. Android FileProvider 沙箱安全通道：
 *    - 文件存放于私有缓存目录并通过 FileProvider 派发 Content URI 与读权限，兼容各类第三方应用。
 */
object DoctorReportShareHelper {

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getColLetter(colIdx: Int): String {
        var c = colIdx
        val sb = StringBuilder()
        while (c >= 0) {
            sb.insert(0, ('A' + (c % 26)))
            c = (c / 26) - 1
        }
        return sb.toString()
    }

    /**
     * 生成供医生快速查阅的核心指标统计与每日明细纯文本报告
     */
    fun buildDoctorTextReport(records: List<InsulinRecord>, rangeDescription: String = ""): String {
        if (records.isEmpty()) {
            return "【糖舒心 · 随访报表】\n当前周期暂无测量记录。"
        }

        val sortedRecords = records.sortedByDescending { it.date }
        val earliest = sortedRecords.last().date
        val latest = sortedRecords.first().date

        // 收集所有有效血糖
        val fastingList = mutableListOf<Float>()
        val postMealList = mutableListOf<Float>()
        val allBgs = mutableListOf<Float>()

        for (r in sortedRecords) {
            r.fastingBG?.let { fastingList.add(it); allBgs.add(it) }
            r.preBfBG?.let { allBgs.add(it) }
            r.postBfBG?.let { postMealList.add(it); allBgs.add(it) }
            r.getPostMealList(MealPeriod.MORNING).forEach { postMealList.add(it.value); allBgs.add(it.value) }

            r.preLunchBG?.let { allBgs.add(it) }
            r.postLunchBG?.let { postMealList.add(it); allBgs.add(it) }
            r.getPostMealList(MealPeriod.LUNCH).forEach { postMealList.add(it.value); allBgs.add(it.value) }

            r.preDinnerBG?.let { allBgs.add(it) }
            r.postDinnerBG?.let { postMealList.add(it); allBgs.add(it) }
            r.getPostMealList(MealPeriod.DINNER).forEach { postMealList.add(it.value); allBgs.add(it.value) }

            r.preNightBG?.let { allBgs.add(it) }
            r.postNightBG?.let { postMealList.add(it); allBgs.add(it) }
            r.getPostMealList(MealPeriod.NIGHT).forEach { postMealList.add(it.value); allBgs.add(it.value) }
        }

        val fastingAvg = if (fastingList.isNotEmpty()) String.format(Locale.CHINA, "%.1f", fastingList.average()) else "--"
        val postMealAvg = if (postMealList.isNotEmpty()) String.format(Locale.CHINA, "%.1f", postMealList.average()) else "--"
        val maxBg = if (allBgs.isNotEmpty()) String.format(Locale.CHINA, "%.1f", allBgs.maxOrNull() ?: 0f) else "--"
        val minBg = if (allBgs.isNotEmpty()) String.format(Locale.CHINA, "%.1f", allBgs.minOrNull() ?: 0f) else "--"

        // 达标率计算 (3.9 ~ 10.0 mmol/L TIR)
        val inRangeCount = allBgs.count { it in 3.9f..10.0f }
        val lowCount = allBgs.count { it < 3.9f }
        val tirPercent = if (allBgs.isNotEmpty()) ((inRangeCount * 100f) / allBgs.size).toInt() else 0

        val sb = StringBuilder()
        sb.append("📋【糖舒心 · 随访数据报表】\n")
        sb.append("📅 随访周期：$earliest 至 $latest")
        if (rangeDescription.isNotBlank()) sb.append("（$rangeDescription，共 ${records.size} 天）")
        sb.append("\n\n")

        sb.append("📊 临床核心指标概况：\n")
        sb.append("• 血糖总测定次数：${allBgs.size} 次\n")
        sb.append("• 晨起空腹均值：$fastingAvg mmol/L (达标范围 4.4~7.0)\n")
        sb.append("• 餐后血糖均值：$postMealAvg mmol/L (达标范围 4.4~10.0)\n")
        sb.append("• 最高 / 最低值：$maxBg / $minBg mmol/L\n")
        sb.append("• 目标范围内时间 (TIR)：$tirPercent% (达标 $inRangeCount 次)\n")
        sb.append("• 低血糖发生次数 (<3.9)：$lowCount 次${if (lowCount > 0) " ⚠️ 请注意防范低血糖" else ""}\n\n")

        sb.append("📝 近期每日餐段记录明细：\n")
        sb.append("------------------------------------------\n")
        for (r in sortedRecords.take(14)) { // 最多展示近14天精要明细
            sb.append("【${r.date}】\n")
            // 晨间
            val mornItems = mutableListOf<String>()
            r.fastingBG?.let { mornItems.add("空腹: ${String.format(Locale.CHINA, "%.1f", it)}") }
            r.bfInsulin?.let { mornItems.add("用药/胰岛素: ${r.bfMedName} ${it.toInt()}U") }
            r.postBfBG?.let { mornItems.add("早后2h: ${String.format(Locale.CHINA, "%.1f", it)}") }
            if (r.bfDiet.isNotBlank()) mornItems.add("饮食: ${r.getDietSummary(MealPeriod.MORNING)}")
            if (mornItems.isNotEmpty()) sb.append("• 早餐: ").append(mornItems.joinToString(" | ")).append("\n")

            // 午间
            val lunchItems = mutableListOf<String>()
            r.preLunchBG?.let { lunchItems.add("餐前: ${String.format(Locale.CHINA, "%.1f", it)}") }
            r.lunchInsulin?.let { lunchItems.add("用药/胰岛素: ${r.lunchMedName} ${it.toInt()}U") }
            r.postLunchBG?.let { lunchItems.add("午后2h: ${String.format(Locale.CHINA, "%.1f", it)}") }
            if (r.lunchDiet.isNotBlank()) lunchItems.add("饮食: ${r.getDietSummary(MealPeriod.LUNCH)}")
            if (lunchItems.isNotEmpty()) sb.append("• 午餐: ").append(lunchItems.joinToString(" | ")).append("\n")

            // 晚间
            val dinnerItems = mutableListOf<String>()
            r.preDinnerBG?.let { dinnerItems.add("餐前: ${String.format(Locale.CHINA, "%.1f", it)}") }
            r.dinnerInsulin?.let { dinnerItems.add("用药/胰岛素: ${r.dinnerMedName} ${it.toInt()}U") }
            r.postDinnerBG?.let { dinnerItems.add("晚后2h: ${String.format(Locale.CHINA, "%.1f", it)}") }
            if (r.dinnerDiet.isNotBlank()) dinnerItems.add("饮食: ${r.getDietSummary(MealPeriod.DINNER)}")
            if (dinnerItems.isNotEmpty()) sb.append("• 晚餐: ").append(dinnerItems.joinToString(" | ")).append("\n")

            // 睡前
            val nightItems = mutableListOf<String>()
            r.preNightBG?.let { nightItems.add("睡前: ${String.format(Locale.CHINA, "%.1f", it)}") }
            r.bedtimeInsulin?.let { nightItems.add("睡前用药: ${r.nightMedName} ${it.toInt()}U") }
            r.postNightBG?.let { nightItems.add("夜间: ${String.format(Locale.CHINA, "%.1f", it)}") }
            if (r.nightDiet.isNotBlank()) nightItems.add("加餐: ${r.getDietSummary(MealPeriod.NIGHT)}")
            if (nightItems.isNotEmpty()) sb.append("• 睡前: ").append(nightItems.joinToString(" | ")).append("\n")

            if (r.notes.isNotBlank()) {
                sb.append("• 备注: ${r.notes}\n")
            }
            sb.append("------------------------------------------\n")
        }

        if (sortedRecords.size > 14) {
            sb.append("(其余更早记录已同步包含在随附的完整 Excel/CSV 表格文件中)\n")
        }

        return sb.toString()
    }

    /**
     * 生成标准 Microsoft Excel (.xlsx) 表格文件
     * - 原生 OpenXML ZIP 架构打包，0 外部依赖
     * - 内置临床规范排版：翡翠绿主题表头 (#0F766E)、白字加粗、数值居中、文本左对齐、自适应列宽、细边框
     */
    fun createDoctorXlsxFile(context: Context, records: List<InsulinRecord>, fileName: String): File {
        val sortedRecords = records.sortedByDescending { it.date }
        val xlsxFile = File(context.cacheDir, fileName)

        val headerTitles = listOf(
            "日期", "早晨空腹血糖(mmol/L)", "早前用药名称", "早用药剂量(U)", "早后血糖(mmol/L)", "早餐饮食", "早间运动",
            "午餐前血糖(mmol/L)", "午用药名称", "午用药剂量(U)", "午后血糖(mmol/L)", "午餐饮食", "午间运动",
            "晚餐前血糖(mmol/L)", "晚用药名称", "晚用药剂量(U)", "晚后血糖(mmol/L)", "晚餐饮食", "晚间运动",
            "睡前血糖(mmol/L)", "睡前用药名称", "睡前用药剂量(U)", "夜间血糖(mmol/L)", "睡前饮食", "医生备注"
        )
        val colWidths = listOf(
            13, 22, 16, 15, 22, 26, 18,
            22, 16, 15, 22, 26, 18,
            22, 16, 15, 22, 26, 18,
            20, 16, 16, 20, 22, 30
        )

        val sheetDataBuilder = StringBuilder()
        // 1. 表头行 (r=1)
        sheetDataBuilder.append("""<row r="1" ht="26" customHeight="1">""")
        headerTitles.forEachIndexed { colIdx, title ->
            val colLetter = getColLetter(colIdx)
            sheetDataBuilder.append("""<c r="${colLetter}1" s="1" t="inlineStr"><is><t>${escapeXml(title)}</t></is></c>""")
        }
        sheetDataBuilder.append("</row>")

        // 2. 数据行
        sortedRecords.forEachIndexed { rowIdx, r ->
            val rNum = rowIdx + 2
            sheetDataBuilder.append("""<row r="$rNum" ht="20" customHeight="1">""")

            val bfPostStr = r.getPostMealList(MealPeriod.MORNING).let { list ->
                if (list.isEmpty()) (r.postBfBG?.toString() ?: "") else list.joinToString(";") { "${it.value}${if (it.tag.isNotBlank()) "(${it.tag})" else ""}" }
            }
            val lunchPostStr = r.getPostMealList(MealPeriod.LUNCH).let { list ->
                if (list.isEmpty()) (r.postLunchBG?.toString() ?: "") else list.joinToString(";") { "${it.value}${if (it.tag.isNotBlank()) "(${it.tag})" else ""}" }
            }
            val dinnerPostStr = r.getPostMealList(MealPeriod.DINNER).let { list ->
                if (list.isEmpty()) (r.postDinnerBG?.toString() ?: "") else list.joinToString(";") { "${it.value}${if (it.tag.isNotBlank()) "(${it.tag})" else ""}" }
            }
            val nightPostStr = r.getPostMealList(MealPeriod.NIGHT).let { list ->
                if (list.isEmpty()) (r.postNightBG?.toString() ?: "") else list.joinToString(";") { "${it.value}${if (it.tag.isNotBlank()) "(${it.tag})" else ""}" }
            }

            val rowValues: List<Triple<String, Boolean, Boolean>> = listOf(
                Triple(r.date, false, false),
                Triple(r.fastingBG?.toString() ?: "", false, r.fastingBG != null),
                Triple(r.bfMedName, false, false),
                Triple(r.bfInsulin?.toString() ?: "", false, r.bfInsulin != null),
                Triple(bfPostStr, false, false),
                Triple(r.getDietSummary(MealPeriod.MORNING), true, false),
                Triple(r.bfExercise, true, false),
                Triple(r.preLunchBG?.toString() ?: "", false, r.preLunchBG != null),
                Triple(r.lunchMedName, false, false),
                Triple(r.lunchInsulin?.toString() ?: "", false, r.lunchInsulin != null),
                Triple(lunchPostStr, false, false),
                Triple(r.getDietSummary(MealPeriod.LUNCH), true, false),
                Triple(r.lunchExercise, true, false),
                Triple(r.preDinnerBG?.toString() ?: "", false, r.preDinnerBG != null),
                Triple(r.dinnerMedName, false, false),
                Triple(r.dinnerInsulin?.toString() ?: "", false, r.dinnerInsulin != null),
                Triple(dinnerPostStr, false, false),
                Triple(r.getDietSummary(MealPeriod.DINNER), true, false),
                Triple(r.dinnerExercise, true, false),
                Triple(r.preNightBG?.toString() ?: "", false, r.preNightBG != null),
                Triple(r.nightMedName, false, false),
                Triple(r.bedtimeInsulin?.toString() ?: "", false, r.bedtimeInsulin != null),
                Triple(nightPostStr, false, false),
                Triple(r.getDietSummary(MealPeriod.NIGHT), true, false),
                Triple(r.notes, true, false)
            )

            rowValues.forEachIndexed { colIdx, (value, isLeft, isNum) ->
                val colLetter = getColLetter(colIdx)
                val cellRef = "$colLetter$rNum"
                if (value.isNotBlank()) {
                    if (isNum) {
                        sheetDataBuilder.append("""<c r="$cellRef" s="0"><v>$value</v></c>""")
                    } else {
                        val styleId = if (isLeft) "2" else "0"
                        sheetDataBuilder.append("""<c r="$cellRef" s="$styleId" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>""")
                    }
                } else {
                    sheetDataBuilder.append("""<c r="$cellRef" s="0"/>""")
                }
            }
            sheetDataBuilder.append("</row>")
        }

        val colsXml = buildString {
            colWidths.forEachIndexed { idx, width ->
                val cNum = idx + 1
                append("""<col min="$cNum" max="$cNum" width="$width" customWidth="1"/>""")
            }
        }

        val sheetXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>$colsXml</cols>
  <sheetData>$sheetDataBuilder</sheetData>
</worksheet>"""

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="血糖随访记录表" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Arial"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Arial"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0F766E"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFD1D5DB"/></left>
      <right style="thin"><color rgb="FFD1D5DB"/></right>
      <top style="thin"><color rgb="FFD1D5DB"/></top>
      <bottom style="thin"><color rgb="FFD1D5DB"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="3">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center"/>
    </xf>
  </cellXfs>
</styleSheet>"""

        ZipOutputStream(BufferedOutputStream(FileOutputStream(xlsxFile))).use { zos ->
            fun put(path: String, content: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            put("[Content_Types].xml", contentTypesXml)
            put("_rels/.rels", relsXml)
            put("xl/_rels/workbook.xml.rels", workbookRelsXml)
            put("xl/workbook.xml", workbookXml)
            put("xl/styles.xml", stylesXml)
            put("xl/worksheets/sheet1.xml", sheetXml)
        }

        return xlsxFile
    }

    /**
     * 生成标准 CSV 表格文件（保留向后兼容）
     */
    fun createDoctorCsvFile(context: Context, records: List<InsulinRecord>, fileName: String): File {
        val sortedRecords = records.sortedByDescending { it.date }
        val csvFile = File(context.cacheDir, fileName)

        val csvBuilder = StringBuilder()
        csvBuilder.append('\uFEFF') // UTF-8 BOM
        csvBuilder.append("日期,早晨空腹血糖(mmol/L),早前用药名称,早用药剂量(U),早后血糖(mmol/L),早餐饮食,早间运动,午餐前血糖(mmol/L),午用药名称,午用药剂量(U),午后血糖(mmol/L),午餐饮食,午间运动,晚餐前血糖(mmol/L),晚用药名称,晚用药剂量(U),晚后血糖(mmol/L),晚餐饮食,晚间运动,睡前血糖(mmol/L),睡前用药名称,睡前用药剂量(U),夜间血糖(mmol/L),睡前饮食,医生备注\r\n")

        for (r in sortedRecords) {
            val bfPostStr = r.getPostMealList(MealPeriod.MORNING).let { list ->
                if (list.isEmpty()) (r.postBfBG?.toString() ?: "") else list.joinToString(";") { "${it.value}${if (it.tag.isNotBlank()) "(${it.tag})" else ""}" }
            }
            val lunchPostStr = r.getPostMealList(MealPeriod.LUNCH).let { list ->
                if (list.isEmpty()) (r.postLunchBG?.toString() ?: "") else list.joinToString(";") { "${it.value}${if (it.tag.isNotBlank()) "(${it.tag})" else ""}" }
            }
            val dinnerPostStr = r.getPostMealList(MealPeriod.DINNER).let { list ->
                if (list.isEmpty()) (r.postDinnerBG?.toString() ?: "") else list.joinToString(";") { "${it.value}${if (it.tag.isNotBlank()) "(${it.tag})" else ""}" }
            }
            val nightPostStr = r.getPostMealList(MealPeriod.NIGHT).let { list ->
                if (list.isEmpty()) (r.postNightBG?.toString() ?: "") else list.joinToString(";") { "${it.value}${if (it.tag.isNotBlank()) "(${it.tag})" else ""}" }
            }

            val cols = listOf(
                r.date,
                r.fastingBG?.toString() ?: "",
                escapeCsv(r.bfMedName),
                r.bfInsulin?.toString() ?: "",
                escapeCsv(bfPostStr),
                escapeCsv(r.getDietSummary(MealPeriod.MORNING)),
                escapeCsv(r.bfExercise),
                r.preLunchBG?.toString() ?: "",
                escapeCsv(r.lunchMedName),
                r.lunchInsulin?.toString() ?: "",
                escapeCsv(lunchPostStr),
                escapeCsv(r.getDietSummary(MealPeriod.LUNCH)),
                escapeCsv(r.lunchExercise),
                r.preDinnerBG?.toString() ?: "",
                escapeCsv(r.dinnerMedName),
                r.dinnerInsulin?.toString() ?: "",
                escapeCsv(dinnerPostStr),
                escapeCsv(r.getDietSummary(MealPeriod.DINNER)),
                escapeCsv(r.dinnerExercise),
                r.preNightBG?.toString() ?: "",
                escapeCsv(r.nightMedName),
                r.bedtimeInsulin?.toString() ?: "",
                escapeCsv(nightPostStr),
                escapeCsv(r.getDietSummary(MealPeriod.NIGHT)),
                escapeCsv(r.notes)
            )
            csvBuilder.append(cols.joinToString(",")).append("\r\n")
        }

        BufferedOutputStream(FileOutputStream(csvFile)).use { out ->
            out.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
        }

        return csvFile
    }

    /**
     * 一键调起系统导出与分享，生成原生 Excel (.xlsx) 随访数据表格
     */
    fun shareDoctorReport(
        context: Context,
        records: List<InsulinRecord>,
        rangeDescription: String = "近期随访记录"
    ) {
        if (records.isEmpty()) {
            Toast.makeText(context, "当前暂无随访数据可供导出", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val fileName = "糖舒心_血糖随访数据表_${today}.xlsx"
            val xlsxFile = createDoctorXlsxFile(context, records, fileName)
            val textReport = buildDoctorTextReport(records, rangeDescription)

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                xlsxFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, textReport)
                putExtra(Intent.EXTRA_SUBJECT, "糖舒心 · 患者血糖与胰岛素随访数据表 ($rangeDescription)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "导出随访表格 (Excel)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "导出表格失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
