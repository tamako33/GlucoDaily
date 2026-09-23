package com.example.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.MealPeriod
import com.example.data.ParsedVoiceRecord
import com.example.data.VoiceRecordParser
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealOnContainer
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealPrimaryDark
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceInputDialog(
    onDismiss: () -> Unit,
    onConfirmSave: (ParsedVoiceRecord) -> Unit,
    onFineTuneInEditDialog: (ParsedVoiceRecord) -> Unit
) {
    val context = LocalContext.current
    var spokenText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    // 智能解析结果
    val parsedRecord by remember(spokenText) {
        derivedStateOf {
            VoiceRecordParser.parse(spokenText)
        }
    }

    val recognizedItems = remember(parsedRecord) {
        parsedRecord.getRecognizedItems()
    }

    // 语音识别回调
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                spokenText = if (spokenText.isBlank()) text else "$spokenText，${text}"
            }
        }
    }

    // 录音权限申请
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechRecognition(context, speechLauncher) { isListening = it }
        } else {
            isListening = false
            Toast.makeText(context, "需要麦克风权限以进行语音输入", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestAndStartListening() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startSpeechRecognition(context, speechLauncher) { isListening = it }
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // 示例口述短语
    val examplePhrases = listOf(
        "今天早餐吃了包子鸡蛋，空腹血糖5.8，打了4单位门冬，餐后血糖7.2",
        "今天早餐前血糖5.8",
        "中午餐前血糖6.2，吃了一碗牛肉面，餐后6.8",
        "晚餐吃了青菜和鸡肉，打了5单位赖脯，餐后7.5",
        "睡前血糖6.5，打了10单位甘精胰岛素"
    )

    // 脉冲波纹动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = imeBottom > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .imePadding()
                .padding(bottom = if (isKeyboardOpen) 8.dp else 0.dp)
                .padding(horizontal = 16.dp, vertical = if (isKeyboardOpen) 8.dp else 24.dp)
                .fillMaxWidth()
                .testTag("voice_input_dialog")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                TealPrimary.copy(alpha = 0.16f),
                                TealContainer.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(x = 120f, y = 90f),
                            radius = 700f
                        )
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                TealPrimaryDark.copy(alpha = 0.09f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(x = 800f, y = 500f),
                            radius = 600f
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                TealPrimary.copy(alpha = 0.06f),
                                Color.Transparent,
                                TealPrimary.copy(alpha = 0.04f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                // 顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TealContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = TealOnContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "智能语音录入",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                )
                            )
                            Text(
                                text = "说出饮食、血糖或用药，智能提取并自动合并",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 大麦克风按钮区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .scale(if (isListening) pulseScale else 1.0f)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (isListening) listOf(Color(0xFFEF4444), Color(0xFFF87171))
                                    else listOf(TealPrimary, TealPrimaryDark)
                                )
                            )
                            .clickable {
                                requestAndStartListening()
                            }
                            .testTag("voice_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "点击说话",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isListening) "正在聆听中，请说话..." else "点击麦克风开始口述",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isListening) Color(0xFFEF4444) else TealPrimary
                        )
                    )
                    Text(
                        text = "支持只说一句话，如：'今天早餐前血糖5.8' 或完整餐食与用药",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 口述文本编辑框
                OutlinedTextField(
                    value = spokenText,
                    onValueChange = { spokenText = it },
                    label = { Text("识别或输入的文字") },
                    placeholder = { Text("例如：我今天早餐吃了包子鸡蛋，空腹血糖5.8，打了4单位门冬，餐后血糖7.2") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_input_textfield"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 4,
                    trailingIcon = {
                        if (spokenText.isNotBlank()) {
                            IconButton(onClick = { spokenText = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清空"
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 示例标签
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "点击快捷填入口述示例：",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        examplePhrases.forEach { phrase ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { spokenText = phrase }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = phrase,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 智能识别结果卡片
                AnimatedVisibility(
                    visible = recognizedItems.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = TealContainer.copy(alpha = 0.35f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            TealPrimary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = TealPrimaryDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "已提取到 ${recognizedItems.size} 项数据",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TealPrimaryDark
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TealPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "📅 ${parsedRecord.date}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TealPrimaryDark
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 识别出来的项目列表
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                recognizedItems.forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 2.dp,
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.5.dp,
                                            TealPrimary.copy(alpha = 0.35f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(text = item.icon, fontSize = 13.sp)
                                            Text(
                                                text = "${item.label}:",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            Text(
                                                text = item.value,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TealPrimaryDark,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(
                                color = TealPrimary.copy(alpha = 0.2f),
                                thickness = 0.8.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 智能合并规则：只更新上述提取的内容，该日期未提及的其它项目将完整保留不变。",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = TealOnContainer.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }

                if (spokenText.isNotBlank() && recognizedItems.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "未能从当前文字中提取出明确的血糖、用药或饮食信息。您可以尝试加入关键字，如：'空腹血糖5.8'、'吃了包子'、'打了4单位'等。",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 底部操作按钮
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (parsedRecord.hasAnyData()) {
                                onConfirmSave(parsedRecord)
                                onDismiss()
                            }
                        },
                        enabled = parsedRecord.hasAnyData(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_confirm_voice_save")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (parsedRecord.hasAnyData()) "确认智能录入 (${recognizedItems.size}项)" else "确认智能录入",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onFineTuneInEditDialog(parsedRecord)
                                onDismiss()
                            },
                            enabled = parsedRecord.hasAnyData(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_finetune_in_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "在表单中微调", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.7f)
                                .height(44.dp)
                        ) {
                            Text(text = "取消", fontSize = 13.sp)
                        }
                    }
                }
            }
            }
        }
    }
}

/**
 * 启动系统语音识别或给出指引
 */
private fun startSpeechRecognition(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onListeningChanged: (Boolean) -> Unit
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出饮食、血糖或用药情况...")
    }

    try {
        onListeningChanged(true)
        launcher.launch(intent)
    } catch (e: ActivityNotFoundException) {
        onListeningChanged(false)
        Toast.makeText(
            context,
            "当前系统未安装语音识别服务，您可直接在此输入文字或点击下方示例",
            Toast.LENGTH_LONG
        ).show()
    } catch (e: Exception) {
        onListeningChanged(false)
        Toast.makeText(context, "启动语音识别失败：${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
