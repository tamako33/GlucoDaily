package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = compositionLocalOf { false }

// =========================================================================
// Apple Design System Tokens (per VoltAgent/awesome-design-md: apple/DESIGN.md)
// =========================================================================

// Original Medical Teal Palette
val TealPrimary = Color(0xFF0D9488)
val TealPrimaryDark = Color(0xFF0F766E)
val TealPrimaryLight = Color(0xFF14B8A6)
val TealContainer = Color(0xFFCCFBF1)
val TealOnContainer = Color(0xFF115E59)

val AppleActionBlue = TealPrimary
val AppleActionBlueFocus = TealPrimaryDark
val AppleActionBlueDark = TealPrimaryLight
val AppleActionBlueContainer = TealContainer
val AppleActionBlueOnContainer = TealOnContainer

// Surface & Canvas: Signature Apple Parchment (#f5f5f7) canvas with Pure White (#ffffff) cards
val AppleParchment = Color(0xFFF5F5F7)
val AppleCanvas = Color(0xFFFFFFFF)
val AppleSurfacePearl = Color(0xFFFAFAFC)
val AppleSurfaceChip = Color(0xFFE5E5EA)

val MedicalBackground = AppleParchment
val MedicalSurface = AppleCanvas
val MedicalSurfaceVariant = AppleSurfaceChip

// Dark Mode Canvas: Apple System Grouped Dark Palette
val AppleDarkCanvas = Color(0xFF000000)        // Pure Black
val AppleDarkTile1 = Color(0xFF1C1C1E)         // Surface Tile 1 (Card background)
val AppleDarkTile2 = Color(0xFF2C2C2E)         // Surface Tile 2 (Inner controls / chips)
val AppleDarkTile3 = Color(0xFF3A3A3C)         // Surface Tile 3 (Borders / hairlines)

val DarkBackground = AppleDarkCanvas
val DarkSurface = AppleDarkTile1
val DarkSurfaceVariant = AppleDarkTile2
val DarkOutline = AppleDarkTile3
val DarkOutlineVariant = AppleDarkTile3

// Typography Ink Colors: Apple Near-Black Ink (#1d1d1f) and Muted Secondary (#86868b)
val AppleInk = Color(0xFF1D1D1F)
val AppleInkMuted80 = Color(0xFF333333)
val AppleInkMuted48 = Color(0xFF86868B)
val AppleInkLight = Color(0xFFF5F5F7)

// Hairlines & Dividers: 1px hairline border (no heavy shadows on chrome)
val AppleHairline = Color(0xFFE5E5EA)
val AppleDividerSoft = Color(0xFFF0F0F0)
val AppleDarkHairline = Color(0xFF38383A)

// Table Header Period Backgrounds (Light mode - distinct, clear color separation)
val BreakfastHeaderBg = Color(0xFFCCFBF1)   // Distinct Mint Teal tint (100)
val LunchHeaderBg = Color(0xFFFEF08A)       // Distinct Sunny Amber/Yellow tint (200)
val DinnerHeaderBg = Color(0xFFBAE6FD)      // Distinct Sky Blue tint (200)
val BedtimeHeaderBg = Color(0xFFE0E7FF)     // Distinct Lavender/Indigo tint (200)

val BreakfastHeaderBgDark = Color(0xFF133633) // Distinct Dark Teal tint
val LunchHeaderBgDark = Color(0xFF38290E)     // Distinct Dark Amber tint
val DinnerHeaderBgDark = Color(0xFF102D42)    // Distinct Dark Blue tint
val BedtimeHeaderBgDark = Color(0xFF231F45)   // Distinct Dark Lavender tint

// Meal Period Accents (Light) - 四时段丰富专属色彩时间轴体系
val BreakfastColor = Color(0xFF0D9488)        // Medical Teal (晨间沉稳青绿)
val BreakfastBg = AppleCanvas
val BreakfastBorder = AppleHairline
val BreakfastColumnBg = Color(0xFFF0FDF9)    // Mint teal column background

val LunchColor = Color(0xFFD97706)            // Warm Amber (午间阳光暖橙)
val LunchBg = AppleCanvas
val LunchBorder = AppleHairline
val LunchColumnBg = Color(0xFFFFFBEB)        // Amber column background

val DinnerColor = Color(0xFF0284C7)           // Ocean Blue (傍晚澄澈海蓝)
val DinnerBg = AppleCanvas
val DinnerBorder = AppleHairline
val DinnerColumnBg = Color(0xFFF0F9FF)       // Light blue column background

val BedtimeColor = Color(0xFF6366F1)          // Deep Indigo (睡前静谧紫罗兰)
val BedtimeBg = AppleCanvas
val BedtimeBorder = AppleHairline
val BedtimeColumnBg = Color(0xFFF5F7FF)      // Lavender column background

// Meal Period Accents (Dark)
val BreakfastBgDark = AppleDarkTile1
val BreakfastColumnBgDark = Color.Transparent

val LunchBgDark = AppleDarkTile1
val LunchColumnBgDark = Color.Transparent

val DinnerBgDark = AppleDarkTile1
val DinnerColumnBgDark = Color.Transparent

val BedtimeBgDark = AppleDarkTile1
val BedtimeColumnBgDark = Color.Transparent

val BreakfastColorDark = Color(0xFF2DD4BF)
val LunchColorDark = Color(0xFFFBBF24)
val DinnerColorDark = Color(0xFF38BDF8)
val BedtimeColorDark = Color(0xFF818CF8)

// Blood Glucose Status Indicators (Light) - 纯正医用级色彩，温和克制且绝非深棕暗红
val GlucoseNormal = Color(0xFF059669)        // 翡翠绿 (#059669)，与主题协调统一
val GlucoseNormalBg = Color(0xFFECFDF5)

