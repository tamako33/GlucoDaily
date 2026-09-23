package com.example.ui.components

import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ParsedVoiceRecord
import com.example.data.VoiceRecognitionManager
import com.example.data.VoiceRecognitionService
import androidx.compose.runtime.collectAsState
import com.example.data.VoiceRecordParser
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealOnContainer
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 状态机：语音录入与智能识别状态
 */
enum class SiriVoiceState {
    LISTENING,   // 正在录音聆听中
    PROCESSING,  // 正在语音识别与提取中
    AUTO_SAVING, // 正在自动填入保存
    COMPLETED,   // 成功保存
    ERROR        // 识别提示
}

/**
 * 竖向对齐的Siri长按/点击语音悬浮按钮
 */
@Composable
fun SiriVoiceFabButton(
    isHolding: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isHolding) 1.18f else 1.0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.75f),
        label = "buttonScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressStart()
                        tryAwaitRelease()
                        onPressEnd()
                    },
                    onTap = {
                        if (onClick != null) {
                            onClick()
                        } else {
                            onPressStart()
                        }
                    }
                )
            }
            .testTag("fab_siri_voice")
    ) {
        // 呼吸光晕外环（按住时放大扩散）
        if (isHolding) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                (if (isDark) Color(0xFF00F2FE) else Color(0xFF0284C7)).copy(alpha = if (isDark) 0.45f else 0.35f),
                                (if (isDark) Color(0xFF8B5CF6) else Color(0xFF38BDF8)).copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // 主按钮圆盘
        Surface(
            shape = CircleShape,
            color = if (isHolding) {
                if (isDark) Color(0xFF0F172A) else Color(0xFF0D9488)
            } else {
                TealContainer
            },
            shadowElevation = if (isHolding) 8.dp else 3.dp,
            modifier = Modifier
                .size(56.dp)
                .border(
                    width = if (isHolding) 2.dp else 1.dp,
                    brush = if (isHolding) {
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFF00F2FE),
                                Color(0xFF8B5CF6),
                                Color(0xFFEC4899),
                                Color(0xFF00F2FE)
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(TealPrimary.copy(alpha = 0.35f), TealPrimary))
                    },
                    shape = CircleShape
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = if (isHolding) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = "语音智能录入",
                    tint = if (isHolding) {
                        if (isDark) Color(0xFF00F2FE) else Color.White
                    } else {
                        TealOnContainer
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * 屏幕下方仿Siri智能语音识别与录入浮层：
 * 1. 离线优先架构：支持无网络/离线语音识别与100%本地规则/数值提取，断网下依然流畅使用；
 * 2. Siri浮动式文字设计：口述内容直接浮动在炫彩声波动效上方，去除传统底框卡片；
 * 3. 自动高亮提取数值徽章并支持一键自动填入保存；
 * 4. 完美适配白天与黑夜模式；
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SiriVoiceBottomOverlay(
    isVisible: Boolean,
    isHolding: Boolean,
    onSaveRecord: (ParsedVoiceRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val isListening by VoiceRecognitionManager.isListening.collectAsState()
    val audioLevel by VoiceRecognitionManager.audioLevel.collectAsState()
    val spokenText by VoiceRecognitionManager.spokenText.collectAsState()
    val statusHint by VoiceRecognitionManager.statusText.collectAsState()
    val parsedRecord by VoiceRecognitionManager.parsedRecord.collectAsState()

    val haptic = LocalHapticFeedback.current
    var isLocalHolding by remember { mutableStateOf(false) }
    val isCurrentlyHolding = isHolding || isLocalHolding
    var voiceState by remember { mutableStateOf(SiriVoiceState.LISTENING) }
    var isManualEditing by remember { mutableStateOf(false) }
    var manualEditText by remember { mutableStateOf("") }
    var restartTrigger by remember { mutableLongStateOf(0L) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    val recognizedItems by remember(parsedRecord) {
        derivedStateOf { parsedRecord.getRecognizedItems() }
    }

    // 麦克风录音权限检测
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    // 录音计时器
    LaunchedEffect(isListening, isVisible) {
        if (isVisible && isListening) {
            recordingSeconds = 0
            while (isActive && isListening) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    var stopListeningJob by remember { mutableStateOf<Job?>(null) }

    fun safeStartListening() {
        stopListeningJob?.cancel()
        stopListeningJob = null
        voiceState = SiriVoiceState.LISTENING
        if (hasAudioPermission) {
            VoiceRecognitionManager.startListening(context)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun safeStopListeningWithDelay() {
        stopListeningJob?.cancel()
        stopListeningJob = coroutineScope.launch {
            voiceState = SiriVoiceState.PROCESSING
            delay(500)
            VoiceRecognitionManager.stopListening()
        }
    }

    // 弹窗关闭时停止录音并重置状态
    DisposableEffect(isVisible) {
        isManualEditing = false
        recordingSeconds = 0
        if (!isVisible) {
            stopListeningJob?.cancel()
            stopListeningJob = null
            VoiceRecognitionManager.stopListening()
            isLocalHolding = false
        }
        onDispose {
            stopListeningJob?.cancel()
            stopListeningJob = null
            VoiceRecognitionManager.stopListening()
            isLocalHolding = false
        }
    }

    // 浮层显示时检查权限（若无权限则请求，但不自动开启录音，严格遵循按住说话）
    LaunchedEffect(isVisible) {
        if (isVisible && !hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // 监听主界面悬浮 FAB 长按说话松开事件：按住启动实时离线识别，松开后延迟停止识别并分析提取
    LaunchedEffect(isHolding) {
        if (isVisible) {
            if (isHolding) {
                safeStartListening()
            } else if (isListening && !isLocalHolding) {
                safeStopListeningWithDelay()
            }
        }
    }

    // 顶部全屏半透明蒙层遮罩
    val scrimColor = if (isDark) Color(0xB8000000) else Color(0x330F172A)

    val sheetGradient = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x000F172A),
                    Color(0xD90F172A),
                    Color(0xF50F172A),
                    Color(0xFF0B1120)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x00FFFFFF),
                    Color(0xD9FFFFFF),
                    Color(0xF8FFFFFF),
                    Color(0xFFFFFFFF)
                )
            )
        }
    }

    val primaryTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentCyan = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = 450f, dampingRatio = 0.8f)
        ) + fadeIn(animationSpec = tween(180)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(220, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            val density = LocalDensity.current
            val imeBottom = WindowInsets.ime.getBottom(density)
            val isKeyboardOpen = imeBottom > 0

            // 弥散风格底板容器（Siri式无底框浮层设计，输入法弹出时避让半个字距8dp）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(sheetGradient)
                    .imePadding()
                    .padding(bottom = if (isKeyboardOpen) 8.dp else 0.dp)
                    .navigationBarsPadding()
                    .graphicsLayer { clip = false }
                    .clickable(enabled = false) {}
                    .testTag("siri_voice_overlay")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = if (isKeyboardOpen) 10.dp else 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 极简半透明拖拽指示微条
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.22f)
                                else Color(0xFFCBD5E1)
                            )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 状态栏提示（简洁优雅居中）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (voiceState == SiriVoiceState.PROCESSING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = accentCyan
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (voiceState) {
                                            SiriVoiceState.COMPLETED -> Color(0xFF10B981)
                                            SiriVoiceState.AUTO_SAVING -> accentCyan
                                            SiriVoiceState.PROCESSING -> Color(0xFF38BDF8)
                                            else -> if (isListening) accentCyan else secondaryTextColor
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        val currentHint = if (isListening && recordingSeconds > 0) {
                            "$statusHint (${String.format(Locale.getDefault(), "%02d:%02d", recordingSeconds / 60, recordingSeconds % 60)})"
                        } else {
                            statusHint
                        }

                        Text(
                            text = currentHint,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = primaryTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ==========================================
                    // 🌟 Siri 浮动式文字展示区（位于动效上方，去底框）
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .heightIn(min = 44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isManualEditing) {
                            // 手动轻触编辑模式（高出输入法半个字距，带有完成确认）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f)
                                        else Color(0xFFF1F5F9)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = spokenText,
                                    onValueChange = {
                                        VoiceRecognitionManager.setCustomText(it)
                                    },
                                    textStyle = TextStyle(
                                        color = primaryTextColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Start
                                    ),
                                    cursorBrush = SolidColor(accentCyan),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { isManualEditing = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("完成", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        } else if (spokenText.isNotBlank()) {
                            // Siri浮动式口述文字（大字号、无边框、无底框卡片、优雅居中浮动）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "“ $spokenText ”",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = primaryTextColor,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp,
                                        lineHeight = 24.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .testTag("siri_recognized_text")
                                        .clickable { isManualEditing = true }
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { isManualEditing = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "修改口述内容",
                                            tint = accentCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { VoiceRecognitionManager.setCustomText("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "清空文本",
                                            tint = secondaryTextColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // 未说话时的浮动占位引导文案（无框、轻透）
                            Text(
                                text = if (isListening) "请说话，正在实时离线识别中..." else "按住下方按钮说话，松开后自动提取数值填入",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isListening) accentCyan else secondaryTextColor.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ==========================================
                    // 🌟 炫彩声波动效（位于浮动文字正下方）
                    // ==========================================
                    SiriWaveform(
                        audioLevel = if (isListening) audioLevel else 0.15f,
                        isDark = isDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )

                    // 提取到的数值徽章列表（用户结束说话、进入分析或完成状态后才显示供核对，说话中不抢先提取）
                    AnimatedVisibility(
                        visible = recognizedItems.isNotEmpty() && !isListening && !isCurrentlyHolding,
                        enter = fadeIn(tween(150)) + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut(tween(150))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = accentCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "识别提取出 ${recognizedItems.size} 项内容，供核对填入：",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = accentCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Text(
                                    text = "📅 ${parsedRecord.date}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = secondaryTextColor,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                recognizedItems.forEach { item ->
                                    val badgeBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF0FDF4)
                                    val badgeBorder = if (isDark) accentCyan.copy(alpha = 0.35f) else Color(0xFF86EFAC)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeBg,
                                        border = BorderStroke(0.8.dp, badgeBorder),
                                        shadowElevation = if (isDark) 0.dp else 1.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(text = item.icon, fontSize = 13.sp)
                                            Text(
                                                text = "${item.label}:",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isDark) secondaryTextColor else Color(0xFF166534),
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Text(
                                                text = item.value,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isDark) accentCyan else Color(0xFF15803D),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // 确认自动填入按钮
                            Button(
                                onClick = {
                                    if (parsedRecord.hasAnyData()) {
                                        onSaveRecord(parsedRecord)
                                        voiceState = SiriVoiceState.COMPLETED
                                        coroutineScope.launch {
                                            delay(500)
                                            onDismiss()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TealPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "核对无误，填入记录 (${recognizedItems.size}项数值)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 🌟 核心操作区：按住说话，松开自动识别并提取
                    // ==========================================
                    Spacer(modifier = Modifier.height(18.dp))

                    val isActionActive = isListening || isCurrentlyHolding

                    val holdScale by animateFloatAsState(
                        targetValue = if (isActionActive) 1.05f else 1.0f,
                        animationSpec = spring(stiffness = 500f, dampingRatio = 0.75f),
                        label = "holdScale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = if (isActionActive) {
                                if (isDark) Color(0xFF0F766E) else Color(0xFF0D9488)
                            } else {
                                if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            },
                            border = BorderStroke(
                                width = if (isActionActive) 2.dp else 1.dp,
                                brush = if (isActionActive) {
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF00F2FE),
                                            Color(0xFF8B5CF6),
                                            Color(0xFFEC4899),
                                            Color(0xFF00F2FE)
                                        )
                                    )
                                } else {
                                    SolidColor(if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                                }
                            ),
                            shadowElevation = if (isActionActive) 10.dp else 2.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .height(56.dp)
                                .graphicsLayer {
                                    scaleX = holdScale
                                    scaleY = holdScale
                                }
                                .pointerInput(hasAudioPermission) {
                                    detectTapGestures(
                                        onTap = {
                                            if (!hasAudioPermission) {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        onPress = {
                                            if (!hasAudioPermission) {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                return@detectTapGestures
                                            }
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isLocalHolding = true
                                            safeStartListening()
                                            val released = tryAwaitRelease()
                                            isLocalHolding = false
                                            if (released) {
                                                safeStopListeningWithDelay()
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            } else {
                                                stopListeningJob?.cancel()
                                                stopListeningJob = null
                                                VoiceRecognitionManager.cancel()
                                            }
                                        }
                                    )
                                }
                                .testTag("hold_to_speak_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isActionActive) {
                                        Color.White
                                    } else {
                                        if (isDark) Color(0xFF2DD4BF) else TealPrimary
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isActionActive) "松开 完成识别" else "按住 说话",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActionActive) {
                                        Color.White
                                    } else {
                                        primaryTextColor
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isActionActive) "阿里流式识别中 (松开后1s停止)..." else "阿里离线流式识别 · 松开后分析填入",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                color = if (isActionActive) accentCyan else secondaryTextColor
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 仿Siri炫彩多层正弦波流动绘制画布
 */
@Composable
fun SiriWaveform(
    audioLevel: Float,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "siriWavePhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // 音量平滑弹簧动画，快速响应声音振幅
    val animatedVolume by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0.12f, 1.0f),
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.7f),
        label = "audioSpring"
    )

    val path1 = remember { Path() }
    val path2 = remember { Path() }
    val path3 = remember { Path() }
    val corePath = remember { Path() }

    val waveColors1 = remember(isDark) {
        if (isDark) listOf(Color(0xFF00F2FE), Color(0xFF38BDF8)) else listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
    }
    val waveColors2 = remember(isDark) {
        if (isDark) listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)) else listOf(Color(0xFF7C3AED), Color(0xFFA855F7))
    }
    val waveColors3 = remember(isDark) {
        if (isDark) listOf(Color(0xFF10B981), Color(0xFF06B6D4)) else listOf(Color(0xFF059669), Color(0xFF10B981))
    }

    Canvas(
        modifier = modifier.graphicsLayer {}
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        if (width <= 0 || height <= 0) return@Canvas

        // 中心柔光背景
        val glowColor1 = if (isDark) Color(0xFF00F2FE).copy(alpha = 0.18f * animatedVolume) else Color(0xFF38BDF8).copy(alpha = 0.10f * animatedVolume)
        val glowColor2 = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.12f * animatedVolume) else Color(0xFFA855F7).copy(alpha = 0.06f * animatedVolume)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor1, glowColor2, Color.Transparent),
                center = Offset(width / 2f, midY),
                radius = width / 2.5f
            )
        )

        val steps = 36
        val dx = width / steps

        // 绘制波浪 1（青蓝波）
        path1.reset()
        val amp1 = (height * 0.38f) * animatedVolume
        for (i in 0..steps) {
            val x = i * dx
            val normX = x / width
            val envelope = sin(PI * normX).toFloat()
            val y = midY + sin(normX * 2 * PI * 1.5f + phase).toFloat() * amp1 * envelope
            if (i == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
        }
        drawPath(
            path = path1,
            brush = Brush.horizontalGradient(waveColors1),
            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // 绘制波浪 2（紫粉波）
        path2.reset()
        val amp2 = (height * 0.32f) * animatedVolume
        val phase2 = phase + (PI / 2).toFloat()
        for (i in 0..steps) {
            val x = i * dx
            val normX = x / width
            val envelope = sin(PI * normX).toFloat()
            val y = midY + sin(normX * 2 * PI * 1.8f + phase2).toFloat() * amp2 * envelope
            if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
        }
        drawPath(
            path = path2,
            brush = Brush.horizontalGradient(waveColors2),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )

        // 绘制波浪 3（翠绿波）
        path3.reset()
        val amp3 = (height * 0.26f) * animatedVolume
        val phase3 = phase + PI.toFloat()
        for (i in 0..steps) {
            val x = i * dx
            val normX = x / width
            val envelope = sin(PI * normX).toFloat()
            val y = midY + sin(normX * 2 * PI * 1.3f + phase3).toFloat() * amp3 * envelope
            if (i == 0) path3.moveTo(x, y) else path3.lineTo(x, y)
        }
        drawPath(
            path = path3,
            brush = Brush.horizontalGradient(waveColors3),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // 中心高亮细波
        corePath.reset()
        val coreAmp = (height * 0.18f) * animatedVolume
        for (i in 0..steps) {
            val x = i * dx
            val normX = x / width
            val envelope = sin(PI * normX).toFloat()
            val y = midY + sin(normX * 2 * PI * 2f + phase).toFloat() * coreAmp * envelope
            if (i == 0) corePath.moveTo(x, y) else corePath.lineTo(x, y)
        }
        val coreColor = if (isDark) Color.White.copy(alpha = 0.8f * animatedVolume) else Color(0xFF0369A1).copy(alpha = 0.6f * animatedVolume)
        drawPath(
            path = corePath,
            color = coreColor,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
