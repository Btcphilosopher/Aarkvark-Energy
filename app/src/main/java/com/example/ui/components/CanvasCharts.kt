package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricTeal

@Composable
fun MetricLineChart(
    data: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = ElectricTeal,
    gradientColors: List<Color> = listOf(ElectricTeal.copy(alpha = 0.35f), Color.Transparent)
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = DarkSurfaceVariant.copy(alpha = 0.5f)

    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No Chart Data Available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxVal = (data.maxOrNull() ?: 1.0).coerceAtLeast(1.0) * 1.15
    val minVal = 0.0

    Canvas(modifier = modifier.padding(16.dp)) {
        val width = size.width
        val height = size.height
        val paddingLeft = 60.dp.toPx()
        val paddingBottom = 40.dp.toPx()
        val paddingTop = 10.dp.toPx()
        val paddingRight = 10.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingBottom - paddingTop

        // Draw Y-Axis Grid Lines & Labels
        val gridLines = 4
        for (i in 0..gridLines) {
            val ratio = i.toFloat() / gridLines
            val y = height - paddingBottom - (ratio * chartHeight)
            val gridVal = minVal + ratio * (maxVal - minVal)

            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1.dp.toPx()
            )

            drawText(
                textMeasurer = textMeasurer,
                text = String.format("%.0f", gridVal),
                style = TextStyle(color = Color.Gray, fontSize = 10.sp),
                topLeft = Offset(10.dp.toPx(), y - 8.dp.toPx())
            )
        }

        // Calculate Data Points
        val points = mutableListOf<Offset>()
        val stepX = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

        for (i in data.indices) {
            val x = paddingLeft + i * stepX
            val ratioY = ((data[i] - minVal) / (maxVal - minVal)).toFloat()
            val y = height - paddingBottom - (ratioY * chartHeight)
            points.add(Offset(x, y))
        }

        // Draw Area Fill Gradient under Path
        if (points.isNotEmpty()) {
            val fillPath = Path().apply {
                moveTo(paddingLeft, height - paddingBottom)
                for (i in points.indices) {
                    lineTo(points[i].x, points[i].y)
                }
                lineTo(points.last().x, height - paddingBottom)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                    startY = paddingTop,
                    endY = height - paddingBottom
                )
            )
        }

        // Draw smooth Line Path
        if (points.isNotEmpty()) {
            val strokePath = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    // Smooth curve using cubic segments (Bezier)
                    val prev = points[i - 1]
                    val curr = points[i]
                    val controlX = (prev.x + curr.x) / 2
                    cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                }
            }
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw glowing circles at key data points
            for (offset in points) {
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = offset
                )
            }
        }

        // Draw X-Axis Labels
        for (i in labels.indices) {
            val labelX = paddingLeft + i * stepX
            drawText(
                textMeasurer = textMeasurer,
                text = labels[i],
                style = TextStyle(color = Color.Gray, fontSize = 10.sp),
                topLeft = Offset(labelX - 15.dp.toPx(), height - paddingBottom + 8.dp.toPx())
            )
        }
    }
}

@Composable
fun MetricBarChart(
    data: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = ElectricAmber
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = DarkSurfaceVariant.copy(alpha = 0.5f)

    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No Chart Data Available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxVal = (data.maxOrNull() ?: 1.0).coerceAtLeast(1.0) * 1.15
    val minVal = 0.0

    Canvas(modifier = modifier.padding(16.dp)) {
        val width = size.width
        val height = size.height
        val paddingLeft = 60.dp.toPx()
        val paddingBottom = 40.dp.toPx()
        val paddingTop = 10.dp.toPx()
        val paddingRight = 10.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingBottom - paddingTop

        // Draw Y-Axis Grid Lines & Labels
        val gridLines = 4
        for (i in 0..gridLines) {
            val ratio = i.toFloat() / gridLines
            val y = height - paddingBottom - (ratio * chartHeight)
            val gridVal = minVal + ratio * (maxVal - minVal)

            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1.dp.toPx()
            )

            drawText(
                textMeasurer = textMeasurer,
                text = String.format("%.0f", gridVal),
                style = TextStyle(color = Color.Gray, fontSize = 10.sp),
                topLeft = Offset(10.dp.toPx(), y - 8.dp.toPx())
            )
        }

        // Draw Bars
        val barCount = data.size
        val totalSpacing = chartWidth * 0.3f
        val remainingWidth = chartWidth - totalSpacing
        val barWidth = remainingWidth / barCount
        val barSpacing = totalSpacing / (barCount + 1)

        for (i in data.indices) {
            val barX = paddingLeft + barSpacing + i * (barWidth + barSpacing)
            val ratioY = ((data[i] - minVal) / (maxVal - minVal)).toFloat()
            val barHeight = ratioY * chartHeight
            val barY = height - paddingBottom - barHeight

            drawRect(
                color = barColor,
                topLeft = Offset(barX, barY),
                size = Size(barWidth, barHeight)
            )

            // Draw value on top of bar
            val valueStr = String.format("%.0f", data[i])
            drawText(
                textMeasurer = textMeasurer,
                text = valueStr,
                style = TextStyle(color = Color.White, fontSize = 9.sp),
                topLeft = Offset(barX + (barWidth / 2) - 10.dp.toPx(), barY - 14.dp.toPx())
            )

            // Draw X label
            if (i < labels.size) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = labels[i],
                    style = TextStyle(color = Color.Gray, fontSize = 10.sp),
                    topLeft = Offset(barX + (barWidth / 2) - 15.dp.toPx(), height - paddingBottom + 8.dp.toPx())
                )
            }
        }
    }
}
