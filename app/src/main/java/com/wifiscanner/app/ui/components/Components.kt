package com.wifiscanner.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wifiscanner.app.domain.Finding
import com.wifiscanner.app.domain.FindingKind
import com.wifiscanner.app.domain.SignalQuality
import com.wifiscanner.app.domain.WifiMath
import com.wifiscanner.app.R
import com.wifiscanner.app.ui.theme.WsColors

/** Standard rounded card with a subtle outline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

/** Hero card with the brand gradient. Content should use white text. */
@Composable
fun GradientCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(WsColors.GradientStart, WsColors.GradientEnd)))
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        actions()
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = color.copy(alpha = 0.16f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.secondary) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
    }
}

/** Four-bar signal indicator colored by quality. */
@Composable
fun SignalBars(rssi: Int?, modifier: Modifier = Modifier.size(28.dp, 22.dp)) {
    val quality = rssi?.let { WifiMath.classify(it) }
    val lit = when (quality) {
        SignalQuality.EXCELLENT -> 4
        SignalQuality.GOOD -> 3
        SignalQuality.FAIR -> 2
        SignalQuality.WEAK, SignalQuality.POOR -> 1
        null -> 0
    }
    val on = quality?.color() ?: MaterialTheme.colorScheme.outline
    val off = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Canvas(modifier) {
        val gap = size.width / 14f
        val barW = (size.width - gap * 3) / 4f
        for (i in 0 until 4) {
            val h = size.height * (i + 1) / 4f
            drawRoundRect(
                color = if (i < lit) on else off,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 3f)
            )
        }
    }
}

/** Horizontal strength meter (0..100 %) with a gradient fill. */
@Composable
fun SignalMeter(rssi: Int, modifier: Modifier = Modifier) {
    val quality = WifiMath.classify(rssi)
    val fraction = WifiMath.percent(rssi) / 100f
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(track, cornerRadius = radius)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(quality.color().copy(alpha = 0.55f), quality.color())),
            size = Size(size.width * fraction.coerceAtLeast(0.03f), size.height),
            cornerRadius = radius
        )
    }
}

data class ChartSeries(val values: List<Float>, val color: Color)

/** Simple line chart drawn with Canvas. Y labels are shown at the top and bottom of the plot. */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    yMin: Float,
    yMax: Float,
    modifier: Modifier = Modifier,
    guides: List<Float> = emptyList(),
    labelFormat: (Float) -> String = { it.toInt().toString() }
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    Row(modifier.fillMaxWidth().height(160.dp)) {
        Column(Modifier.fillMaxSize().weight(0.14f), verticalArrangement = Arrangement.SpaceBetween) {
            Text(labelFormat(yMax), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(labelFormat(yMin), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(Modifier.fillMaxSize().weight(0.86f)) {
            val range = (yMax - yMin).coerceAtLeast(0.0001f)
            fun yOf(v: Float) = size.height - ((v.coerceIn(yMin, yMax) - yMin) / range) * size.height
            for (i in 0..3) {
                val y = size.height * i / 3f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            guides.forEach { g -> drawLine(gridColor, Offset(0f, yOf(g)), Offset(size.width, yOf(g)), strokeWidth = 2f) }
            series.forEachIndexed { index, s ->
                val n = s.values.size
                if (n == 0) return@forEachIndexed
                fun xOf(i: Int) = if (n == 1) size.width / 2f else size.width * i / (n - 1f)
                if (n == 1) {
                    drawCircle(s.color, radius = 5f, center = Offset(xOf(0), yOf(s.values[0])))
                } else {
                    val line = Path()
                    s.values.forEachIndexed { i, v ->
                        if (i == 0) line.moveTo(xOf(i), yOf(v)) else line.lineTo(xOf(i), yOf(v))
                    }
                    if (index == 0) {
                        val fill = Path().apply {
                            addPath(line)
                            lineTo(xOf(n - 1), size.height)
                            lineTo(xOf(0), size.height)
                            close()
                        }
                        drawPath(fill, Brush.verticalGradient(listOf(s.color.copy(alpha = 0.30f), Color.Transparent)))
                    }
                    drawPath(line, s.color, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun LoadingRow(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5f.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

enum class BannerKind { INFO, SUCCESS, WARNING, ERROR }

@Composable
fun Banner(kind: BannerKind, text: String, modifier: Modifier = Modifier) {
    val color = when (kind) {
        BannerKind.INFO -> MaterialTheme.colorScheme.secondary
        BannerKind.SUCCESS -> WsColors.Success
        BannerKind.WARNING -> WsColors.Warning
        BannerKind.ERROR -> WsColors.Danger
    }
    val icon = when (kind) {
        BannerKind.INFO -> Icons.Filled.Info
        BannerKind.SUCCESS -> Icons.Filled.CheckCircle
        BannerKind.WARNING -> Icons.Filled.Warning
        BannerKind.ERROR -> Icons.Filled.Error
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** A security finding with a label that separates verified facts from advice and missing data. */
@Composable
fun FindingCard(finding: Finding, modifier: Modifier = Modifier) {
    val (labelRes, color) = when (finding.kind) {
        FindingKind.VERIFIED -> R.string.kind_verified to WsColors.Success
        FindingKind.WARNING -> R.string.kind_warning to WsColors.Warning
        FindingKind.RECOMMENDATION -> R.string.kind_recommendation to MaterialTheme.colorScheme.primary
        FindingKind.UNAVAILABLE -> R.string.kind_unavailable to MaterialTheme.colorScheme.onSurfaceVariant
    }
    WsCard(modifier.fillMaxWidth()) {
        Pill(stringResource(labelRes), color)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(finding.titleRes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        finding.detailRes?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(it, *finding.args.toTypedArray()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
