package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed class BackupParseResult {
    data class Success(
        val records: List<InsulinRecord>,
        val count: Int,
        val dateRange: String,
        val fileName: String? = null
    ) : BackupParseResult()

    data class Error(val message: String) : BackupParseResult()
}

object DataBackupManager {

    /**
     * 获取 URI 对应的文件名
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            val name = cursor.getString(index)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    /**
     * 将所有记录导出为标准 ZIP 格式的备份数据包
     * 压缩包内包含：
     * 1. backup_meta.json：备份元数据（版本、导出时间、记录总条数等）
     * 2. records.json：所有记录全量 JSON 数据（包含所有字段，确保导入 100% 无损）
     * 3. records_readable.csv：人类可读的表格格式（带 UTF-8 BOM，方便在 Excel / WPS 中直接双击查看）
     */
    fun exportBackupZip(context: Context, records: List<InsulinRecord>): File {
        val sortedRecords = records.sortedBy { it.date }
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timestamp = System.currentTimeMillis()
        val zipFile = File(context.cacheDir, "血糖数据备份_${today}_$timestamp.zip")

        // 1. 生成元数据 JSON
        val metaJson = JSONObject().apply {
            put("app", "血糖记录本")
            put("formatVersion", 1)
            put("backupDate", today)
            put("timestamp", timestamp)
            put("totalRecords", sortedRecords.size)
            if (sortedRecords.isNotEmpty()) {
                put("earliestDate", sortedRecords.first().date)
                put("latestDate", sortedRecords.last().date)
            }
        }

        // 2. 生成全量 records.json
        val recordsArray = JSONArray()
        for (record in sortedRecords) {
            recordsArray.put(recordToJson(record))
        }
        val recordsRootJson = JSONObject().apply {
            put("version", 1)
            put("records", recordsArray)
        }

        // 3. 生成便于在电脑查看的 CSV 表格 (带 UTF-8 BOM)
        val csvBuilder = StringBuilder()
        csvBuilder.append('\uFEFF')
        csvBuilder.append("日期,早餐空腹血糖,早前血糖,早用药名称,早用药剂量,早后血糖,早餐食谱,午餐前血糖,午用药名称,午用药剂量,午后血糖,午餐食谱,晚餐前血糖,晚用药名称,晚用药剂量,晚后血糖,晚餐食谱,睡前血糖,夜间血糖,睡前用药名称,睡前用药剂量,睡前饮食,备注\r\n")
        for (r in sortedRecords) {
            val cols = listOf(
                r.date,
                r.fastingBG?.toString() ?: "",
                r.preBfBG?.toString() ?: "",
                escapeCsv(r.bfMedName),
                r.bfInsulin?.toString() ?: "",
                r.postBfBG?.toString() ?: "",
                escapeCsv(r.bfDiet),
                r.preLunchBG?.toString() ?: "",
                escapeCsv(r.lunchMedName),
                r.lunchInsulin?.toString() ?: "",
                r.postLunchBG?.toString() ?: "",
                escapeCsv(r.lunchDiet),
                r.preDinnerBG?.toString() ?: "",
                escapeCsv(r.dinnerMedName),
                r.dinnerInsulin?.toString() ?: "",
                r.postDinnerBG?.toString() ?: "",
                escapeCsv(r.dinnerDiet),
                r.preNightBG?.toString() ?: "",
                r.postNightBG?.toString() ?: "",
                escapeCsv(r.nightMedName),
                r.bedtimeInsulin?.toString() ?: "",
                escapeCsv(r.nightDiet),
                escapeCsv(r.notes)
            )
            csvBuilder.append(cols.joinToString(",")).append("\r\n")
        }

        // 写入 ZIP 压缩包
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
            // Entry 1: backup_meta.json
            zipOut.putNextEntry(ZipEntry("backup_meta.json"))
            zipOut.write(metaJson.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // Entry 2: records.json
            zipOut.putNextEntry(ZipEntry("records.json"))
            zipOut.write(recordsRootJson.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // Entry 3: records_readable.csv
            zipOut.putNextEntry(ZipEntry("records_readable.csv"))
            zipOut.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }

        return zipFile
    }

    /**
     * 调用系统分享 / 文件保存意图，导出 ZIP 备份
     */
    fun shareBackupZip(context: Context, zipFile: File, today: String) {
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_SUBJECT, "血糖数据备份 ($today)")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "保存或分享备份数据 (ZIP)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * 从用户选取的 URI 解析备份文件（支持 .zip 格式或直接解析的 .json 格式）
     */
    fun parseBackupFromUri(context: Context, uri: Uri): BackupParseResult {
        val fileName = getFileNameFromUri(context, uri)
        fun openStream(): InputStream? {
            return try {
                when (uri.scheme) {
                    "file" -> {
                        uri.path?.let { path ->
                            val f = File(path)
                            if (f.exists()) java.io.FileInputStream(f) else null
                        } ?: context.contentResolver.openInputStream(uri)
                    }
                    else -> context.contentResolver.openInputStream(uri)
                }
            } catch (_: Exception) {
                null
            }
        }

        return try {
            val inputStream = openStream()
                ?: return BackupParseResult.Error("无法打开所选备份文件，请检查文件访问权限")

            // 先尝试当做 ZIP 格式解析
            val recordsFromZip = parseFromZipStream(inputStream)
            if (recordsFromZip != null && recordsFromZip.isNotEmpty()) {
                val sorted = recordsFromZip.sortedBy { it.date }
                return BackupParseResult.Success(
                    records = sorted,
                    count = sorted.size,
                    dateRange = "${sorted.first().date} ~ ${sorted.last().date}",
                    fileName = fileName
                )
            }

            // 如果不是有效 ZIP，再尝试作为纯 JSON 解析
            val directStream = openStream()
                ?: return BackupParseResult.Error("无法读取文件内容")
            val jsonText = directStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val recordsFromJson = parseRecordsFromJsonText(jsonText)

            if (recordsFromJson != null && recordsFromJson.isNotEmpty()) {
                val sorted = recordsFromJson.sortedBy { it.date }
                return BackupParseResult.Success(
                    records = sorted,
                    count = sorted.size,
                    dateRange = "${sorted.first().date} ~ ${sorted.last().date}",
                    fileName = fileName
                )
            }

            BackupParseResult.Error("未在备份文件中找到有效的血糖记录数据")
        } catch (e: Exception) {
            BackupParseResult.Error("解析备份失败: ${e.localizedMessage ?: "文件损坏或格式不正确"}")
        }
    }

    private fun parseFromZipStream(inputStream: InputStream): List<InsulinRecord>? {
        return try {
            BufferedInputStream(inputStream).use { bis ->
                ZipInputStream(bis).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name == "records.json" || entry.name.endsWith(".json") && !entry.name.contains("meta")) {
                            val jsonText = zipIn.bufferedReader(Charsets.UTF_8).readText()
                            val records = parseRecordsFromJsonText(jsonText)
                            if (!records.isNullOrEmpty()) {
                                return records
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRecordsFromJsonText(jsonText: String): List<InsulinRecord>? {
        return try {
            val list = mutableListOf<InsulinRecord>()
            val trimmed = jsonText.trim()
            if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(jsonToRecord(obj))
                }
            } else if (trimmed.startsWith("{")) {
                val rootObj = JSONObject(trimmed)
                val jsonArray = rootObj.optJSONArray("records") ?: JSONArray()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(jsonToRecord(obj))
                }
            }
            if (list.isNotEmpty()) list else null
        } catch (_: Exception) {
            null
        }
    }

    private fun escapeCsv(text: String): String {
        return if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            "\"${text.replace("\"", "\"\"")}\""
        } else {
            text
        }
    }

    private fun recordToJson(r: InsulinRecord): JSONObject {
        val json = JSONObject()
        json.put("date", r.date)
        r.fastingBG?.let { json.put("fastingBG", it.toDouble()) }
        r.preBfBG?.let { json.put("preBfBG", it.toDouble()) }
        r.postBfBG?.let { json.put("postBfBG", it.toDouble()) }
        json.put("bfMedName", r.bfMedName)
        r.bfInsulin?.let { json.put("bfInsulin", it.toDouble()) }
        json.put("bfDiet", r.bfDiet)

        r.preLunchBG?.let { json.put("preLunchBG", it.toDouble()) }
        r.postLunchBG?.let { json.put("postLunchBG", it.toDouble()) }
        json.put("lunchMedName", r.lunchMedName)
        r.lunchInsulin?.let { json.put("lunchInsulin", it.toDouble()) }
        json.put("lunchDiet", r.lunchDiet)

        r.preDinnerBG?.let { json.put("preDinnerBG", it.toDouble()) }
        r.postDinnerBG?.let { json.put("postDinnerBG", it.toDouble()) }
        json.put("dinnerMedName", r.dinnerMedName)
        r.dinnerInsulin?.let { json.put("dinnerInsulin", it.toDouble()) }
        json.put("dinnerDiet", r.dinnerDiet)

        r.preNightBG?.let { json.put("preNightBG", it.toDouble()) }
        r.postNightBG?.let { json.put("postNightBG", it.toDouble()) }
        json.put("nightMedName", r.nightMedName)
        r.bedtimeInsulin?.let { json.put("bedtimeInsulin", it.toDouble()) }
        json.put("nightDiet", r.nightDiet)

        json.put("notes", r.notes)
        return json
    }

    private fun jsonToRecord(json: JSONObject): InsulinRecord {
        fun optFloat(key: String): Float? {
            if (!json.has(key) || json.isNull(key)) return null
            return json.optDouble(key).toFloat()
        }

        return InsulinRecord(
            date = json.getString("date"),
            fastingBG = optFloat("fastingBG"),
            preBfBG = optFloat("preBfBG"),
            postBfBG = optFloat("postBfBG"),
            bfMedName = json.optString("bfMedName", "胰岛素"),
            bfInsulin = optFloat("bfInsulin"),
            bfDiet = json.optString("bfDiet", ""),

            preLunchBG = optFloat("preLunchBG"),
            postLunchBG = optFloat("postLunchBG"),
            lunchMedName = json.optString("lunchMedName", "胰岛素"),
            lunchInsulin = optFloat("lunchInsulin"),
            lunchDiet = json.optString("lunchDiet", ""),

            preDinnerBG = optFloat("preDinnerBG"),
            postDinnerBG = optFloat("postDinnerBG"),
            dinnerMedName = json.optString("dinnerMedName", "胰岛素"),
            dinnerInsulin = optFloat("dinnerInsulin"),
            dinnerDiet = json.optString("dinnerDiet", ""),

            preNightBG = optFloat("preNightBG"),
            postNightBG = optFloat("postNightBG"),
            nightMedName = json.optString("nightMedName", "胰岛素"),
            bedtimeInsulin = optFloat("bedtimeInsulin"),
            nightDiet = json.optString("nightDiet", ""),

            notes = json.optString("notes", "")
        )
    }
}
