package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InsulinRecord
import com.example.ui.theme.AppThemeColors

@Composable
fun DeleteConfirmDialog(
    record: InsulinRecord,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isDark = AppThemeColors.isDark

    FrostedGlassDialogOverlay(
        onDismissRequest = onDismiss
    ) { dismissWithAnimation ->
        Card(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 380.dp)
                .testTag("delete_confirm_dialog"),
            shape = AppleCardShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White
            ),
            border = appleCardBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            val isDark = isSystemInDarkTheme()
            val destructiveRed = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(destructiveRed.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = destructiveRed,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "确认删除",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.38).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "确定要删除 ${record.date} 的记录吗？此操作无法撤销。",
                    fontSize = 13.5.sp,
                    letterSpacing = (-0.12).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { dismissWithAnimation() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .applePressEffect(0.95f),
                        shape = ApplePillShape
                    ) {
                        Text(
                            "取消",
                            fontSize = 15.sp,
                            letterSpacing = (-0.2).sp
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = destructiveRed,
                            contentColor = Color.White
                        ),
                        shape = ApplePillShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .applePressEffect(0.95f)
                            .testTag("confirm_delete_button")
                    ) {
                        Text(
                            "确认删除",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }
            }
        }
    }
}
