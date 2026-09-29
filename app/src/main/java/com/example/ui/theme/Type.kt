package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// =========================================================================
// Apple SF Pro Typography System (per VoltAgent/awesome-design-md: apple/DESIGN.md)
//
// Principles:
// 1. Negative letter-spacing at display sizes: gives the signature "Apple tight" cadence.
// 2. Body copy strictly at 17sp (not 16sp): line-height 1.47 gives "reading, not scanning" pace.
// 3. Weight ladder: 300 / 400 / 600 / 700. Weight 500 is deliberately absent!
// 4. includeFontPadding = false to maintain pure geometric baseline alignment.
// =========================================================================

val AppleFontFamily = FontFamily.Default

val AppleTextStyleBase = TextStyle(
    fontFamily = AppleFontFamily,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

val Typography = Typography(
    // Apple Display Headlines (SF Pro Display 600 with negative tracking)
    headlineLarge = AppleTextStyleBase.copy(
        fontSize = 34.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 41.sp,
        letterSpacing = (-0.374).sp
    ),
    headlineMedium = AppleTextStyleBase.copy(
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 34.sp,
        letterSpacing = (-0.28).sp
    ),
    headlineSmall = AppleTextStyleBase.copy(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
        letterSpacing = (-0.22).sp
    ),

    // Apple Section Titles & Sub-nav
    titleLarge = AppleTextStyleBase.copy(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = AppleTextStyleBase.copy(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
        letterSpacing = (-0.374).sp
    ),
    titleSmall = AppleTextStyleBase.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
        letterSpacing = (-0.15).sp
    ),

    // Apple Body Copy (17sp / 400 / 1.47 line-height / -0.374 tracking)
    bodyLarge = AppleTextStyleBase.copy(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
        letterSpacing = (-0.374).sp
    ),
    bodyMedium = AppleTextStyleBase.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp,
        letterSpacing = (-0.2).sp
    ),
    bodySmall = AppleTextStyleBase.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp
    ),

    // Apple Buttons & Captions (14sp and 12sp)
    labelLarge = AppleTextStyleBase.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp,
        letterSpacing = (-0.224).sp
    ),
    labelMedium = AppleTextStyleBase.copy(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = (-0.12).sp
    ),
    labelSmall = AppleTextStyleBase.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 14.sp,
        letterSpacing = (-0.08).sp
    )
)
