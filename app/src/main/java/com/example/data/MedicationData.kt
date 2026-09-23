package com.example.data

enum class MedCategory(val label: String, val icon: String) {
    INSULIN("胰岛素", "💉"),
    ORAL("口服药", "💊")
}

object MedicationData {
    // 常见口服抗糖药及GLP-1受体激动剂列表（增加通用“口服药”选项）
    val commonOralMeds = listOf(
        "口服药",
        "二甲双胍",
        "司美格鲁肽",
        "替尔泊肽",
        "阿卡波糖",
        "达格列净",
        "恩格列净",
        "利拉鲁肽",
        "度拉糖肽",
        "西格列汀",
        "利格列汀",
        "格列美脲",
        "瑞格列奈",
        "吡格列酮",
        "伏格列波糖",
        "维格列汀",
        "卡格列净",
        "沙格列汀",
        "那格列奈",
        "格列齐特",
        "格列吡嗪"
    )

    // 常见胰岛素列表（默认“胰岛素”通用选项）
    val commonInsulinMeds = listOf(
        "胰岛素",
        "门冬胰岛素",
        "甘精胰岛素",
        "德谷胰岛素",
        "地特胰岛素",
        "赖脯胰岛素",
        "谷赖胰岛素",
        "德谷门冬双胰岛素",
        "精蛋白生物合成人胰岛素",
        "门冬胰岛素30"
    )

    // 常见降糖药物商品名与口语别名对照映射表
    val medicationAliasMap = mapOf(
        // 常见口服药 / GLP-1 知名商品名及别名
        "格华止" to "二甲双胍",
        "美福明" to "二甲双胍",
        "卜可" to "二甲双胍",
        "拜唐苹" to "阿卡波糖",
        "拜糖苹" to "阿卡波糖",
        "卡博平" to "阿卡波糖",
        "安达唐" to "达格列净",
        "欧唐静" to "恩格列净",
        "捷诺维" to "西格列汀",
        "欧唐宁" to "利格列汀",
        "亚莫利" to "格列美脲",
        "万苏平" to "格列美脲",
        "诺和龙" to "瑞格列奈",
        "孚来迪" to "瑞格列奈",
        "达美康" to "格列齐特",
        "倍欣" to "伏格列波糖",
        "佳维乐" to "维格列汀",
        "怡可安" to "卡格列净",
        "安立泽" to "沙格列汀",
        "唐力" to "那格列奈",
        "艾可拓" to "吡格列酮",
        "美吡达" to "格列吡嗪",
        "瑞易宁" to "格列吡嗪",
        "诺和泰" to "司美格鲁肽",
        "诺和盈" to "司美格鲁肽",
        "诺和力" to "利拉鲁肽",
        "度易达" to "度拉糖肽",
        "穆峰达" to "替尔泊肽",

        // 常见胰岛素知名商品名及别名
        "诺和锐" to "门冬胰岛素",
        "诺和锐30" to "门冬胰岛素30",
        "来得时" to "甘精胰岛素",
        "长秀霖" to "甘精胰岛素",
        "优泌乐" to "赖脯胰岛素",
        "优泌乐25" to "赖脯胰岛素",
        "优泌林" to "精蛋白生物合成人胰岛素",
        "优泌林70/30" to "精蛋白生物合成人胰岛素",
        "诺和平" to "地特胰岛素",
        "诺和达" to "德谷胰岛素",
        "诺和灵" to "精蛋白生物合成人胰岛素",
        "诺和灵30R" to "精蛋白生物合成人胰岛素",
        "诺和灵50R" to "精蛋白生物合成人胰岛素",
        "艾倍得" to "谷赖胰岛素"
    )

