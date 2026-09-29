package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.TealPrimary

/**
 * 单元格数据长按特大号卡片放大弹窗
 */
data class CellZoomDetail(
    val title: String,        // 标题，如 "🌅 早餐 · 用药"、"☀️ 午餐 · 餐后2h"、"🛌 睡前 · 加餐"
    val date: String,         // 日期，如 "2026-09-18"
    val mainText: String,     // 主要放大数值或文本，如 "12U"、"6.8"、"全麦面包1片"
    val subText: String = "", // 附加说明，如 "门冬胰岛素 · 餐前"、"正常范围"、"偏高 (空腹参考: 3.9~6.1)"
    val highlightColor: Color? = null // 主题高亮色
)

@Composable
fun CellZoomDialog(
    detail: CellZoomDetail,
    onDismiss: () -> Unit
) {
    val themeColor = detail.highlightColor ?: TealPrimary
    val isDark = com.example.ui.theme.AppThemeColors.isDark

    FrostedGlassDialogOverlay(
        onDismissRequest = onDismiss
    ) { dismissWithAnimation ->
        Surface(
            shape = AppleCardShape,
            color = if (isDark) Color(0xFF1C1C1E) else Color.White,
            border = appleCardBorder(),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp)
                .testTag("cell_zoom_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部：分类与关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = ApplePillShape,
                        color = themeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = detail.title,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp,
                            color = themeColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(32.dp)
                            .applePressEffect(0.92f)
                    ) {
                        IconButton(
                            onClick = { dismissWithAnimation() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 日期标识
                Text(
                    text = "记录日期：${detail.date}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.15).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 核心放大展示区域 (大字号、醒目背景卡片)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppleMdShape)
                        .background(themeColor.copy(alpha = 0.08f))
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = detail.mainText,
                        fontSize = if (detail.mainText.length > 8) 26.sp else 38.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-0.38).sp,
                        color = themeColor,
                        textAlign = TextAlign.Center,
                        lineHeight = if (detail.mainText.length > 8) 32.sp else 44.sp
                    )
                }

                if (detail.subText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = ApplePillShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = detail.subText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = (-0.15).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 确定按钮 (Apple Pill Button + Press Effect)
                androidx.compose.material3.Button(
                    onClick = { dismissWithAnimation() },
                    shape = ApplePillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColor,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .applePressEffect(0.95f)
                ) {
                    Text(
                        text = "完成",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.23).sp
                    )
                }
            }
        }
    }
}
