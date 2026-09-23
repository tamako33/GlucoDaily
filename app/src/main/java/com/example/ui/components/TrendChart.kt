package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InsulinRecord
import com.example.ui.theme.AppThemeColors
import java.util.Locale
import kotlin.math.max

@Composable
fun TrendChart(
    records: List<InsulinRecord>,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trend_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            // Header with legend and collapse toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onToggleExpanded != null) { onToggleExpanded?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(AppThemeColors.breakfastColor, CircleShape)
                    )
                    Text(
                        text = "近期走势",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (onToggleExpanded != null) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "折叠走势图" else "展开走势图",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isExpanded) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = AppThemeColors.breakfastColor, label = "空腹", isLine = true)
                        LegendItem(color = AppThemeColors.lunchColor, label = "餐后", isLine = true, isDashed = true)
                        LegendItem(
                            color = if (AppThemeColors.isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            label = "胰岛素",
                            isBar = true
                        )
                    }
                } else {
                    Text(
                        text = "已折叠 · 点击展开",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))

                    if (records.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无近期走势数据",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        CanvasChart(records = records)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isLine: Boolean = false,
    isDashed: Boolean = false,
    isBar: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (isBar) {
            Box(
                modifier = Modifier
                    .size(width = 7.dp, height = 7.dp)
                    .background(color.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
            )
        } else if (isLine) {
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 2.5.dp)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
        Text(
            text = label,
            fontSize = 10.5.sp,
            maxLines = 1,
            softWrap = false,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CanvasChart(records: List<InsulinRecord>) {
    val fastingColor = AppThemeColors.breakfastColor
    val postMealColor = AppThemeColors.lunchColor
    val isDark = AppThemeColors.isDark
    val insulinBarColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
    val gridLineColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textPaintColor = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.GRAY
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .testTag("trend_canvas")
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 26.dp.toPx()
        val paddingRight = 10.dp.toPx()
        val paddingTop = 8.dp.toPx()
        val paddingBottom = 20.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val minGlucose = 3.0f
        // Dynamically calculate the maximum observed glucose across all records
        val maxObservedGlucose = records.flatMap {
            listOfNotNull(it.fastingBG, it.postMealAverageBG, it.postBfBG, it.postLunchBG, it.postDinnerBG)
        }.maxOrNull() ?: 10f

        // Intelligent step calculation: default step is 3 (max 12), automatically scaling up for 15, 20, 25+
        val stepSize = kotlin.math.ceil((maxObservedGlucose + 1.5f - minGlucose) / 3f).coerceAtLeast(3f)
        val maxGlucose = minGlucose + stepSize * 3f

        val maxObservedInsulin = records.map { it.totalInsulin }.maxOrNull() ?: 0f
        val maxInsulin = max(40.0f, kotlin.math.ceil((maxObservedInsulin + 5f) / 10f) * 10f)

        // Draw horizontal grid lines for glucose (3, 6, 9, 12 mmol/L)
        val gridSteps = 3
        for (i in 0..gridSteps) {
            val bgValue = minGlucose + (maxGlucose - minGlucose) * (i / gridSteps.toFloat())
            val y = paddingTop + chartHeight * (1f - (bgValue - minGlucose) / (maxGlucose - minGlucose))
            drawLine(
                color = gridLineColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 0.8.dp.toPx()
            )

            // Left axis labels (mmol/L)
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.US, "%.0f", bgValue),
                paddingLeft - 6.dp.toPx(),
                y + 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textPaintColor
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }

        val n = records.size
        if (n == 0) return@Canvas

        // Fixed 7-slot coordinate system so bars never overlap Y axis and have fixed positions
        val totalSlots = 7
        val slotWidth = chartWidth / totalSlots.toFloat()
        fun getSlotCenterX(index: Int): Float = paddingLeft + (index + 0.5f) * slotWidth

        // 1. Draw Insulin Bars (Fixed bar width & fixed slot positions)
        val barWidth = 10.dp.toPx()
        for (i in records.indices) {
            val r = records[i]
            val cx = getSlotCenterX(i)
            val totalInsulin = r.totalInsulin
            if (totalInsulin > 0) {
                val barFraction = (totalInsulin / maxInsulin).coerceIn(0f, 1f)
                val barH = chartHeight * barFraction
                drawRoundRect(
                    color = insulinBarColor.copy(alpha = 0.7f),
                    topLeft = Offset(cx - barWidth / 2f, height - paddingBottom - barH),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
            }

            // Draw date labels on X axis
            val dateLabel = if (r.date.length >= 10) r.date.substring(5) else r.date
            drawContext.canvas.nativeCanvas.drawText(
                dateLabel,
                cx,
                height - 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textPaintColor
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }

        // 2. Draw Fasting Glucose Line (Solid)
        val fastingPoints = mutableListOf<Offset>()
        for (i in records.indices) {
            val bg = records[i].fastingBG ?: records[i].preBfBG
            if (bg != null) {
                val cx = getSlotCenterX(i)
                val fraction = ((bg - minGlucose) / (maxGlucose - minGlucose)).coerceIn(0f, 1f)
                val cy = paddingTop + chartHeight * (1f - fraction)
                fastingPoints.add(Offset(cx, cy))
            }
        }

        if (fastingPoints.size > 1) {
            val fastingPath = Path().apply {
                moveTo(fastingPoints.first().x, fastingPoints.first().y)
            for (p in fastingPoints.drop(1)) {
                lineTo(p.x, p.y)
            }
        }
        drawPath(
            path = fastingPath,
            color = fastingColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
    for (p in fastingPoints) {
        drawCircle(color = surfaceColor, radius = 3.dp.toPx(), center = p)
        drawCircle(color = fastingColor, radius = 1.8.dp.toPx(), center = p)
    }

    // 3. Draw Post-Meal Average Glucose Line (Dashed)
    val postMealPoints = mutableListOf<Offset>()
    for (i in records.indices) {
        val avg = records[i].postMealAverageBG
        if (avg != null) {
            val cx = getSlotCenterX(i)
            val fraction = ((avg - minGlucose) / (maxGlucose - minGlucose)).coerceIn(0f, 1f)
            val cy = paddingTop + chartHeight * (1f - fraction)
            postMealPoints.add(Offset(cx, cy))
        }
    }

    if (postMealPoints.size > 1) {
        val postMealPath = Path().apply {
            moveTo(postMealPoints.first().x, postMealPoints.first().y)
            for (p in postMealPoints.drop(1)) {
                lineTo(p.x, p.y)
            }
        }
        drawPath(
            path = postMealPath,
            color = postMealColor,
            style = Stroke(
                width = 1.8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )
    }
    for (p in postMealPoints) {
        drawCircle(color = surfaceColor, radius = 2.8.dp.toPx(), center = p)
        drawCircle(color = postMealColor, radius = 1.6.dp.toPx(), center = p)
    }
}
}
