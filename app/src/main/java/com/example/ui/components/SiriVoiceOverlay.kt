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
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
 * 竖向对齐的Siri点击启停语音悬浮按钮（按下开始识别，再按停止识别）
 */
@Composable
fun SiriVoiceFabButton(
    isListening: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHolding: Boolean = false,
    onPressStart: (() -> Unit)? = null,
    onPressEnd: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val isActive = isListening || isHolding

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
        targetValue = if (isActive) 1.15f else 1.0f,
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
            .clip(CircleShape)
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
            .testTag("fab_siri_voice")
    ) {
        // 呼吸光晕外环（识别中放大扩散）
        if (isActive) {
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
            color = if (isActive) {
                if (isDark) Color(0xFF0F172A) else Color(0xFF0D9488)
            } else {
                TealContainer
            },
            shadowElevation = if (isActive) 8.dp else 3.dp,
            modifier = Modifier
                .size(56.dp)
                .border(
                    width = if (isActive) 2.dp else 1.dp,
                    brush = if (isActive) {
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
                    imageVector = if (isActive) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = if (isActive) "停止语音录入" else "开启语音录入",
                    tint = if (isActive) {
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
    onSaveRecord: (ParsedVoiceRecord) -> Unit,
    onDismiss: () -> Unit,
    isHolding: Boolean = false,
    isCareMode: Boolean = false
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

    var permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>? = null
    var stopListeningJob by remember { mutableStateOf<Job?>(null) }

    fun safeStartListening() {
        stopListeningJob?.cancel()
        stopListeningJob = null
        voiceState = SiriVoiceState.LISTENING
        if (hasAudioPermission) {
            VoiceRecognitionManager.startListening(context)
        } else {
            permissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun safeStopListeningWithDelay() {
        stopListeningJob?.cancel()
        stopListeningJob = coroutineScope.launch {
            voiceState = SiriVoiceState.PROCESSING
            delay(300)
            VoiceRecognitionManager.stopListening()
        }
    }

    val actualLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted && isVisible) {
            safeStartListening()
        }
    }
    permissionLauncher = actualLauncher

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

    // 浮层显示时检查权限并立即开启识别（按下启动、再按停止）
    LaunchedEffect(isVisible) {
        if (isVisible) {
            if (!hasAudioPermission) {
                permissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
            } else if (!isListening) {
                safeStartListening()
            }
        }
    }

    // 兼容外部传入的按住状态
    LaunchedEffect(isHolding) {
        if (isVisible) {
            if (isHolding) {
                safeStartListening()
            } else if (isListening && !isLocalHolding) {
                safeStopListeningWithDelay()
            }
        }
    }

    // 顶部全屏半透明毛玻璃蒙层遮罩：加强雾面漫反射质感
    val scrimColor = if (isDark) Color(0xFF040711).copy(alpha = 0.80f) else Color(0xFFF1F5F9).copy(alpha = 0.78f)

    // 底部弥散浮层背景：从底部向上融合App主题色（TealPrimary翡翠蓝绿）的过渡渐变毛玻璃
    val sheetGradient = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x000B131F),
                    Color(0xC00F172A),
                    Color(0xEB0D1F25),
                    Color(0xF50A2E33),
                    Color(0xFF05383C)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x00FFFFFF),
                    Color(0xC8FFFFFF),
                    Color(0xEFF4FAF9),
                    Color(0xF5DBF1ED),
                    Color(0xFCCCEDE7)
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
            initialOffsetY = { (it * 0.45f).toInt() },
            animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f)
        ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(170))
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

            // 弥散风格底板容器（Siri式无底框浮层设计，关怀模式适度增高浮层占位，使提示框居上、文字居中偏下）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isCareMode) Modifier.fillMaxHeight(0.72f) else Modifier)
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
                        .padding(start = 20.dp, end = 20.dp, top = if (isCareMode) 16.dp else 14.dp, bottom = if (isKeyboardOpen) 10.dp else 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isCareMode) {
                        // 关怀模式顶部大标题与显眼关闭按键
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🎙️", fontSize = 24.sp)
                                Text(
                                    text = "语音记一笔",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryTextColor
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { onDismiss() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "关闭",
                                        tint = primaryTextColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // 关怀模式通俗易懂的语音例句提示（置顶靠上，提示框向上移）
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.75f) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.2.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("💡", fontSize = 18.sp)
                                Text(
                                    text = "口述示例：“早饭前6.2，打8个门冬，吃了包子，散步20分钟”",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = secondaryTextColor,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    } else {
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
                    }

                    // 状态栏提示（简洁优雅居中）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (voiceState == SiriVoiceState.PROCESSING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(if (isCareMode) 16.dp else 12.dp),
                                strokeWidth = 2.dp,
                                color = accentCyan
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(if (isCareMode) 10.dp else 8.dp)
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

                        Spacer(modifier = Modifier.width(if (isCareMode) 8.dp else 6.dp))

                        val currentHint = if (isListening && recordingSeconds > 0) {
                            if (isCareMode) "正在倾听您说话 (${String.format(Locale.getDefault(), "%02d:%02d", recordingSeconds / 60, recordingSeconds % 60)}) · 说完点下方按钮"
                            else "$statusHint (${String.format(Locale.getDefault(), "%02d:%02d", recordingSeconds / 60, recordingSeconds % 60)})"
                        } else {
                            if (isCareMode && !isListening) "请点击下方大按钮开始说话"
                            else statusHint
                        }

                        Text(
                            text = currentHint,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = primaryTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isCareMode) 16.sp else 13.sp
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isCareMode) 22.dp else 14.dp))

                    val subtitleScrollState = rememberScrollState()
                    LaunchedEffect(spokenText) {
                        if (spokenText.isNotBlank()) {
                            subtitleScrollState.animateScrollTo(
                                subtitleScrollState.maxValue,
                                animationSpec = spring(stiffness = 380f, dampingRatio = 0.85f)
                            )
                        }
                    }

                    // ==========================================
                    // 🌟 Siri 浮动式文字展示区（向上自然滑动隐退，最新文字居于视野）
                    // ==========================================
                    val subtitleViewportHeight = if (isCareMode) 120.dp else 74.dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(subtitleViewportHeight)
                            .padding(horizontal = 8.dp),
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
                                        fontSize = if (isCareMode) 22.sp else 16.sp,
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
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(if (isCareMode) 40.dp else 32.dp)
                                ) {
                                    Text("完成", fontSize = if (isCareMode) 15.sp else 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        } else if (spokenText.isNotBlank()) {
                            // 🌟 社交媒体式实时滚动字幕流：长段文字向上自然滑动并羽化渐隐，最新内容始终停驻在视野中央
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        // 顶部平滑羽化渐隐：之前说过的文字向上滑出时自然隐褪，绝不向下无限堆叠撑爆屏幕
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Black,
                                                0.25f to Color.Transparent,
                                                startY = 0f,
                                                endY = size.height * 0.32f
                                            ),
                                            blendMode = BlendMode.DstOut
                                        )
                                    }
                                    .verticalScroll(subtitleScrollState),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    text = "“ $spokenText ”",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = primaryTextColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isCareMode) 26.sp else 18.sp,
                                        lineHeight = if (isCareMode) 36.sp else 24.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 36.dp, vertical = 4.dp)
                                        .testTag("siri_recognized_text")
                                        .clickable { isManualEditing = true }
                                )
                            }

                            // 独立悬浮于右上角的操作按键（编辑与清空），不随文本向上滑动而位移
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 2.dp, end = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { isManualEditing = true },
                                    modifier = Modifier.size(if (isCareMode) 32.dp else 24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "修改口述内容",
                                        tint = accentCyan,
                                        modifier = Modifier.size(if (isCareMode) 18.dp else 14.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { VoiceRecognitionManager.setCustomText("") },
                                    modifier = Modifier.size(if (isCareMode) 32.dp else 24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "清空文本",
                                        tint = secondaryTextColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(if (isCareMode) 18.dp else 14.dp)
                                    )
                                }
                            }
                        } else {
                            // 未说话时的浮动占位引导文案（无框、轻透、居中偏下）
                            Text(
                                text = if (isCareMode) {
                                    if (isListening) "🎙️ 正在倾听您说话，口述将大字展示在此…"
                                    else "👉 点击下方大按钮开始说话，支持一句话记全"
                                } else {
                                    if (isListening) "请说话，正在实时离线识别中..."
                                    else "点击下方按钮开始说话，再次点击停止识别并提取"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isListening) accentCyan else secondaryTextColor.copy(alpha = 0.85f),
                                    fontSize = if (isCareMode) 18.sp else 14.sp,
                                    lineHeight = if (isCareMode) 26.sp else 20.sp,
                                    fontWeight = if (isCareMode) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        tint = accentCyan,
                                        modifier = Modifier.size(if (isCareMode) 20.dp else 15.dp)
                                    )
                                    Text(
                                        text = if (isCareMode) "✅ 识别提取出 ${recognizedItems.size} 项内容，请核对：" else "识别提取出 ${recognizedItems.size} 项内容，供核对填入：",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = accentCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (isCareMode) 16.5.sp else 12.sp
                                        )
                                    )
                                }

                                Text(
                                    text = "📅 ${parsedRecord.date}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = secondaryTextColor,
                                        fontSize = if (isCareMode) 14.sp else 11.sp
                                    )
                                )
                            }

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recognizedItems.forEach { item ->
                                    val badgeBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF0FDF4)
                                    val badgeBorder = if (isDark) accentCyan.copy(alpha = 0.35f) else Color(0xFF86EFAC)
                                    Surface(
                                        shape = RoundedCornerShape(if (isCareMode) 12.dp else 8.dp),
                                        color = badgeBg,
                                        border = BorderStroke(if (isCareMode) 1.2.dp else 0.8.dp, badgeBorder),
                                        shadowElevation = if (isDark) 0.dp else 1.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = if (isCareMode) 12.dp else 9.dp,
                                                vertical = if (isCareMode) 8.dp else 5.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = item.icon, fontSize = if (isCareMode) 18.sp else 13.sp)
                                            Text(
                                                text = "${item.label}:",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isDark) secondaryTextColor else Color(0xFF166534),
                                                    fontSize = if (isCareMode) 15.sp else 11.sp
                                                )
                                            )
                                            Text(
                                                text = item.value,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isDark) accentCyan else Color(0xFF15803D),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = if (isCareMode) 17.sp else 12.sp
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
                                shape = RoundedCornerShape(if (isCareMode) 16.dp else 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isCareMode) 56.dp else 44.dp)
                                    .padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(if (isCareMode) 22.dp else 16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCareMode) "✓ 核对无误，保存记录 (${recognizedItems.size}项)" else "核对无误，填入记录 (${recognizedItems.size}项数值)",
                                    fontSize = if (isCareMode) 18.sp else 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 🌟 核心操作区：按下开始识别，再按下停止识别（告别长按模式）
                    // ==========================================
                    Spacer(modifier = Modifier.height(18.dp))

                    val holdScale by animateFloatAsState(
                        targetValue = if (isListening) 1.04f else 1.0f,
                        animationSpec = spring(stiffness = 500f, dampingRatio = 0.75f),
                        label = "holdScale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val careBtnColor = if (isListening) {
                            if (isDark) Color(0xFFDC2626) else Color(0xFFEF4444)
                        } else {
                            TealPrimary
                        }
                        Surface(
                            shape = RoundedCornerShape(if (isCareMode) 20.dp else 28.dp),
                            color = if (isCareMode) careBtnColor else {
                                if (isListening) {
                                    if (isDark) Color(0xFF0F766E) else Color(0xFF0D9488)
                                } else {
                                    if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                }
                            },
                            border = BorderStroke(
                                width = if (isListening) 2.dp else 1.dp,
                                brush = if (isCareMode) {
                                    SolidColor(Color.Transparent)
                                } else if (isListening) {
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
                            shadowElevation = if (isListening || isCareMode) 8.dp else 2.dp,
                            modifier = Modifier
                                .fillMaxWidth(if (isCareMode) 0.95f else 0.92f)
                                .height(if (isCareMode) 64.dp else 56.dp)
                                .graphicsLayer {
                                    scaleX = holdScale
                                    scaleY = holdScale
                                }
                                .clip(RoundedCornerShape(if (isCareMode) 20.dp else 28.dp))
                                .clickable {
                                    if (!hasAudioPermission) {
                                        permissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
                                        return@clickable
                                    }
                                    if (isListening) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        safeStopListeningWithDelay()
                                    } else {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        safeStartListening()
                                    }
                                }
                                .testTag("toggle_voice_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isCareMode || isListening) {
                                        Color.White
                                    } else {
                                        if (isDark) Color(0xFF2DD4BF) else TealPrimary
                                    },
                                    modifier = Modifier.size(if (isCareMode) 30.dp else 24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isCareMode) {
                                        if (isListening) "说完了，点击停止" else "点击开始说话"
                                    } else {
                                        if (isListening) "点击 停止识别" else "点击 开始说话"
                                    },
                                    fontSize = if (isCareMode) 20.sp else 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCareMode || isListening) {
                                        Color.White
                                    } else {
                                        primaryTextColor
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isCareMode) {
                                if (isListening) "🔴 录音中 · 说完轻触上方红色按钮即可停止并提取"
                                else "💡 单次轻触即可说话 · 再次轻触即可停止"
                            } else {
                                if (isListening) "正在录音倾听中 · 再次点击停止识别并提取"
                                else "点击开启语音识别 · 支持血糖、饮食、用药与运动"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (isCareMode) 14.5.sp else 12.sp,
                                color = if (isListening) accentCyan else secondaryTextColor
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
