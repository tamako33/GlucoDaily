package com.example.data

/**
 * 用餐/饮食条目数据实体：
 * 支持在同一餐段记录正餐及多次加餐（正餐、加餐1、加餐2、加餐3...）
 */
data class DietEntry(
    val content: String,
    val time: String = "", // 进餐时间点，如 "08:00"、"10:30"
    val tag: String = "正餐" // 标签："正餐"、"加餐1"、"加餐2"、"加餐3"...
)

/**
 * 饮食/用餐序列化与格式化工具类 (DietUtils)：
 * 1. 结构与 [PostMealUtils] 完全对齐，支持多加餐动态扩展；
 * 2. 100% 向后兼容纯文本旧记录与 JSON 新记录；
 * 3. 具备临床报告格式化能力。
 */
object DietUtils {

    /**
     * 解析饮食字符串为 DietEntry 列表
     * - 支持标准 JSON 格式：[{"c":"燕麦片","t":"08:00","g":"正餐"},{"c":"苹果","t":"10:30","g":"加餐1"}]
     * - 兼容传统纯文本字符串："燕麦片半碗、水煮蛋1个" -> [DietEntry("燕麦片半碗、水煮蛋1个", "", "正餐")]
     * - 兼容分号/冒号格式："正餐: 燕麦片；加餐1: 苹果"
     */
    fun parseEntries(dietStr: String): List<DietEntry> {
        val raw = dietStr.trim()
        if (raw.isBlank()) return emptyList()

        val list = mutableListOf<DietEntry>()
        if (raw.startsWith("[") && raw.endsWith("]")) {
            // 1. 尝试标准 Android org.json 解析
            try {
                val arr = org.json.JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val c = obj.optString("c", "").trim()
                    val t = obj.optString("t", "").trim()
                    val g = obj.optString("g", "正餐").trim().ifBlank { "正餐" }
                    if (c.isNotBlank()) {
                        list.add(DietEntry(c, t, g))
                    }
                }
                if (list.isNotEmpty()) return list
            } catch (_: Throwable) {
                // JVM 单元测试 stub 或异常后备
            }

            // 2. 正则后备处理
            val regex = Regex("""\{"c":"([^"]*)"(?:,"t":"([^"]*)")?(?:,"g":"([^"]*)")?\}""")
            val matches = regex.findAll(raw).toList()
            if (matches.isNotEmpty()) {
                for (m in matches) {
                    val c = m.groupValues[1].replace("\\\"", "\"").trim()
                    val t = m.groupValues.getOrNull(2)?.replace("\\\"", "\"")?.trim() ?: ""
                    val g = m.groupValues.getOrNull(3)?.replace("\\\"", "\"")?.trim()?.ifBlank { "正餐" } ?: "正餐"
                    if (c.isNotBlank()) {
                        list.add(DietEntry(c, t, g))
                    }
                }
                if (list.isNotEmpty()) return list
            }
        }

        // 3. 兼容带分号/冒号的文本格式："正餐: 燕麦片；加餐1: 苹果"
        if (raw.contains("正餐") || raw.contains("加餐")) {
            val parts = raw.split("；", ";", "\n")
            var matchedAny = false
            for (p in parts) {
                val trimmed = p.trim()
                val colonIdx = trimmed.indexOfAny(charArrayOf(':', '：'))
                if (colonIdx > 0) {
                    val tagPart = trimmed.substring(0, colonIdx).trim()
                    val contentPart = trimmed.substring(colonIdx + 1).trim()
                    if (tagPart.startsWith("正餐") || tagPart.startsWith("加餐")) {
                        if (contentPart.isNotBlank()) {
                            list.add(DietEntry(contentPart, "", tagPart))
                            matchedAny = true
                        }
                    }
                }
            }
            if (matchedAny && list.isNotEmpty()) return list
        }

        // 4. 默认将普通单段文本视为“正餐”
        return listOf(DietEntry(content = raw, time = "", tag = "正餐"))
    }

    /**
     * 将 DietEntry 列表序列化为持久化存储字符串
     * - 若仅有 1 条且为正餐且无时间，直接存为纯文本以实现最大向后兼容；
     * - 若有多条或加餐，采用轻量 JSON 存储。
     */
    fun serializeEntries(entries: List<DietEntry>): String {
        val valid = entries.filter { it.content.isNotBlank() }
        if (valid.isEmpty()) return ""
        if (valid.size == 1 && (valid[0].tag == "正餐" || valid[0].tag.isBlank()) && valid[0].time.isBlank()) {
            return valid[0].content.trim()
        }
        return valid.joinToString(prefix = "[", postfix = "]", separator = ",") { e ->
            buildString {
                append("{\"c\":\"")
                append(e.content.replace("\"", "\\\""))
                append("\"")
                if (e.time.isNotBlank()) {
                    append(",\"t\":\"")
                    append(e.time.replace("\"", "\\\""))
                    append("\"")
                }
                append(",\"g\":\"")
                append(e.tag.replace("\"", "\\\""))
                append("\"}")
            }
        }
    }

    /**
     * 将 DietEntry 格式化为临床表格/随访报告/分享纯文本展示
     * 如："正餐: 燕麦片半碗、水煮蛋；加餐1: 苹果半个 (10:30)"
     */
    fun formatSummary(entries: List<DietEntry>): String {
        val valid = entries.filter { it.content.isNotBlank() }
        if (valid.isEmpty()) return ""
        if (valid.size == 1 && valid[0].tag == "正餐") {
            return valid[0].content
        }
        return valid.joinToString("；") { e ->
            val timePart = if (e.time.isNotBlank()) " (${e.time})" else ""
            "${e.tag}: ${e.content}$timePart"
        }
    }

    fun normalizeTag(tag: String): String {
        val t = tag.trim()
        if (t == "正餐" || t.isBlank()) return "正餐"
        val m = Regex("""^加餐(\d+)$""").find(t)
        if (m != null) return "加餐${m.groupValues[1]}"
        if (t == "加餐") return "加餐1"
        return t
    }

    fun isTagMatch(tagA: String, tagB: String): Boolean {
        if (tagA.trim().equals(tagB.trim(), ignoreCase = true)) return true
        return normalizeTag(tagA) == normalizeTag(tagB)
    }

    fun getNextSnackTag(existingTags: Collection<String>): String {
        var maxSnackNum = 2 // 默认保证加餐1、加餐2存在后，从加餐3开始递增
        existingTags.forEach { t ->
            val norm = normalizeTag(t)
            val m = Regex("""^加餐(\d+)$""").find(norm)
            if (m != null) {
                val num = m.groupValues[1].toIntOrNull() ?: 0
                if (num > maxSnackNum) {
                    maxSnackNum = num
                }
            }
        }
        return "加餐${maxSnackNum + 1}"
    }
}