val GlucoseLow = Color(0xFFDC2626)           // 纯正警示红 (清晰醒目，非暗沉深红)
val GlucoseLowBg = Color(0xFFFEF2F2)

val GlucoseHigh = Color(0xFFEA580C)          // 温暖纯正橙 (清晰醒目，绝非暗褐棕色)
val GlucoseHighBg = Color(0xFFFFF7ED)

// Blood Glucose Status Indicators (Dark)
val GlucoseNormalDark = Color(0xFF34D399)
val GlucoseNormalBgDark = Color(0xFF064E3B).copy(alpha = 0.35f)

val GlucoseLowDark = Color(0xFFF87171)
val GlucoseLowBgDark = Color(0xFF7F1D1D).copy(alpha = 0.35f)

val GlucoseHighDark = Color(0xFFFB923C)
val GlucoseHighBgDark = Color(0xFF7C2D12).copy(alpha = 0.35f)

/**
 * AppThemeColors dynamically resolves colors according to current active theme (Light or Dark).
 */
object AppThemeColors {
    val isDark: Boolean
        @Composable get() = LocalIsDarkTheme.current

    @Composable
    private fun anim(target: Color): Color =
        animateColorAsState(target, tween(durationMillis = 300), label = "app_theme_color").value

    val actionBlue: Color
        @Composable get() = anim(if (isDark) AppleActionBlueDark else AppleActionBlue)

    val background: Color
        @Composable get() = anim(if (isDark) DarkBackground else MedicalBackground)

    val surface: Color
        @Composable get() = anim(if (isDark) DarkSurface else MedicalSurface)

    val surfaceVariant: Color
        @Composable get() = anim(if (isDark) DarkSurfaceVariant else MedicalSurfaceVariant)

    val hairline: Color
        @Composable get() = anim(if (isDark) AppleDarkHairline else AppleHairline)

    val inkPrimary: Color
        @Composable get() = anim(if (isDark) Color.White else AppleInk)

    val inkSecondary: Color
        @Composable get() = anim(if (isDark) Color(0xFF8E8E93) else AppleInkMuted48)

    val breakfastBg: Color
        @Composable get() = anim(if (isDark) BreakfastBgDark else BreakfastBg)

    val breakfastHeaderBg: Color
        @Composable get() = anim(if (isDark) BreakfastHeaderBgDark else BreakfastHeaderBg)

    val breakfastColumnBg: Color
        @Composable get() = anim(if (isDark) BreakfastColumnBgDark else BreakfastColumnBg)

    val breakfastColor: Color
        @Composable get() = anim(if (isDark) BreakfastColorDark else BreakfastColor)

    val breakfastBorder: Color
        @Composable get() = anim(if (isDark) AppleDarkHairline else AppleHairline)

    val lunchBg: Color
        @Composable get() = anim(if (isDark) LunchBgDark else LunchBg)

    val lunchHeaderBg: Color
        @Composable get() = anim(if (isDark) LunchHeaderBgDark else LunchHeaderBg)

    val lunchColumnBg: Color
        @Composable get() = anim(if (isDark) LunchColumnBgDark else LunchColumnBg)

    val lunchColor: Color
        @Composable get() = anim(if (isDark) LunchColorDark else LunchColor)

    val lunchBorder: Color
        @Composable get() = anim(if (isDark) AppleDarkHairline else AppleHairline)

    val dinnerBg: Color
        @Composable get() = anim(if (isDark) DinnerBgDark else DinnerBg)

    val dinnerHeaderBg: Color
        @Composable get() = anim(if (isDark) DinnerHeaderBgDark else DinnerHeaderBg)

    val dinnerColumnBg: Color
        @Composable get() = anim(if (isDark) DinnerColumnBgDark else DinnerColumnBg)

    val dinnerColor: Color
        @Composable get() = anim(if (isDark) DinnerColorDark else DinnerColor)

    val dinnerBorder: Color
        @Composable get() = anim(if (isDark) AppleDarkHairline else AppleHairline)

    val bedtimeBg: Color
        @Composable get() = anim(if (isDark) BedtimeBgDark else BedtimeBg)

    val bedtimeHeaderBg: Color
        @Composable get() = anim(if (isDark) BedtimeHeaderBgDark else BedtimeHeaderBg)

    val bedtimeColumnBg: Color
        @Composable get() = anim(if (isDark) BedtimeColumnBgDark else BedtimeColumnBg)

    val bedtimeColor: Color
        @Composable get() = anim(if (isDark) BedtimeColorDark else BedtimeColor)

    val bedtimeBorder: Color
        @Composable get() = anim(if (isDark) AppleDarkHairline else AppleHairline)

    val glucoseNormal: Color
        @Composable get() = anim(if (isDark) GlucoseNormalDark else GlucoseNormal)

    val glucoseNormalBg: Color
        @Composable get() = anim(if (isDark) GlucoseNormalBgDark else GlucoseNormalBg)

    val glucoseLow: Color
        @Composable get() = anim(if (isDark) GlucoseLowDark else GlucoseLow)

    val glucoseLowBg: Color
        @Composable get() = anim(if (isDark) GlucoseLowBgDark else GlucoseLowBg)

    val glucoseHigh: Color
        @Composable get() = anim(if (isDark) GlucoseHighDark else GlucoseHigh)

    val glucoseHighBg: Color
        @Composable get() = anim(if (isDark) GlucoseHighBgDark else GlucoseHighBg)

    val rowCardBg: Color
        @Composable get() = anim(if (isDark) AppleDarkTile1 else AppleCanvas)

    val rowCardBorder: Color
        @Composable get() = anim(if (isDark) AppleDarkHairline else AppleHairline)
}
