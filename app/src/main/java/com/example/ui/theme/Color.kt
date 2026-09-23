package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = compositionLocalOf { false }

// Medical Teal Palette
val TealPrimary = Color(0xFF0D9488)
val TealPrimaryDark = Color(0xFF0F766E)
val TealPrimaryLight = Color(0xFF14B8A6)
val TealContainer = Color(0xFFCCFBF1)
val TealOnContainer = Color(0xFF115E59)

// Light theme canvas
val MedicalBackground = Color(0xFFF8FAFC)
val MedicalSurface = Color(0xFFFFFFFF)
val MedicalSurfaceVariant = Color(0xFFF1F5F9)

// Dark theme canvas
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)
val DarkOutline = Color(0xFF475569)
val DarkOutlineVariant = Color(0xFF334155)

// Table Header Period Backgrounds (Light mode - distinct, clear color separation)
val BreakfastHeaderBg = Color(0xFFCCFBF1)   // Distinct Mint Teal tint (100)
val LunchHeaderBg = Color(0xFFFEF08A)       // Distinct Sunny Amber/Yellow tint (200)
val DinnerHeaderBg = Color(0xFFBAE6FD)      // Distinct Sky Blue tint (200)
val BedtimeHeaderBg = Color(0xFFE0E7FF)     // Distinct Lavender/Indigo tint (200)

val BreakfastHeaderBgDark = Color(0xFF133633) // Distinct Dark Teal tint
val LunchHeaderBgDark = Color(0xFF38290E)     // Distinct Dark Amber tint
val DinnerHeaderBgDark = Color(0xFF102D42)    // Distinct Dark Blue tint
val BedtimeHeaderBgDark = Color(0xFF231F45)   // Distinct Dark Lavender tint

// Meal Period Accents (Light)
val BreakfastColor = Color(0xFF0D9488)
val BreakfastBg = Color(0xFFF8FAFC)          // Clean neutral slate tint for card
val BreakfastBorder = Color(0xFFE2E8F0)      // Subtle crisp border
val BreakfastColumnBg = Color(0xFFF0FDF9)    // Light mint-teal column background for table distinction

val LunchColor = Color(0xFFD97706)
val LunchBg = Color(0xFFF8FAFC)              // Clean neutral slate tint for card
val LunchBorder = Color(0xFFE2E8F0)          // Subtle crisp border
val LunchColumnBg = Color(0xFFFFFBEB)        // Light amber column background for table distinction

val DinnerColor = Color(0xFF0284C7)
val DinnerBg = Color(0xFFF8FAFC)             // Clean neutral slate tint for card
val DinnerBorder = Color(0xFFE2E8F0)         // Subtle crisp border
val DinnerColumnBg = Color(0xFFF0F9FF)       // Light blue column background for table distinction

val BedtimeColor = Color(0xFF6366F1)
val BedtimeBg = Color(0xFFF8FAFC)            // Clean neutral slate tint for card
val BedtimeBorder = Color(0xFFE2E8F0)        // Subtle crisp border
val BedtimeColumnBg = Color(0xFFF5F7FF)      // Light lavender column background for table distinction

// Meal Period Accents (Dark) - Harmonious, clean, elegant dark tones
val BreakfastBgDark = Color(0xFF132227)       // Subtle dark teal tint
val BreakfastColumnBgDark = Color.Transparent // Clean, no dark striping under table columns

val LunchBgDark = Color(0xFF272115)           // Subtle dark amber tint
val LunchColumnBgDark = Color.Transparent     // Clean, no dark striping under table columns

val DinnerBgDark = Color(0xFF14202E)          // Subtle dark ocean blue tint
val DinnerColumnBgDark = Color.Transparent    // Clean, no dark striping under table columns

val BedtimeBgDark = Color(0xFF1E1D2D)         // Subtle dark indigo tint
val BedtimeColumnBgDark = Color.Transparent   // Clean, no dark striping under table columns

val BreakfastColorDark = Color(0xFF2DD4BF)
val LunchColorDark = Color(0xFFFBBF24)
val DinnerColorDark = Color(0xFF38BDF8)
val BedtimeColorDark = Color(0xFF818CF8)

// Blood Glucose Status Indicators (Light)
val GlucoseNormal = Color(0xFF059669)
val GlucoseNormalBg = Color(0xFFECFDF5)

