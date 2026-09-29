package com.example.data

enum class MedCategory(val label: String, val icon: String) {
    INSULIN("胰岛素", "💉"),
    ORAL("口服药", "💊"),
    GLP1("GLP-1/针剂", "💉")
}

/**
 * 用户个性化常用药物画像（自学习画像）
 */
data class UserMedProfile(
    val topInsulin: String? = null,
    val topBedtimeInsulin: String? = null,
    val topOralMed: String? = null,
    val topGLP1: String? = null,
    val medFrequencyMap: Map<String, Int> = emptyMap()
)

object MedicationData {
    // 常见口服抗糖药列表（严谨剥离皮下注射针剂，纯正口服降糖药物）
    val commonOralMeds = listOf(
        "口服药",
        "二甲双胍",
        "阿卡波糖",
        "达格列净",
        "恩格列净",
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

    // 常见 GLP-1 受体激动剂及 GIP/GLP-1 双受体针剂列表（肠促胰素类皮下注射注射笔）
    val commonGLP1Meds = listOf(
        "GLP-1/针剂",
        "司美格鲁肽",
        "替尔泊肽",
        "利拉鲁肽",
        "度拉糖肽",
        "艾塞那肽",
        "贝那鲁肽",
        "洛塞那肽"
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
        // 常见口服药知名商品名及别名
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

        // GLP-1 / GIP 双受体针剂知名商品名及别名
        "诺和泰" to "司美格鲁肽",
        "诺和盈" to "司美格鲁肽",
        "穆峰达" to "替尔泊肽",
        "诺和力" to "利拉鲁肽",
        "度易达" to "度拉糖肽",
        "百泌达" to "艾塞那肽",
        "谊生泰" to "贝那鲁肽",
        "孚来美" to "洛塞那肽",

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
     * 判断是否为 GLP-1 / GIP 受体激动剂类药物
     */
    fun isGLP1Med(name: String): Boolean {
        val n = name.trim()
        if (n == "GLP-1/针剂" || n == "GLP-1") return true
        for (glp in commonGLP1Meds) {
            if (glp != "GLP-1/针剂" && n.contains(glp)) return true
        }
        return n.contains("司美") || n.contains("格鲁肽") || n.contains("泊肽") ||
               n.contains("诺和泰") || n.contains("诺和盈") || n.contains("穆峰达") ||
               n.contains("度易达") || n.contains("诺和力") || n.contains("百泌达") ||
               n.contains("谊生泰") || n.contains("孚来美")
    }

    /**
     * 自动识别药物并返回匹配的用药单位
     * 胰岛素 -> U
     * GLP-1/针剂 -> mg (或 支/针/μg)
     * 口服药 -> 片 (常见口服降糖药统一使用“片”)
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
        if (cat == MedCategory.GLP1 || isGLP1Med(name)) {
            return when {
                name.contains("支") -> "支"
                name.contains("针") -> "针"
                name.contains("微克") || name.contains("ug", ignoreCase = true) || name.contains("μg") -> "μg"
                else -> "mg"
            }
        }
        if (name == "口服药" || name.contains("口服药")) {
            return when {
                name.contains("粒") -> "粒"
                name.contains("袋") -> "袋"
                else -> "片"
            }
        }
        return when {
            name.contains("粒") -> "粒"
            name.contains("袋") || name.contains("包") -> "袋"
            name.contains("支") -> "支"
            name.contains("针") -> "针"
            name.contains("mg", ignoreCase = true) || name.contains("毫克") -> "mg"
            cat == MedCategory.ORAL -> "片"
            else -> "片"
        }
    }

    /**
     * 智能推断药物类别（胰岛素 / 口服药 / GLP-1针剂）
     */
    fun inferCategory(medName: String): MedCategory {
        val name = medName.trim()
        if (name.isBlank()) return MedCategory.INSULIN
        if (name == "口服药" || name.contains("口服药")) {
            return MedCategory.ORAL
        }
        if (name == "GLP-1" || name.contains("GLP") || name.contains("针剂") || isGLP1Med(name)) {
            return MedCategory.GLP1
        }
        if (name.contains("胰岛素") || name.contains("门冬") || name.contains("甘精") ||
            name.contains("德谷") || name.contains("地特") || name.contains("赖脯") ||
            name.contains("谷赖") || name.contains("诺和锐") || name.contains("来得时") ||
            name.contains("诺和达") || name.contains("诺和平")) {
            return MedCategory.INSULIN
        }
        for (oral in commonOralMeds) {
            if (oral != "口服药" && name.contains(oral)) return MedCategory.ORAL
        }
        // 如果包含典型口服抗糖药特征
        if (name.contains("双胍") || name.contains("波糖") || name.contains("列净") ||
            name.contains("列汀") || name.contains("列脲") || name.contains("列奈") ||
            name.contains("列酮") || name.contains("片") || name.contains("胶囊")) {
            return MedCategory.ORAL
        }
        return MedCategory.INSULIN
    }

    /**
     * 判断文本是否包含降糖药物名称、商品名或专科词根（用于防止语义解析误将用药识别为食物）
     */
    fun isMedicationText(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return false
        if (medicationAliasMap.keys.any { trimmed.contains(it) }) return true
        if ((commonOralMeds + commonGLP1Meds + commonInsulinMeds).any { med ->
            med != "胰岛素" && med != "口服药" && med != "GLP-1/针剂" && trimmed.contains(med)
        }) return true
        val medRoots = listOf(
            "双胍", "波糖", "列净", "列汀", "列脲", "列奈", "列酮",
            "格鲁肽", "泊肽", "糖肽", "那肽", "胰岛素", "降糖药", "抗糖药",
            "口服药", "门冬", "甘精", "德谷", "地特", "赖脯", "谷赖",
            "诺和锐", "来得时", "优泌乐", "优泌林", "诺和平", "诺和达", "诺和灵",
            "司美", "替尔", "度拉", "利拉", "艾塞", "贝那", "洛塞",
            "吃药", "用药", "服药", "打针", "注射"
        )
        if (medRoots.any { trimmed.contains(it) }) return true
        if (trimmed.matches(Regex("""^(?:每次)?[\d\s.一两二三四五六七八九半]+(?:个)?(?:片|粒|袋|颗|支|针|单位|U|u|mg|毫克)$"""))) return true
        return false
    }

    /**
     * 统计全量历史记录中的药物使用频次
     */
    fun computeMedicationFrequencies(records: List<InsulinRecord>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        records.forEach { record ->
            if (record.bfInsulin != null && record.bfInsulin > 0f && record.bfMedName.isNotBlank()) {
                val name = record.bfMedName.trim()
                map[name] = (map[name] ?: 0) + 1
            }
            if (record.lunchInsulin != null && record.lunchInsulin > 0f && record.lunchMedName.isNotBlank()) {
                val name = record.lunchMedName.trim()
                map[name] = (map[name] ?: 0) + 1
            }
            if (record.dinnerInsulin != null && record.dinnerInsulin > 0f && record.dinnerMedName.isNotBlank()) {
                val name = record.dinnerMedName.trim()
                map[name] = (map[name] ?: 0) + 1
            }
            if (record.bedtimeInsulin != null && record.bedtimeInsulin > 0f && record.nightMedName.isNotBlank()) {
                val name = record.nightMedName.trim()
                map[name] = (map[name] ?: 0) + 1
            }
        }
        return map
    }

    /**
     * 根据用户历史用药记录自学习生成常用药特征画像
     */
    fun getUserMedProfile(records: List<InsulinRecord>): UserMedProfile {
        val freqMap = computeMedicationFrequencies(records)
        val mealtimeInsulins = mutableMapOf<String, Int>()
        val bedtimeInsulins = mutableMapOf<String, Int>()
        val oralMeds = mutableMapOf<String, Int>()
        val glp1Meds = mutableMapOf<String, Int>()

        records.forEach { r ->
            fun countMed(name: String, dose: Float?, isBedtime: Boolean) {
                if (dose != null && dose > 0f && name.isNotBlank()) {
                    val med = name.trim()
                    when (inferCategory(med)) {
                        MedCategory.INSULIN -> {
                            if (isBedtime) bedtimeInsulins[med] = (bedtimeInsulins[med] ?: 0) + 1
                            else mealtimeInsulins[med] = (mealtimeInsulins[med] ?: 0) + 1
                        }
                        MedCategory.ORAL -> oralMeds[med] = (oralMeds[med] ?: 0) + 1
                        MedCategory.GLP1 -> glp1Meds[med] = (glp1Meds[med] ?: 0) + 1
                    }
                }
            }
            countMed(r.bfMedName, r.bfInsulin, false)
            countMed(r.lunchMedName, r.lunchInsulin, false)
            countMed(r.dinnerMedName, r.dinnerInsulin, false)
            countMed(r.nightMedName, r.bedtimeInsulin, true)
        }

        val topInsulin = mealtimeInsulins.entries
            .filter { it.key != "胰岛素" }
            .maxByOrNull { it.value }?.key
            ?: mealtimeInsulins.maxByOrNull { it.value }?.key

        val topBedtime = bedtimeInsulins.entries
            .filter { it.key != "胰岛素" }
            .maxByOrNull { it.value }?.key
            ?: bedtimeInsulins.maxByOrNull { it.value }?.key
            ?: "甘精胰岛素"

        val topOral = oralMeds.entries
            .filter { it.key != "口服药" }
            .maxByOrNull { it.value }?.key
            ?: oralMeds.maxByOrNull { it.value }?.key

        val topGLP1 = glp1Meds.entries
            .filter { it.key != "GLP-1/针剂" && it.key != "GLP-1" }
            .maxByOrNull { it.value }?.key
            ?: glp1Meds.maxByOrNull { it.value }?.key

        return UserMedProfile(
            topInsulin = topInsulin,
            topBedtimeInsulin = topBedtime,
            topOralMed = topOral,
            topGLP1 = topGLP1,
            medFrequencyMap = freqMap
        )
    }

    /**
     * 获取按使用频次自学习排序的候选药物列表（高频药置顶，支持包含历史自填药品）
     */
    fun getMedicationOptions(
        category: MedCategory,
        records: List<InsulinRecord>,
        freqMap: Map<String, Int> = emptyMap()
    ): List<String> {
        val baseList = when (category) {
            MedCategory.INSULIN -> commonInsulinMeds
            MedCategory.ORAL -> commonOralMeds
            MedCategory.GLP1 -> commonGLP1Meds
        }
        val effectiveFreq = if (freqMap.isNotEmpty()) freqMap else computeMedicationFrequencies(records)
        val historyMeds = records.flatMap { r ->
            listOf(
                r.bfMedName.trim().takeIf { (r.bfInsulin ?: 0f) > 0f },
                r.lunchMedName.trim().takeIf { (r.lunchInsulin ?: 0f) > 0f },
                r.dinnerMedName.trim().takeIf { (r.dinnerInsulin ?: 0f) > 0f },
                r.nightMedName.trim().takeIf { (r.bedtimeInsulin ?: 0f) > 0f }
            ).filterNotNull()
        }.filter { it.isNotBlank() }
        .distinct()
        .filter { med -> inferCategory(med) == category && med !in baseList }

        val combined = (baseList + historyMeds).distinct()
        return combined.sortedWith(
            compareByDescending<String> { med ->
                effectiveFreq[med] ?: 0
            }.thenBy { med ->
                val idx = baseList.indexOf(med)
                if (idx >= 0) idx else 999
            }
        )
    }

    data class RecognitionResult(
        val matchedName: String?,
        val category: MedCategory,
        val suggestedDose: Float? = null,
        val suggestedTiming: String? = null,
        val detectedBG: Float? = null,
        val rawOcrText: String
    )

    /**
     * 从 OCR 识别的文本中智能匹配抗糖药物、用药剂量、时机及血糖测量值
     *
     * 架构协同：
     * 1. 与自然语言解析器对齐：通过数字归一化与临床三大类（胰岛素/口服药/GLP-1）专属规则提取合理剂量；
     * 2. 避免浓度误识：杜绝将“100单位/ml”或“1.34mg/ml”包装规格误当作给药剂量；
     * 3. 智能时机提取：从药盒说明书（随餐/餐前/睡前）提取服药时机；
     * 4. 血糖仪/化验单兜底支持：识别图片中出现的血糖数值。
     */
    fun matchMedicationFromOcr(ocrText: String): RecognitionResult {
        // 中文数字转换预处理（如“每次一片” -> “每次1片”，“每次两单位” -> “每次2单位”）
        var cleaned = ocrText.replace(" ", "")
        val cnMap = mapOf("一" to "1", "二" to "2", "两" to "2", "三" to "3", "四" to "4", "五" to "5", "六" to "6", "七" to "7", "八" to "8", "九" to "9", "半" to "0.5")
        for ((k, v) in cnMap) {
            cleaned = cleaned.replace("每次$k", "每次$v")
        }

        var matchedName: String? = null
        var category = MedCategory.ORAL

        // 1. 优先检查胰岛素
        for (insulin in commonInsulinMeds) {
            if (cleaned.contains(insulin) ||
                (insulin == "门冬胰岛素" && (cleaned.contains("门冬") || cleaned.contains("诺和锐") || cleaned.contains("novorapid", ignoreCase = true))) ||
                (insulin == "甘精胰岛素" && (cleaned.contains("甘精") || cleaned.contains("来得时") || cleaned.contains("lantus", ignoreCase = true) || cleaned.contains("长秀霖"))) ||
                (insulin == "德谷胰岛素" && (cleaned.contains("德谷") || cleaned.contains("诺和达") || cleaned.contains("tresiba", ignoreCase = true))) ||
                (insulin == "地特胰岛素" && (cleaned.contains("地特") || cleaned.contains("诺和平") || cleaned.contains("levemir", ignoreCase = true))) ||
                (insulin == "赖脯胰岛素" && (cleaned.contains("赖脯") || cleaned.contains("优泌乐") || cleaned.contains("humalog", ignoreCase = true))) ||
                (insulin == "谷赖胰岛素" && (cleaned.contains("谷赖") || cleaned.contains("艾倍得") || cleaned.contains("apidra", ignoreCase = true))) ||
                (insulin == "重组人胰岛素" && (cleaned.contains("重组人胰岛素") || cleaned.contains("人胰岛素") || cleaned.contains("优泌林") || cleaned.contains("诺和灵")))
            ) {
                matchedName = insulin
                category = MedCategory.INSULIN
                break
            }
        }

        // 2. 检查 GLP-1 / GIP 双受体针剂
        if (matchedName == null) {
            val glp1Mapping = listOf(
                "司美格鲁肽" to listOf("司美格鲁肽", "司美", "诺和泰", "诺和盈", "ozempic", "wegovy"),
                "替尔泊肽" to listOf("替尔泊肽", "穆峰达", "mounjaro", "zepbound"),
                "利拉鲁肽" to listOf("利拉鲁肽", "诺和力", "victoza", "saxenda"),
                "度拉糖肽" to listOf("度拉糖肽", "度易达", "trulicity"),
                "艾塞那肽" to listOf("艾塞那肽", "百泌达", "byetta"),
                "贝那鲁肽" to listOf("贝那鲁肽", "谊生泰"),
                "洛塞那肽" to listOf("聚乙二醇洛塞那肽", "洛塞那肽", "孚来美")
            )
            for ((canonical, aliases) in glp1Mapping) {
                if (aliases.any { cleaned.contains(it, ignoreCase = true) }) {
                    matchedName = canonical
                    category = MedCategory.GLP1
                    break
                }
            }
        }

        // 3. 检查口服药
        if (matchedName == null) {
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
                if (aliases.any { cleaned.contains(it, ignoreCase = true) }) {
                    matchedName = canonical
                    category = MedCategory.ORAL
                    break
                }
            }
        }

        // 4. 通用关键词特征兜底
        if (matchedName == null) {
            when {
                cleaned.contains("胰岛素") -> {
                    matchedName = "胰岛素"
                    category = MedCategory.INSULIN
                }
                cleaned.contains("针剂") || cleaned.contains("注射液") -> {
                    matchedName = "GLP-1/针剂"
                    category = MedCategory.GLP1
                }
                cleaned.contains("片") || cleaned.contains("胶囊") || cleaned.contains("口服") -> {
                    matchedName = "口服药"
                    category = MedCategory.ORAL
                }
            }
        }

        // 5. 临床级智能剂量提取
        var suggestedDose: Float? = null
        when (category) {
            MedCategory.INSULIN -> {
                // 优先查找 "每次 x 单位/U"
                val explicitDose = Regex("""每次\s*(\d+(?:\.\d+)?)\s*(?:个)?\s*(?:单位|U|u)""", RegexOption.IGNORE_CASE).find(cleaned)
                if (explicitDose != null) {
                    suggestedDose = explicitDose.groupValues[1].toFloatOrNull()
                } else {
                    // 查找非浓度、非总体积的单位数，且排除 "100单位/ml" 和 "300单位" 总体积
                    val unitMatches = Regex("""(?:打|注射|用)?\s*(\d{1,2}(?:\.\d+)?)\s*(?:个)?\s*(?:单位|U|u)(?!/|ml|毫升)""", RegexOption.IGNORE_CASE).findAll(cleaned)
                    for (m in unitMatches) {
                        val valNum = m.groupValues[1].toFloatOrNull()
                        if (valNum != null && valNum <= 80f) {
                            suggestedDose = valNum
                            break
                        }
                    }
                }
            }
            MedCategory.ORAL -> {
                // 口服药优先查找 "每次 x 片/粒/袋/颗"
                val explicitPills = Regex("""每次\s*(\d+(?:\.\d+)?)\s*(?:片|粒|袋|颗)""").find(cleaned)
                if (explicitPills != null) {
                    suggestedDose = explicitPills.groupValues[1].toFloatOrNull()
                } else {
                    val pillsMatch = Regex("""(?:吃了|口服|服用)?\s*(\d+(?:\.\d+)?)\s*(?:片|粒|袋|颗)(?!/|盒|板)""").find(cleaned)
                    if (pillsMatch != null) {
                        suggestedDose = pillsMatch.groupValues[1].toFloatOrNull()
                    } else {
                        // 若未出现片数，匹配单次毫克数如 "每次500mg"
                        val mgMatch = Regex("""每次\s*(\d+(?:\.\d+)?)\s*(?:毫克|mg)""", RegexOption.IGNORE_CASE).find(cleaned)
                        if (mgMatch != null) {
                            suggestedDose = mgMatch.groupValues[1].toFloatOrNull()
                        }
                    }
                }
            }
            MedCategory.GLP1 -> {
                // GLP-1 针剂通常为 mg 或 支/针
                val explicitMg = Regex("""每次\s*(\d+(?:\.\d+)?)\s*(?:毫克|mg|支|针)""", RegexOption.IGNORE_CASE).find(cleaned)
                if (explicitMg != null) {
                    suggestedDose = explicitMg.groupValues[1].toFloatOrNull()
                } else {
                    // 查找非浓度的 mg（避免 1.34mg/ml）
                    val mgMatches = Regex("""(?:注射|用|每次)?\s*(\d+(?:\.\d+)?)\s*(?:毫克|mg)(?!/|ml|毫升)""", RegexOption.IGNORE_CASE).findAll(cleaned)
                    for (m in mgMatches) {
                        val valNum = m.groupValues[1].toFloatOrNull()
                        if (valNum != null && valNum <= 20f) {
                            suggestedDose = valNum
                            break
                        }
                    }
                    if (suggestedDose == null) {
                        val stickMatch = Regex("""(\d+(?:\.\d+)?)\s*(?:支|针)""").find(cleaned)
                        if (stickMatch != null) {
                            suggestedDose = stickMatch.groupValues[1].toFloatOrNull()
                        }
                    }
                }
            }
        }

        // 6. 用药时机提取
        val suggestedTiming = when {
            cleaned.contains("随餐") || cleaned.contains("餐中") || cleaned.contains("吃饭时") || cleaned.contains("随第一口饭") -> "餐中"
            cleaned.contains("餐前") || cleaned.contains("饭前") -> "餐前"
            cleaned.contains("餐后") || cleaned.contains("饭后") -> "餐后"
            cleaned.contains("睡前") || cleaned.contains("夜间") || cleaned.contains("临睡") -> "睡前"
            else -> null
        }

        // 7. 血糖数值检测（化验单 / 血糖仪屏幕）
        var detectedBG: Float? = null
        val bgMatch = Regex("""(?:血糖|葡萄糖|Glu|GLU|FPG|PBG|BG)?\s*[:：]?\s*(\d{1,2}\.\d{1,2})\s*(?:mmol/L|mmol)?""", RegexOption.IGNORE_CASE).find(cleaned)
        if (bgMatch != null) {
            val valNum = bgMatch.groupValues[1].toFloatOrNull()
            if (valNum != null && valNum in 1.5f..33.3f) {
                detectedBG = valNum
            }
        }

        return RecognitionResult(
            matchedName = matchedName,
            category = category,
            suggestedDose = suggestedDose,
            suggestedTiming = suggestedTiming,
            detectedBG = detectedBG,
            rawOcrText = ocrText
        )
    }

    /**
     * 获取药物简写/展示名
     */
    fun getShortName(medName: String): String {
        val name = medName.trim()
        if (name.isBlank()) return ""
        if (name == "胰岛素") return "胰岛素"
        if (name == "口服药") return "口服药"
        if (name == "GLP-1/针剂" || name == "GLP-1") return "GLP-1"
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
            name.contains("司美") -> "司美"
            name.contains("替尔泊肽") || name.contains("穆峰达") -> "替尔泊肽"
            name.contains("利拉鲁肽") || name.contains("诺和力") -> "利拉鲁肽"
            name.contains("度拉糖肽") || name.contains("度易达") -> "度拉糖肽"
            name.length <= 4 -> name
            else -> name.take(4)
        }
    }

