package com.example.ui

/**
 * 关怀模式字号大小配置：
 * - STANDARD (标准大字): 基础适老倍率 (1.0x)，清晰整洁
 * - EXTRA (特大字): 推荐字号 (1.18x)，舒适护眼防老花
 * - SUPER (超大字): 极致特大字 (1.38x)，数字与文字超大呈现，适合高龄或视力较弱长辈
 */
enum class CareFontSize(
    val title: String,
    val shortLabel: String,
    val scaleFactor: Float,
    val desc: String
) {
    STANDARD("标准大字", "大字", 1.0f, "适合轻度老花，兼顾紧凑与清晰"),
    EXTRA("特大字", "特大", 1.18f, "推荐字号，大字易读防视疲劳"),
    SUPER("超大字", "超大", 1.38f, "极致特大字号，长辈看字毫不费力");

    fun next(): CareFontSize = when (this) {
        STANDARD -> EXTRA
        EXTRA -> SUPER
        SUPER -> STANDARD
    }
}