    /**
     * 自动识别药物并返回匹配的用药单位
     * 胰岛素 -> U
     * 口服药 -> 片 (或 mg/袋 等)
     */
    fun detectUnit(medName: String, category: MedCategory? = null): String {
        val name = medName.trim()
        val cat = category ?: inferCategory(name)
        if (cat == MedCategory.INSULIN || name.contains("胰岛素") || name.contains("门冬") ||
            name.contains("甘精") || name.contains("德谷") || name.contains("地特") ||
            name.contains("赖脯") || name.contains("谷赖") || name.contains("诺和锐") ||
            name.contains("来得时")) {
            return "U"
        }
        if (name == "口服药" || name.contains("口服药")) {
            return when {
                name.contains("mg", ignoreCase = true) -> "mg"
                name.contains("粒") -> "粒"
                name.contains("袋") -> "袋"
                else -> "片"
            }
        }
        return when {
            name.contains("片") -> "片"
            name.contains("粒") -> "粒"
            name.contains("袋") || name.contains("包") -> "袋"
            cat == MedCategory.ORAL -> "mg"
            else -> "U"
        }
    }

    /**
     * 智能推断药物类别（胰岛素 or 口服药）
     */
    fun inferCategory(medName: String): MedCategory {
        val name = medName.trim()
        if (name == "口服药" || name.contains("口服药")) {
            return MedCategory.ORAL
        }
        if (name.contains("胰岛素") || name.contains("门冬") || name.contains("甘精") ||
            name.contains("德谷") || name.contains("地特") || name.contains("赖脯") ||
            name.contains("谷赖") || name.contains("诺和锐") || name.contains("来得时") ||
            name.contains("诺和达") || name.contains("诺和平")) {
            return MedCategory.INSULIN
        }
        for (oral in commonOralMeds) {
            if (name.contains(oral)) return MedCategory.ORAL
        }
        // 如果包含典型口服抗糖药特征
        if (name.contains("双胍") || name.contains("波糖") || name.contains("列净") ||
            name.contains("列汀") || name.contains("列脲") || name.contains("列奈") ||
            name.contains("列酮") || name.contains("片") || name.contains("胶囊")) {
            return MedCategory.ORAL
        }
        return MedCategory.INSULIN
    }

    data class RecognitionResult(
        val matchedName: String?,
        val category: MedCategory,
        val suggestedDose: Float? = null,
        val rawOcrText: String
    )

    /**
     * 从 OCR 识别的文本中智能匹配抗糖药物
     */
    fun matchMedicationFromOcr(ocrText: String): RecognitionResult {
        val cleaned = ocrText.replace("\\s+".toRegex(), "")

        // 1. 优先检查胰岛素
        for (insulin in commonInsulinMeds) {
            if (cleaned.contains(insulin) ||
                (insulin == "门冬胰岛素" && (cleaned.contains("门冬") || cleaned.contains("诺和锐"))) ||
                (insulin == "甘精胰岛素" && (cleaned.contains("甘精") || cleaned.contains("来得时") || cleaned.contains("长秀霖"))) ||
                (insulin == "德谷胰岛素" && (cleaned.contains("德谷") || cleaned.contains("诺和达"))) ||
                (insulin == "地特胰岛素" && (cleaned.contains("地特") || cleaned.contains("诺和平"))) ||
                (insulin == "赖脯胰岛素" && (cleaned.contains("赖脯") || cleaned.contains("优泌乐"))) ||
                (insulin == "谷赖胰岛素" && (cleaned.contains("谷赖") || cleaned.contains("艾倍得")))
            ) {
                return RecognitionResult(
                    matchedName = insulin,
                    category = MedCategory.INSULIN,
                    suggestedDose = extractDose(cleaned),
                    rawOcrText = ocrText
                )
            }
        }

        // 2. 检查口服药
        val oralMapping = listOf(
            "二甲双胍" to listOf("二甲双胍", "盐酸二甲双胍", "格华止", "卜可", "美福明"),
            "阿卡波糖" to listOf("阿卡波糖", "拜唐苹", "卡博平"),
            "达格列净" to listOf("达格列净", "安达唐", "forxiga"),
            "恩格列净" to listOf("恩格列净", "欧唐静", "jardiance"),
            "西格列汀" to listOf("西格列汀", "捷诺维", "januvia"),
            "利格列汀" to listOf("利格列汀", "欧唐宁", "trajenta"),
            "格列美脲" to listOf("格列美脲", "亚莫利", "万苏平", "迪北"),
            "瑞格列奈" to listOf("瑞格列奈", "诺和龙", "孚来迪"),
            "吡格列酮" to listOf("吡格列酮", "盐酸吡格列酮", "艾汀", "卡双平"),
            "伏格列波糖" to listOf("伏格列波糖", "倍欣"),
            "维格列汀" to listOf("维格列汀", "佳维乐"),
            "卡格列净" to listOf("卡格列净", "怡可安"),
            "沙格列汀" to listOf("沙格列汀", "安立泽"),
            "那格列奈" to listOf("那格列奈", "唐力"),
            "格列齐特" to listOf("格列齐特", "达美康"),
            "格列吡嗪" to listOf("格列吡嗪", "美吡达", "瑞易宁")
        )

        for ((canonical, aliases) in oralMapping) {
            for (alias in aliases) {
                if (cleaned.contains(alias, ignoreCase = true)) {
                    return RecognitionResult(
                        matchedName = canonical,
                        category = MedCategory.ORAL,
                        suggestedDose = extractDose(cleaned),
                        rawOcrText = ocrText
                    )
                }
            }
        }

        // 未在列表内匹配到药物，保留原始识别线索，交由用户自己填写
        return RecognitionResult(
            matchedName = null,
            category = MedCategory.ORAL,
            suggestedDose = extractDose(cleaned),
            rawOcrText = ocrText
        )
    }

