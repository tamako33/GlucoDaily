package com.example.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * 原生离线语音合成管理器 (SystemTtsManager)：
 *
 * 架构设计与长辈适老化考量：
 * 1. 零体积占用 (0KB 增量)：直接调用 Android 操作系统底层的 TextToSpeech 服务，不引入任何第三方模型权重文件；
 * 2. 纯离线可用：依赖国内各品牌手机系统自带的离线中文语音合成引擎（如小米小爱、OPPO小布、华为、vivo 等离线包）；
 * 3. 适老化语速调优：将默认发音语速设为 0.85x，音调温和适中，便于长辈清晰辨识播报内容；
 * 4. 状态响应式广播：通过 [isSpeaking] 向上暴露当前播报状态，用于长辈界面大喇叭按钮的声波动画律动。
 */
object SystemTtsManager {
    private const val TAG = "SystemTtsManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                var result = tts?.setLanguage(Locale.CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                }
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = tts?.setLanguage(Locale.CHINA)
                }
                isInitialized = true
                // 慢速 0.85x，专为老年人优化，发音清晰沉稳
                tts?.setSpeechRate(0.85f)
                tts?.setPitch(1.0f)
                Log.i(TAG, "系统原生离线 TTS 引擎初始化成功，语言设置结果: $result，语速已调优至 0.85x")
            } else {
                Log.e(TAG, "系统原生 TTS 初始化失败，状态码: $status")
            }
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    Log.w(TAG, "TTS 播报出错: $utteranceId")
                }
            })
        }
    }

    /**
     * 语音朗读文本
     * @param text 需朗读的自然语言文本
     * @param flush 是否打断前序尚未播报完毕的声音
     * @return 是否成功加入播放队列
     */
    fun speak(text: String, flush: Boolean = true): Boolean {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS 引擎尚未准备就绪，跳过播报: $text")
            return false
        }
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val result = tts?.speak(text, queueMode, null, "care_tts_${System.currentTimeMillis()}")
        return result == TextToSpeech.SUCCESS
    }

    /**
     * 停止当前语音播报
     */
    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    /**
     * 释放 TTS 资源
     */
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _isSpeaking.value = false
    }
}
