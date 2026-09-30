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

/**
 * 医生端随访数据表格与临床报告分享助手 (DoctorReportShareHelper)
 *
 * 架构设计与临床交互规范：
 * 1. 双载荷并发分享策略 (Dual-Payload Sharing Architecture)：
 *    - 文本报告 (EXTRA_TEXT)：在微信、短信、邮件会话气泡中直接以排版整洁的纯文本展现关键随访指标与近期每日明细，医生无需下载文件即可一眼秒懂；
 *    - 表格附件 (EXTRA_STREAM)：生成标准 CSV 文件作为附件附带，供医生/营养师导入院内电子病历 (EMR) 或科研分析系统；
 * 2. 国际临床标准达标率 (Time in Range - TIR) 引擎：
 *    - 严格遵循 ADA（美国糖尿病协会）与 CDS（中华医学会糖尿病学分会）指南；
 *    - 目标控制区间锁定为 3.9 ~ 10.0 mmol/L，晨起空腹基线 4.4 ~ 7.0 mmol/L；
 *    - 智能统计低血糖预警次数 (<3.9 mmol/L)，一旦发生即在报表显著标明红标警示；
 * 3. 跨平台 Excel 零乱码保障 (RFC 4180 + UTF-8 BOM)：
 *    - 文件头部写入 `\uFEFF` (Byte Order Mark)，强制 Windows/macOS 版本的 Microsoft Excel 自动识别为 UTF-8 编码，彻底根除中文字符乱码痛点；
 *    - 字段级 [escapeCsv] 转义，对多餐后点位分隔符、逗号、双引号实施 RFC 4180 规范转义；
 * 4. Android FileProvider 沙箱安全通道：
 *    - 文件临时存储于应用私有缓存目录 [context.cacheDir]；
 *    - 通过 [FileProvider.getUriForFile] 派发受控 Content URI，配置 [FLAG_GRANT_READ_URI_PERMISSION]，确保分享至微信、QQ、企微等外部应用合规安全。
 */
object DoctorReportShareHelper {

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
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
     * 生成标准 CSV 表格文件（带 UTF-8 BOM，Excel 双击直接正常打开无乱码）
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
     * 一键调起系统分享，供病人发给医生或营养师
     */
    fun shareDoctorReport(
        context: Context,
        records: List<InsulinRecord>,
        rangeDescription: String = "近期随访记录"
    ) {
        if (records.isEmpty()) {
            Toast.makeText(context, "当前暂无随访数据可供分享", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val fileName = "糖舒心_血糖随访数据表_${today}.csv"
            val csvFile = createDoctorCsvFile(context, records, fileName)
            val textReport = buildDoctorTextReport(records, rangeDescription)

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, textReport)
                putExtra(Intent.EXTRA_SUBJECT, "糖舒心 · 患者血糖与胰岛素随访数据表 ($rangeDescription)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "分享数据表格给医生").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "调起分享失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
