package com.wifiscanner.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifiscanner.app.R
import com.wifiscanner.app.domain.SpeedPhase
import com.wifiscanner.app.network.SpeedErrorKind
import com.wifiscanner.app.ui.components.Banner
import com.wifiscanner.app.ui.components.BannerKind
import com.wifiscanner.app.ui.components.ChartSeries
import com.wifiscanner.app.ui.components.InfoRow
import com.wifiscanner.app.ui.components.LineChart
import com.wifiscanner.app.ui.components.ScreenHeader
import com.wifiscanner.app.ui.components.SectionTitle
import com.wifiscanner.app.ui.components.StatTile
import com.wifiscanner.app.ui.components.WsCard
import com.wifiscanner.app.ui.theme.WsColors
import com.wifiscanner.app.utils.formatDateTime
import com.wifiscanner.app.viewmodel.SpeedViewModel
import com.wifiscanner.app.viewmodel.appViewModelFactory
import kotlin.math.ln

@Composable
fun SpeedScreen(vm: SpeedViewModel = viewModel(factory = appViewModelFactory())) {
    val state by vm.state.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val configured by vm.configuredServer.collectAsStateWithLifecycle()

    val upd = state.update
    val last = history.firstOrNull()
    val dash = stringResource(R.string.dash)

    // While running or right after a run show live values; otherwise show the last stored result.
    val showLive = state.running || upd.phase == SpeedPhase.DONE
    val download = if (showLive) upd.downloadMbps ?: upd.liveMbps.takeIf { upd.phase == SpeedPhase.DOWNLOAD } else last?.downloadMbps
    val upload = if (showLive) upd.uploadMbps ?: upd.liveMbps.takeIf { upd.phase == SpeedPhase.UPLOAD } else last?.uploadMbps
    val latency = if (showLive) upd.latencyMs else last?.latencyMs
    val jitter = if (showLive) upd.jitterMs else last?.jitterMs
    val gaugeValue = when {
        state.running && upd.phase == SpeedPhase.LATENCY -> 0.0
        state.running -> upd.liveMbps ?: 0.0
        else -> download ?: 0.0
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader(stringResource(R.string.nav_speed), stringResource(R.string.speed_subtitle)) }

        item {
            WsCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    SpeedGauge(gaugeValue, stringResource(phaseLabel(state.running, upd.phase)))
                    Spacer(Modifier.height(8.dp))
                    if (state.running) {
                        LinearProgressIndicator(progress = { upd.progress }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { vm.cancel() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Text(stringResource(R.string.cancel), Modifier.padding(start = 6.dp))
                        }
                    } else {
                        Button(onClick = { vm.start() }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text(stringResource(R.string.speed_start), Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }

        state.error?.let { err ->
            item {
                val text = when (err.kind) {
                    SpeedErrorKind.INVALID_URL -> stringResource(R.string.speed_err_invalid_url)
                    SpeedErrorKind.UNREACHABLE -> stringResource(R.string.speed_err_unreachable, err.detail ?: "")
                    SpeedErrorKind.TIMEOUT -> stringResource(R.string.speed_err_timeout, err.detail ?: "")
                    SpeedErrorKind.HTTP -> stringResource(R.string.speed_err_http, err.detail ?: "")
                    SpeedErrorKind.NO_DATA -> stringResource(R.string.speed_err_no_data)
                    SpeedErrorKind.OTHER -> stringResource(R.string.speed_err_other, err.detail ?: "")
                }
                Banner(BannerKind.ERROR, text)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(stringResource(R.string.speed_download), download?.let { stringResource(R.string.fmt_mbps_1, it) } ?: dash, Modifier.weight(1f))
                    StatTile(stringResource(R.string.speed_upload), upload?.let { stringResource(R.string.fmt_mbps_1, it) } ?: dash, Modifier.weight(1f), WsColors.Blue)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(stringResource(R.string.speed_latency), latency?.let { stringResource(R.string.fmt_ms_0, it) } ?: dash, Modifier.weight(1f), WsColors.Success)
                    StatTile(stringResource(R.string.speed_jitter), jitter?.let { stringResource(R.string.fmt_ms_1, it) } ?: dash, Modifier.weight(1f), WsColors.Warning)
                }
            }
        }

        item {
            WsCard(Modifier.fillMaxWidth()) {
                InfoRow(stringResource(R.string.speed_server), state.server.ifBlank { configured }.removePrefix("https://"))
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.speed_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionTitle(stringResource(R.string.speed_history)) }
        item {
            WsCard(Modifier.fillMaxWidth()) {
                if (history.isEmpty()) {
                    Text(
                        stringResource(R.string.speed_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val ordered = history.reversed() // oldest first
                    val maxValue = ordered.maxOf { maxOf(it.downloadMbps, it.uploadMbps) }.toFloat().coerceAtLeast(1f)
                    LineChart(
                        series = listOf(
                            ChartSeries(ordered.map { it.downloadMbps.toFloat() }, MaterialTheme.colorScheme.secondary),
                            ChartSeries(ordered.map { it.uploadMbps.toFloat() }, WsColors.Blue)
                        ),
                        yMin = 0f,
                        yMax = maxValue * 1.15f
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.speed_chart_legend),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    history.take(5).forEach {
                        InfoRow(
                            formatDateTime(it.timestamp),
                            stringResource(R.string.speed_history_row, it.downloadMbps, it.uploadMbps, it.latencyMs)
                        )
                    }
                }
            }
        }
    }
}

private fun phaseLabel(running: Boolean, phase: SpeedPhase): Int = when {
    !running && phase == SpeedPhase.DONE -> R.string.speed_phase_done
    !running -> R.string.speed_phase_idle
    phase == SpeedPhase.LATENCY -> R.string.speed_phase_latency
    phase == SpeedPhase.DOWNLOAD -> R.string.speed_phase_download
    phase == SpeedPhase.UPLOAD -> R.string.speed_phase_upload
    else -> R.string.speed_phase_idle
}

/** Logarithmic gauge: 0..1000 Mbps maps onto a 270 degree arc. */
@Composable
private fun SpeedGauge(mbps: Double, label: String) {
    val fraction = (ln(1.0 + mbps) / ln(1001.0)).toFloat().coerceIn(0f, 1f)
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val color = MaterialTheme.colorScheme.secondary
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp)) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.fmt_number_1, mbps),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(stringResource(R.string.unit_mbps), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
        }
    }
}