    private fun extractDose(text: String): Float? {
        val mgRegex = "([0-9]+(?:\\.[0-9]+)?)\\s*mg".toRegex(RegexOption.IGNORE_CASE)
        mgRegex.find(text)?.let {
            return it.groupValues[1].toFloatOrNull()
        }
        val gRegex = "([0-9]+(?:\\.[0-9]+)?)\\s*g".toRegex(RegexOption.IGNORE_CASE)
        gRegex.find(text)?.let {
            val gVal = it.groupValues[1].toFloatOrNull()
            if (gVal != null) {
                return if (gVal < 5f) gVal * 1000f else gVal
            }
        }
        return null
    }

    /**
     * 获取药物简写/展示名
     */
    fun getShortName(medName: String): String {
        val name = medName.trim()
        if (name.isBlank()) return ""
        if (name == "胰岛素") return "胰岛素"
        if (name == "口服药") return "口服药"
        return when {
            name.contains("门冬") -> "门冬"
            name.contains("甘精") -> "甘精"
            name.contains("德谷") -> "德谷"
            name.contains("地特") -> "地特"
            name.contains("赖脯") -> "赖脯"
            name.contains("谷赖") -> "谷赖"
            name.contains("双胍") -> "二甲双胍"
            name.contains("波糖") -> "阿卡波糖"
            name.contains("达格列净") -> "达格列净"
            name.contains("恩格列净") -> "恩格列净"
            name.contains("西格列汀") -> "西格列汀"
            name.contains("利格列汀") -> "利格列汀"
            name.length <= 4 -> name
            else -> name.take(4)
        }
    }

    /**
     * 生成表格用药列标题文本
     * 无论默认还是常规胰岛素/口服药，列标题统一规范为“用药”，彻底替换掉以往容易引起歧义的“胰岛素(U)”
     */
    fun getColumnHeaderTitle(medName: String? = null): String {
        val name = medName?.trim().orEmpty()
        if (name.isBlank() || name == "胰岛素" || name == "用药" || name == "口服药") {
            return "用药"
        }
        val short = getShortName(name)
        val unit = detectUnit(name)
        return if (short.isNotBlank() && short != "胰岛素" && short != "口服药") {
            "$short($unit)"
        } else {
            "用药"
        }
    }
}
