package com.example

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppThemeMode
import com.example.ui.InsulinTrackerScreen
import com.example.ui.InsulinTrackerViewModel
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

  // 记录从外部文件管理器或第三方应用传入的备份文件 URI
  private val incomingBackupUri = mutableStateOf<Uri?>(null)

  override fun attachBaseContext(newBase: Context) {
    val locale = Locale.SIMPLIFIED_CHINESE
    Locale.setDefault(locale)
    val config = Configuration(newBase.resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    val context = newBase.createConfigurationContext(config)
    super.attachBaseContext(context)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    val locale = Locale.SIMPLIFIED_CHINESE
    Locale.setDefault(locale)
    val config = resources.configuration
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    @Suppress("DEPRECATION")
    resources.updateConfiguration(config, resources.displayMetrics)

    super.onCreate(savedInstanceState)
    enableHighRefreshRate()
    enableEdgeToEdge()

    // 初始化内置离线语音识别模型
    com.example.data.VoiceRecognitionManager.init(applicationContext)

    // 检查冷启动时外部应用传入的 ZIP 备份文件
    incomingBackupUri.value = extractBackupUri(intent)

    setContent {
      val trackerViewModel: InsulinTrackerViewModel = viewModel()
      val themeMode by trackerViewModel.themeMode.collectAsStateWithLifecycle()
      val systemInDark = isSystemInDarkTheme()
      val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
      }

      // 监听到外部传入备份文件时，调起 ViewModel 解析并在界面弹出还原确认弹窗
      val pendingUri by incomingBackupUri
      LaunchedEffect(pendingUri) {
        pendingUri?.let { uri ->
          trackerViewModel.importBackupFromUri(this@MainActivity, uri)
          incomingBackupUri.value = null
        }
      }

      val currentConfig = LocalConfiguration.current
      val localizedConfig = remember(currentConfig) {
        Configuration(currentConfig).apply {
          setLocale(Locale.SIMPLIFIED_CHINESE)
          setLayoutDirection(Locale.SIMPLIFIED_CHINESE)
        }
      }

      CompositionLocalProvider(LocalConfiguration provides localizedConfig) {
        MyApplicationTheme(darkTheme = isDark) {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            InsulinTrackerScreen(viewModel = trackerViewModel)
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    // 当 App 已在后台运行时，用户从外部文件管理器点击“用其他应用打开”唤醒本界面
    incomingBackupUri.value = extractBackupUri(intent)
  }

  /**
   * 从外部传入的 Intent 中提取 ZIP 备份文件的 Uri
   * 支持通过 Intent.ACTION_VIEW (打开) 与 Intent.ACTION_SEND (分享) 传入的文件
   */
  private fun extractBackupUri(intent: Intent?): Uri? {
    if (intent == null) return null
    return when (intent.action) {
      Intent.ACTION_VIEW -> intent.data
      Intent.ACTION_SEND -> {
        val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
          @Suppress("DEPRECATION")
          intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        streamUri ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri ?: intent.data
      }
      else -> intent.data
    }
  }

  /**
   * 自动适配设备高刷新率（90Hz / 120Hz / 144Hz）
   * 优先选择当前屏幕支持的最高刷新率模式，使手势拖动与微动效达到丝滑体验
   */
  private fun enableHighRefreshRate() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val disp = display ?: (getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)?.getDisplay(Display.DEFAULT_DISPLAY)
        val highestMode = disp?.supportedModes?.maxByOrNull { it.refreshRate }
        if (highestMode != null && highestMode.refreshRate > 60f) {
          val layoutParams = window.attributes
          layoutParams.preferredDisplayModeId = highestMode.modeId
          window.attributes = layoutParams
        }
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        @Suppress("DEPRECATION")
        val defaultDisp = window.windowManager.defaultDisplay
        val highestMode = defaultDisp?.supportedModes?.maxByOrNull { it.refreshRate }
        if (highestMode != null && highestMode.refreshRate > 60f) {
          val layoutParams = window.attributes
          layoutParams.preferredDisplayModeId = highestMode.modeId
          window.attributes = layoutParams
        }
      }
    } catch (_: Exception) {
      // 容错降级
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}
