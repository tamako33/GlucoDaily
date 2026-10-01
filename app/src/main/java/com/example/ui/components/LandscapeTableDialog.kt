package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DoctorReportShareHelper
import com.example.data.InsulinRecord
import com.example.ui.TableDateRange
import com.example.ui.theme.TealPrimary
import kotlin.math.hypot

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * 横屏放大纯表格视图：
 * 1. 强制横屏并在退出时稳定恢复竖屏，防止自动转回。
 * 2. 仅展示放大表格与日期范围筛选，不包含上方折线图。
 * 3. 支持“全折”功能：折叠后顶部菜单完全隐藏，表格直接铺满 100% 整个屏幕，由右上角半透明悬浮岛提供返回与展开。
 * 4. 支持双指捏合 / 远离（Pinch-to-zoom）平滑缩放表格，默认 1.25x 舒适大字模式，支持 0.75x ~ 2.5x 自由伸缩。
 */
@Composable
fun LandscapeTableView(
    records: List<InsulinRecord>,
    allRecords: List<InsulinRecord>,
    todayStr: String,
    selectedRange: TableDateRange,
    onRangeSelect: (TableDateRange) -> Unit,
    onEdit: (InsulinRecord) -> Unit,
    onDelete: (InsulinRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    // 物理返回键处理：先切回竖屏，再关闭全屏视图
    BackHandler {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDismiss()
    }

    // 锁定横屏模式并自动隐藏状态栏（类似游戏全屏沉浸模式），退出时保证恢复竖屏与显示状态栏
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        // 类似游戏的全屏沉浸逻辑：隐藏顶部状态栏；用户从顶部边缘下滑可临时呼出半透明状态栏，不破坏布局，随后自动淡出隐藏
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.statusBars())

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            insetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    // 控制顶部菜单栏是否完全折叠，全折后表格占据 100% 全屏高度，不浪费一丝一毫屏幕空间
    var isHeaderCollapsed by remember { mutableStateOf(false) }

    // 表格双指捏合缩放比例：范围 0.75f ~ 2.5f，默认预设为舒适大字模式 1.25f，解决小屏字体偏小问题
    var zoomScale by remember { mutableFloatStateOf(1.25f) }

    // 双指捏合手势监听（在 Initial pass 拦截两指及以上手势，绝不影响单指常规的横向与纵向滚动）
    val pinchZoomModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    val p0 = pressed[0].position
                    val p1 = pressed[1].position
                    val prev0 = pressed[0].previousPosition
                    val prev1 = pressed[1].previousPosition
                    val currentDist = hypot(p0.x - p1.x, p0.y - p1.y)
                    val prevDist = hypot(prev0.x - prev1.x, prev0.y - prev1.y)
                    if (prevDist > 5f && currentDist > 5f) {
                        val factor = currentDist / prevDist
                        if (factor in 0.5f..2.0f) {
                            zoomScale = (zoomScale * factor).coerceIn(0.75f, 2.5f)
                            pressed.forEach { it.consume() }
                        }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("landscape_table_view"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(pinchZoomModifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!isHeaderCollapsed) {
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(top = 6.dp, bottom = 4.dp)
                        } else {
                            Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        }
                    )
            ) {
                // 顶部菜单栏：展开状态展示完整控制区；点击“全折”后彻底折叠，零高度占用
                AnimatedVisibility(
                    visible = !isHeaderCollapsed,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧：返回竖屏按钮
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clickable {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        onDismiss()
                                    }
                                    .testTag("btn_exit_landscape")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回竖屏",
                                        tint = TealPrimary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = "返回竖屏",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                }
                            }

                            // 中间：标题、全折全屏按钮与缩放微调
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "表格明细",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // 全折（全屏看表）按钮
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TealPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .clickable { isHeaderCollapsed = true }
                                        .testTag("btn_collapse_landscape_header")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "全折菜单栏",
                                            tint = TealPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "全折 (全屏看表)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TealPrimary
                                        )
                                    }
                                }

                                // 缩放控制微调胶囊
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "缩小表格",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { zoomScale = (zoomScale - 0.15f).coerceIn(0.75f, 2.5f) }
                                                .padding(2.dp)
                                        )
                                        Text(
                                            text = "${(zoomScale * 100).toInt()}%",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .clickable { zoomScale = 1.25f }
                                                .padding(horizontal = 4.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "放大表格",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { zoomScale = (zoomScale + 0.15f).coerceIn(0.75f, 2.5f) }
                                                .padding(2.dp)
                                        )
                                    }
                                }
                            }

                            // 右侧：日期范围快速切换胶囊（近7天、近14天、近30天、全部）与条数
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TableDateRange.entries.forEach { range ->
                                    val isSelected = range == selectedRange
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .clickable { onRangeSelect(range) }
                                            .testTag("landscape_range_${range.name}")
                                    ) {
                                        Text(
                                            text = range.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                // 计数标签
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        text = "${records.size}条",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }

                                // 发给医生分享按钮
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TealPrimary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, TealPrimary),
                                    modifier = Modifier
                                        .clickable {
                                            DoctorReportShareHelper.shareDoctorReport(
                                                context = context,
                                                records = records,
                                                rangeDescription = selectedRange.label
                                            )
                                        }
                                        .testTag("btn_landscape_share_doctor")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "导出表格",
                                            tint = TealPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "导出表格",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TealPrimary
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // 纯表格展示区域（全折时直接铺满 100% 全屏区域）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (records.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "该时间段暂无记录",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        RecordTable(
                            records = records,
                            allRecords = allRecords,
                            todayStr = todayStr,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            isEnlarged = true,
                            zoomScale = zoomScale
                        )
                    }
                }
            }

            // 全折模式下的悬浮浮岛控件（绝不挤压表格任何高度，在右上角优雅微光悬浮）
            if (isHeaderCollapsed) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 缩放比例与点击恢复 125%
                        Text(
                            text = "${(zoomScale * 100).toInt()}%",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            modifier = Modifier
                                .clickable { zoomScale = 1.25f }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        // 展开菜单栏按钮
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TealPrimary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .clickable { isHeaderCollapsed = false }
                                .testTag("btn_expand_landscape_header")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "展开菜单",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "展开菜单",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )
                            }
                        }

                        // 返回竖屏按钮
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clickable {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    onDismiss()
                                }
                                .testTag("btn_exit_landscape_collapsed")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回竖屏",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "返回",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