    /**
     * 生成表格用药列标题文本
     * 无论默认还是常规胰岛素/口服药/GLP-1，列标题统一规范为“用药”，彻底替换掉以往容易引起歧义的“胰岛素(U)”
     */
    fun getColumnHeaderTitle(medName: String? = null): String {
        val name = medName?.trim().orEmpty()
        if (name.isBlank() || name == "胰岛素" || name == "用药" || name == "口服药" || name == "GLP-1/针剂") {
            return "用药"
        }
        val short = getShortName(name)
        val unit = detectUnit(name)
        return if (short.isNotBlank() && short != "胰岛素" && short != "口服药" && short != "GLP-1/针剂") {
            "$short($unit)"
        } else {
            "用药"
        }
    }
}

/**
 * 运动文本解析数据结构与统一解析工具
 */
data class ParsedExercise(val name: String, val duration: String?, val unit: String)

fun parseExercise(text: String): ParsedExercise {
    var trimmed = text.trim()
    if (trimmed.isBlank()) return ParsedExercise(name = "", duration = null, unit = "")

    // 去除常见的口语前缀，如 "打了"、"做了"、"练了"、"去了"
    trimmed = trimmed.replace(Regex("""^(?:去|进行了|做了|完成了|打了|练了)\s*"""), "")

    // 模式 1: 倒装口语句式，如 "半小时八段锦", "半个小时八段锦", "40分钟太极拳", "30分钟散步"
    val invertedRegex = Regex("""^(\d+(?:\.\d+)?\s*(?:分钟|分种|分|min|小时|h)|(?:一个半|1个半|大半|半)个?(?:多)?(?:小时|钟头|钟)?|(?:一|两|二|三|四|1|2|3|4)个?(?:半小时|小时|钟头|钟))\s*(?:的)?\s*(.+)$""")
    val invMatch = invertedRegex.find(trimmed)
    if (invMatch != null) {
        val durPart = invMatch.groupValues[1].trim()
        val namePart = invMatch.groupValues[2].trim()
        val (normDur, normUnit) = normalizeDuration(durPart)
        if (normDur.isNotBlank() && namePart.isNotBlank()) {
            return ParsedExercise(name = namePart, duration = normDur, unit = normUnit)
        }
    }

    // 模式 2: 中文口语时长后缀，如 "散步半个小时", "八段锦半小时", "散步一个半小时", "散步一小时", "散步两小时", "散步差不多半小时"
    val cnDurRegex = Regex("""^(.*?)\s*(?:了)?\s*(?:约|大约|大概|差不多|将近|快|有)?\s*((?:一个半|1个半|大半|半)个?(?:多)?(?:小时|钟头|钟)?|(?:一|两|二|三|四|1|2|3|4)个?(?:半小时|小时|钟头|钟)|(?:半|一|两|1|2|3|4)个?多小时|\d+\s*多分钟|\d+\s*来分钟)$""")
    val cnMatch = cnDurRegex.find(trimmed)
    if (cnMatch != null) {
        val name = cnMatch.groupValues[1].trim()
        val durText = cnMatch.groupValues[2].trim()
        val (normDur, normUnit) = normalizeDuration(durText)
        return ParsedExercise(name = name.ifBlank { "运动" }, duration = normDur, unit = normUnit)
    }

    // 模式 3: 标准格式，如 "八段锦 30分钟", "散步 40 分钟", "慢跑 1.5小时"
    val standardRegex = Regex("""^(.*?)\s*(\d+(?:\.\d+)?)\s*(分钟|分种|分|min|小时|h)?$""")
    val match = standardRegex.find(trimmed)
    if (match != null) {
        val name = match.groupValues[1].trim()
        val duration = match.groupValues[2].trim()
        val rawUnit = match.groupValues[3].trim()
        val unit = if (rawUnit.isBlank() || rawUnit == "分" || rawUnit == "分种") "分钟" else rawUnit
        return ParsedExercise(name = name.ifBlank { "运动" }, duration = duration, unit = unit)
    }

    return ParsedExercise(name = trimmed, duration = null, unit = "")
}

private fun normalizeDuration(durText: String): Pair<String, String> {
    val s = durText.trim()
    return when {
        s.contains("一个半") || s.contains("1个半") || s.contains("1.5小时") || s.contains("1.5h") -> "90" to "分钟"
        s.contains("半") -> "30" to "分钟"
        s.contains("两") || s.contains("二") || s.contains("2小时") || s.contains("2个") -> "120" to "分钟"
        s.contains("一小时") || s.contains("1小时") || s.contains("一个") || s.contains("1h") -> "60" to "分钟"
        s.contains("三") || s.contains("3小时") || s.contains("3个") -> "180" to "分钟"
        else -> {
            val m = Regex("""^(\d+(?:\.\d+)?)\s*(分钟|分种|分|min|小时|h)?$""").find(s)
            if (m != null) {
                val num = m.groupValues[1]
                val rawU = m.groupValues[2]
                val u = if (rawU.isBlank() || rawU == "分" || rawU == "分种") "分钟" else rawU
                num to u
            } else {
                durText to ""
            }
        }
    }
}