val GlucoseLow = Color(0xFFDC2626)
val GlucoseLowBg = Color(0xFFFEF2F2)

val GlucoseHigh = Color(0xFFD97706)
val GlucoseHighBg = Color(0xFFFFFBEB)

// Blood Glucose Status Indicators (Dark) - Clean & Harmonious
val GlucoseNormalDark = Color(0xFF34D399)
val GlucoseNormalBgDark = Color(0xFF064E3B).copy(alpha = 0.35f)

val GlucoseLowDark = Color(0xFFF87171)
val GlucoseLowBgDark = Color(0xFF7F1D1D).copy(alpha = 0.35f)

val GlucoseHighDark = Color(0xFFFBBF24)
val GlucoseHighBgDark = Color(0xFF78350F).copy(alpha = 0.35f)

/**
 * AppThemeColors dynamically resolves colors according to current active theme (Light or Dark).
 */
object AppThemeColors {
    val isDark: Boolean
        @Composable get() = LocalIsDarkTheme.current

    @Composable
    private fun anim(target: Color): Color =
        animateColorAsState(target, tween(durationMillis = 350), label = "app_theme_color").value

    val breakfastBg: Color
        @Composable get() = anim(if (isDark) BreakfastBgDark else BreakfastBg)

    val breakfastHeaderBg: Color
        @Composable get() = anim(if (isDark) BreakfastHeaderBgDark else BreakfastHeaderBg)

    val breakfastColumnBg: Color
        @Composable get() = anim(if (isDark) BreakfastColumnBgDark else BreakfastColumnBg)

    val breakfastColor: Color
        @Composable get() = anim(if (isDark) BreakfastColorDark else BreakfastColor)

    val breakfastBorder: Color
        @Composable get() = anim(if (isDark) BreakfastColorDark.copy(alpha = 0.3f) else BreakfastColor.copy(alpha = 0.22f))

    val lunchBg: Color
        @Composable get() = anim(if (isDark) LunchBgDark else LunchBg)

    val lunchHeaderBg: Color
        @Composable get() = anim(if (isDark) LunchHeaderBgDark else LunchHeaderBg)

    val lunchColumnBg: Color
        @Composable get() = anim(if (isDark) LunchColumnBgDark else LunchColumnBg)

    val lunchColor: Color
        @Composable get() = anim(if (isDark) LunchColorDark else LunchColor)

    val lunchBorder: Color
        @Composable get() = anim(if (isDark) LunchColorDark.copy(alpha = 0.3f) else LunchColor.copy(alpha = 0.22f))

    val dinnerBg: Color
        @Composable get() = anim(if (isDark) DinnerBgDark else DinnerBg)

    val dinnerHeaderBg: Color
        @Composable get() = anim(if (isDark) DinnerHeaderBgDark else DinnerHeaderBg)

    val dinnerColumnBg: Color
        @Composable get() = anim(if (isDark) DinnerColumnBgDark else DinnerColumnBg)

    val dinnerColor: Color
        @Composable get() = anim(if (isDark) DinnerColorDark else DinnerColor)

    val dinnerBorder: Color
        @Composable get() = anim(if (isDark) DinnerColorDark.copy(alpha = 0.3f) else DinnerColor.copy(alpha = 0.22f))

    val bedtimeBg: Color
        @Composable get() = anim(if (isDark) BedtimeBgDark else BedtimeBg)

    val bedtimeHeaderBg: Color
        @Composable get() = anim(if (isDark) BedtimeHeaderBgDark else BedtimeHeaderBg)

    val bedtimeColumnBg: Color
        @Composable get() = anim(if (isDark) BedtimeColumnBgDark else BedtimeColumnBg)

    val bedtimeColor: Color
        @Composable get() = anim(if (isDark) BedtimeColorDark else BedtimeColor)

    val bedtimeBorder: Color
        @Composable get() = anim(if (isDark) BedtimeColorDark.copy(alpha = 0.3f) else BedtimeColor.copy(alpha = 0.22f))


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
        @Composable get() = anim(if (isDark) Color(0xFF182335) else Color.White)

    val rowCardBorder: Color
        @Composable get() = anim(if (isDark) Color.Transparent else Color(0xFFE2E8F0))
}

